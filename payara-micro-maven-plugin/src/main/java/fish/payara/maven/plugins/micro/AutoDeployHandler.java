/*
 *
 * Copyright (c) 2023-24 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.maven.plugins.micro;

import static fish.payara.maven.plugins.micro.Configuration.INOTIFY_USER_LIMIT_REACHED_MESSAGE;
import static fish.payara.maven.plugins.micro.Configuration.WATCH_SERVICE_ERROR_MESSAGE;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.openqa.selenium.JavascriptExecutor;

/**
 *
 * @author Gaurav Gupta
 */
public class AutoDeployHandler implements Runnable {

    private final StartMojo start;
    private final MavenProject project;
    private final File webappDirectory;
    private final Log log;
    private final ExecutorService executorService;
    private WatchService watchService;
    private Future<?> buildReloadTask;
    private final AtomicBoolean cleanPending = new AtomicBoolean(false);
    private final List<Source> sourceUpdatedPending = new CopyOnWriteArrayList<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final Path buildPath;

    public AutoDeployHandler(StartMojo start, File webappDirectory) {
        this.start = start;
        this.project = start.getEnvironment().getMavenProject();
        this.webappDirectory = webappDirectory;
        this.log = start.getLog();
        this.executorService = Executors.newSingleThreadExecutor();
        this.buildPath = project.getBasedir().toPath().resolve("target");
    }

    public void stop() {
        stopRequested.set(true);
    }

    public boolean isAlive() {
        return !stopRequested.get();
    }

    @Override
    public void run() {
        try {
            Path rootPath = project.getBasedir().toPath();
            this.watchService = FileSystems.getDefault().newWatchService();
            rootPath.register(watchService,
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY);

            registerAllDirectories(rootPath);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (buildReloadTask != null && !buildReloadTask.isDone()) {
                        buildReloadTask.cancel(true);
                    }
                    executorService.shutdown();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }));

            while (isAlive()) {
                WatchKey key = watchService.poll(60, TimeUnit.SECONDS);
                if (key != null) {
                    List<WatchEvent<?>> filteredEvents = new ArrayList<>();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        Path fullPath = ((Path) key.watchable()).resolve(changed);
                        if (fullPath.startsWith(buildPath)) {
                            continue;
                        }
                        filteredEvents.add(event);
                    }
                    if (!filteredEvents.isEmpty()) {
                        if (buildReloadTask != null && !buildReloadTask.isDone()) {
                            buildReloadTask.cancel(true);
                        }
                        boolean fileDeletedOrRenamed = false;
                        boolean resourceModified = false;
                        boolean testClassesModified = false;
                        boolean rebootRequired = false;
                        for (WatchEvent<?> event : filteredEvents) {
                            WatchEvent.Kind<?> kind = event.kind();
                            Path changed = (Path) event.context();
                            Path fullPath = ((Path) key.watchable()).resolve(changed);
                            log.debug("Source modified: " + changed + " - " + kind);

                            Path projectRoot = Paths.get(project.getBasedir().toURI());
                            Path sourceRoot = projectRoot.resolve("src");
                            Path mainDirectory = sourceRoot.resolve("main");
                            Path javaDirectory = mainDirectory.resolve("java");
                            Path resourcesDirectory = mainDirectory.resolve("resources");
                            Path testDirectory = sourceRoot.resolve("test");

                            sourceUpdatedPending.add(new Source(fullPath, kind, fullPath.startsWith(javaDirectory)));
                            if (fullPath.startsWith(resourcesDirectory)) {
                                resourceModified = true;
                            }
                            if (fullPath.startsWith(testDirectory)) {
                                testClassesModified = true;
                            }
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                if (Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS)) {
                                    register(fullPath); // register watch service for newly created dir
                                }
                            }
                            if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                fileDeletedOrRenamed = true;
                                cleanPending.set(true);
                            }
                            if (start.getRebootOnChange().contains(changed.toString())) {
                                rebootRequired = true;
                                fileDeletedOrRenamed = true;
                                cleanPending.set(true);
                                break;
                            }
                        }

                        log.debug("sourceUpdatedPending: " + sourceUpdatedPending + " " + log);
                        if (!sourceUpdatedPending.isEmpty()) {
                            updateTitle("Building");
                            log.info("Auto-build started for " + project.getName());
                            List<String> goalsList = updateGoalsList(fileDeletedOrRenamed, resourceModified, testClassesModified);
                            log.debug("goalsList: " + goalsList);
                            executeBuildReloadTask(goalsList, rebootRequired);
                        }
                    }
                    key.reset();
                }
            }
        } catch (Exception ex) {
            log.error(ex);
            if (hasInotifyLimitReachedException(ex)) {
                log.error(WATCH_SERVICE_ERROR_MESSAGE);
            }
        }
    }

    private boolean hasInotifyLimitReachedException(Throwable ex) {
        while (ex != null) {
            if (ex instanceof IOException && ex.getMessage().contains(INOTIFY_USER_LIMIT_REACHED_MESSAGE)) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    private void registerAllDirectories(Path path) throws IOException {
        Files.walk(path)
                .filter(Files::isDirectory)
                .forEach(this::register);
    }

    private void register(Path path) {
        boolean isChild = path.startsWith(buildPath) && (path.equals(buildPath) || !buildPath.relativize(path).equals(path));
        try {
            if (!isChild) {
                log.debug("register watch service for " + path);
                path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            }
        } catch (IOException ex) {
            log.error("Error registering directories", ex);
        }
    }

    private List<String> updateGoalsList(boolean fileDeletedOrRenamed, boolean resourceModified,
            boolean testClassesModified) {
        boolean onlyJavaFilesUpdated = sourceUpdatedPending.stream().allMatch(k -> k.path.toString().endsWith(".java") && k.kind == ENTRY_MODIFY && k.javaClass);
        List<String> goalsList = new ArrayList<>();
        if (fileDeletedOrRenamed || cleanPending.get() || sourceUpdatedPending.size() > 1) {
            goalsList.add(0, "clean");
            goalsList.add("resources:resources");
            resourceModified = true;
        } else if (resourceModified) {
            goalsList.add("resources:resources");
        }
        goalsList.add("org.apache.maven.plugins:maven-compiler-plugin:3.12.1:compile"); //v3.12.1 is required as is includes fix https://github.com/apache/maven-compiler-plugin/pull/213
        if (onlyJavaFilesUpdated) {
            goalsList.add("-Dmaven.compiler.useIncrementalCompilation=false");
        }
        if (resourceModified || !onlyJavaFilesUpdated) {
            goalsList.add("war:exploded");
        } else {
            Path outputDirectory = Paths.get(webappDirectory.toPath().toString(), "WEB-INF", "classes");
            goalsList.add("-Dmaven.compiler.outputDirectory=\"" + outputDirectory.toString() + "\"");
        }
        if (!testClassesModified) {
            goalsList.add("-Dmaven.test.skip=true");
        } else {
            goalsList.add("-DskipTests");
        }
        return goalsList;
    }

    private void executeBuildReloadTask(List<String> goalsList, boolean rebootRequired) {
        buildReloadTask = executorService.submit(() -> {
            if (goalsList.get(0).equals("clean")) {
                deleteBuildDir(project.getBuild().getDirectory());
                goalsList.remove(0);
            }
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(project.getBasedir(), "pom.xml"));
            request.setGoals(goalsList);
            log.debug("Maven goals: " + goalsList);
            System.setProperty("maven.multiModuleProjectDirectory", project.getBasedir().toString());

            Invoker invoker = new DefaultInvoker();
            invoker.setLogger(new InvokerLoggerImpl(log));
            invoker.setInputStream(InputStream.nullInputStream());
            try {
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    log.debug("Auto-build failed with exit code: " + result.getExitCode());
                    updateTitle("Build failed");
                } else {
                    log.info("Auto-build successful for " + project.getName());
                    cleanPending.set(false);
                    sourceUpdatedPending.clear();
                    
                    if (rebootRequired) {
                        if (start.getMicroProcess().isAlive()) {
                            updateTitle("Restarting");
                            start.getMicroProcess().destroy();
                        }
                    } else {
                        updateTitle("Reloading");
                        ReloadMojo reloadMojo = new ReloadMojo(project, log);
                        reloadMojo.setKeepState(start.keepState);
                        if (start.hotDeploy) {
                            Path rootPath = project.getBasedir().toPath();
                            List<String> sourcesChanged = new ArrayList<>();
                            reloadMojo.setHotDeploy(start.hotDeploy);
                            for (Source source : sourceUpdatedPending) {
                                String extension = source.path.toString().substring(source.path.toString().lastIndexOf('.') + 1);
                                if (extension.equals("xml") || extension.equals("properties")) {
                                    reloadMojo.setMetadataChanged(true);
                                }
                                Path relativePath = rootPath.relativize(source.path);
                                sourcesChanged.add(relativePath.toString().replace(File.separator, "/"));
                            }
                            log.debug("SourcesChanged: " + sourcesChanged);
                            reloadMojo.setSourcesChanged(String.join(", ", sourcesChanged));
                        }
                        try {
                            reloadMojo.execute();
                        } catch (MojoExecutionException ex) {
                            log.error("Error invoking Reload", ex);
                        }
                    }

                    cleanPending.set(false);
                    sourceUpdatedPending.clear();
                }
            } catch (MavenInvocationException ex) {
                log.error("Error invoking Maven", ex);
            }
        });
    }

    public void deleteBuildDir(String filePath) {
        try {
            Path fileToDelete = Paths.get(filePath);
            Files.walkFileTree(fileToDelete, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new DeleteFileVisitor());
        } catch (IOException e) {
            log.error("Error occurred while deleting the file: ", e);
        }
    }
    
    private void updateTitle(String state) {
        if (start.getDriver() != null) {
            ((JavascriptExecutor) start.getDriver()).executeScript(String.format("document.title = '%s %s';", state, project.getName()));
        }
    }

    class DeleteFileVisitor extends SimpleFileVisitor<Path> {

        private boolean hasJarExtension(Path file) {
            return file.getFileName().toString().toLowerCase().endsWith(".jar");
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

            if (Files.isRegularFile(path)) {
                if (hasJarExtension(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore locked jar
                    }
                } else {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Error occurred while deleting the file: ", e);
                    }
                }
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            try {
                Files.delete(file);
            } catch (NoSuchFileException e) {
                log.debug("Error occurred while deleting the file: ", e);
            } catch (IOException e) {
                log.error("Error occurred while deleting the file: ", e);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            try {
                Files.delete(dir);
            } catch (java.nio.file.DirectoryNotEmptyException e) {
                // Ignore
            } catch (IOException e) {
                log.error("Error occurred while deleting the directory: ", e);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
