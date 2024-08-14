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
package fish.payara.maven.plugins;

import static fish.payara.maven.plugins.Configuration.CLASSES_DIRECTORY;
import static fish.payara.maven.plugins.Configuration.GOAL_CLEAN;
import static fish.payara.maven.plugins.Configuration.GOAL_COMPILE;
import static fish.payara.maven.plugins.Configuration.GOAL_PROCESS_RESOURCES;
import static fish.payara.maven.plugins.Configuration.GOAL_WAR;
import static fish.payara.maven.plugins.Configuration.GOAL_WAR_EXPLODED;
import static fish.payara.maven.plugins.Configuration.INOTIFY_USER_LIMIT_REACHED_MESSAGE;
import static fish.payara.maven.plugins.Configuration.JAVA_DIR;
import static fish.payara.maven.plugins.Configuration.JAVA_FILE_EXTENSION;
import static fish.payara.maven.plugins.Configuration.MAIN_DIR;
import static fish.payara.maven.plugins.Configuration.MAVEN_MULTI_MODULE_PROJECT_DIRECTORY;
import static fish.payara.maven.plugins.Configuration.OPTION_DISABLE_INCREMENTAL_COMPILATION;
import static fish.payara.maven.plugins.Configuration.OPTION_OUTPUT_DIRECTORY;
import static fish.payara.maven.plugins.Configuration.POM;
import static fish.payara.maven.plugins.Configuration.POM_XML;
import static fish.payara.maven.plugins.Configuration.RESOURCES_DIR;
import static fish.payara.maven.plugins.Configuration.SKIP_TESTS_FLAG;
import static fish.payara.maven.plugins.Configuration.SKIP_TESTS_OPTION;
import static fish.payara.maven.plugins.Configuration.SRC_DIR;
import static fish.payara.maven.plugins.Configuration.TEST_DIR;
import static fish.payara.maven.plugins.Configuration.WATCH_SERVICE_ERROR_MESSAGE;
import static fish.payara.maven.plugins.Configuration.WEB_INF_DIRECTORY;
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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class AutoDeployHandler implements Runnable {

    private final StartTask start;
    protected final MavenProject project;
    private final File webappDirectory;
    protected final Log log;
    private final ExecutorService executorService;
    private WatchService watchService;
    private Future<?> buildReloadTask;
    private long buildReloadTaskStartTime;
    private final AtomicBoolean cleanPending = new AtomicBoolean(false);
    protected final ConcurrentSkipListSet<Source> sourceUpdatedPending = new ConcurrentSkipListSet<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final Path buildPath, ideaPath,
            eclipsePath, eclipseClasspathPath, eclipseProjectPath,
            vscodePath, nbPath;
    protected final static String RELOADING = "Reloading";

    public AutoDeployHandler(StartTask start, File webappDirectory) {
        this.start = start;
        this.project = start.getProject();
        this.webappDirectory = webappDirectory;
        this.log = start.getLog();
        this.executorService = Executors.newSingleThreadExecutor();
        this.buildPath = project.getBasedir().toPath().resolve("target");
        this.ideaPath = project.getBasedir().toPath().resolve(".idea");
        this.eclipsePath = project.getBasedir().toPath().resolve(".settings");
        this.vscodePath = project.getBasedir().toPath().resolve(".vscode");
        this.eclipseClasspathPath = project.getBasedir().toPath().resolve(".classpath");
        this.eclipseProjectPath = project.getBasedir().toPath().resolve(".project");
        this.nbPath = project.getBasedir().toPath().resolve("nb-configuration.xml");
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
            // Create a set of paths to ignore that are directories
            Set<Path> ignoredDirectories = Set.of(
                    buildPath, ideaPath, eclipsePath, vscodePath
            );

            // Create a set of specific files to ignore
            Set<Path> ignoredFiles = Set.of(
                    eclipseClasspathPath, eclipseProjectPath, nbPath
            );

            while (isAlive()) {
                WatchKey key = watchService.poll(60, TimeUnit.SECONDS);
                if (key != null) {
                    List<WatchEvent<?>> filteredEvents = new ArrayList<>();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        Path fullPath = ((Path) key.watchable()).resolve(changed);

                        // Check if the fullPath is in an ignored directory
                        boolean isInIgnoredDirectory = ignoredDirectories.stream().anyMatch(fullPath::startsWith);
                        boolean isIgnoredFile = ignoredFiles.contains(fullPath);
                        boolean isTemporaryFile = fullPath.toString().endsWith("~");
                        boolean isDirectory = Files.isDirectory(fullPath);

                        // Skip the event if it's in an ignored directory, an ignored file, a temp file, or a directory
                        if (isInIgnoredDirectory || isIgnoredFile || isTemporaryFile || isDirectory) {
                            continue;
                        }
                        filteredEvents.add(event);
                    }
                    if (!filteredEvents.isEmpty()) {
                        boolean skip = false;
                        if (buildReloadTask != null && !buildReloadTask.isDone()) {
                            long duration = (System.currentTimeMillis() - buildReloadTaskStartTime);
                            if (duration < 1000 && sourceUpdatedPending.size() == filteredEvents.size()) {
                                skip = true;
                            }
                            log.debug("Duration : " + duration + ", skip : " + skip);
                            if (!skip) {
                                buildReloadTask.cancel(true);
                            }
                        }
                        if (!skip) {
                            boolean resourceModified = false;
                            boolean testClassesModified = false;
                            boolean testResourcesModified = false;
                            boolean classesModified = false;
                            boolean rebootRequired = false;
                            for (WatchEvent<?> event : filteredEvents) {
                                WatchEvent.Kind<?> kind = event.kind();
                                Path changed = (Path) event.context();
                                Path fullPath = ((Path) key.watchable()).resolve(changed);
                                log.debug("Source modified: " + changed + " - " + kind);

                                Path projectRoot = Paths.get(project.getBasedir().toURI());
                                Path sourceRoot = projectRoot.resolve(SRC_DIR);
                                Path mainDirectory = sourceRoot.resolve(MAIN_DIR);
                                Path javaDirectory = mainDirectory.resolve(JAVA_DIR);
                                Path resourcesDirectory = mainDirectory.resolve(RESOURCES_DIR);
                                Path testDirectory = sourceRoot.resolve(TEST_DIR);
                                Path javaTestDirectory = testDirectory.resolve(JAVA_DIR);
                                Path resourcesTestDirectory = testDirectory.resolve(RESOURCES_DIR);

                                sourceUpdatedPending.add(new Source(fullPath, kind, fullPath.startsWith(javaDirectory)));
                                if (fullPath.startsWith(resourcesDirectory)) {
                                    resourceModified = true;
                                }
                                if (fullPath.startsWith(resourcesTestDirectory)) {
                                    testResourcesModified = true;
                                }
                                if (fullPath.startsWith(javaTestDirectory)) {
                                    testClassesModified = true;
                                }
                                if (fullPath.startsWith(javaDirectory)) {
                                    classesModified = true;
                                }
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    if (Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS)) {
                                        register(fullPath); // register watch service for newly created dir
                                    }
                                }
                                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    cleanPending.set(true);
                                }
                                if (start.getRebootOnChange().contains(changed.toString())) {
                                    rebootRequired = true;
                                    cleanPending.set(true);
                                    break;
                                }
                            }

                            log.debug("sourceUpdatedPending: " + sourceUpdatedPending);
                            if (!sourceUpdatedPending.isEmpty()) {
                                WebDriverFactory.updateTitle("Building", project, start.getDriver(), log);
                                List<String> goalsList = updateGoalsList(classesModified, resourceModified, testClassesModified, testResourcesModified);
                                executeBuildReloadTask(goalsList, rebootRequired);
                            }
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

    private List<String> updateGoalsList(boolean classesModified, boolean resourceModified,
            boolean testClassesModified, boolean testResourcesModified) {
        boolean onlyJavaFilesUpdated = sourceUpdatedPending.stream()
                .allMatch(k -> k.getPath().toString().endsWith(JAVA_FILE_EXTENSION) && k.getKind() == ENTRY_MODIFY && k.isJavaClass());
        List<String> goalsList = new ArrayList<>();
        boolean clean = cleanPending.get();
        if (clean) {
            goalsList.add(0, GOAL_CLEAN);
        }
        if (clean || resourceModified) {
            goalsList.add(GOAL_PROCESS_RESOURCES);
        }
        if (clean || classesModified) {
            goalsList.add(GOAL_COMPILE);
            if (onlyJavaFilesUpdated) {
                goalsList.add(OPTION_DISABLE_INCREMENTAL_COMPILATION);
            }
        }
        if (!clean && start.isLocal() && onlyJavaFilesUpdated) {
            Path outputDirectory = Paths.get(webappDirectory.toPath().toString(), WEB_INF_DIRECTORY, CLASSES_DIRECTORY);
            goalsList.add(OPTION_OUTPUT_DIRECTORY + "\"" + outputDirectory.toString() + "\"");
        } else {
            goalsList.add(GOAL_WAR + ":" + (start.isLocal() ? GOAL_WAR_EXPLODED : GOAL_WAR));
        }
        if (!testClassesModified && !testResourcesModified) {
            goalsList.add(SKIP_TESTS_FLAG);
        } else {
            goalsList.add(SKIP_TESTS_OPTION);
        }
        for (Profile profile : project.getActiveProfiles()) {
            if (POM.equalsIgnoreCase(profile.getSource())) {
                goalsList.add("-P" + profile.getId() + " ");
            }
        }
        return goalsList;
    }

    private void executeBuildReloadTask(List<String> goalsList, boolean rebootRequired) {
        buildReloadTaskStartTime = System.currentTimeMillis();
        buildReloadTask = executorService.submit(() -> {
            String message = "Auto-build started for " + project.getName() + " with goals: " + goalsList;

            Invoker invoker = new DefaultInvoker();
            invoker.setLogger(new InvokerLoggerImpl(log));
            invoker.setInputStream(InputStream.nullInputStream());

            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(project.getBasedir(), POM_XML));
            System.setProperty(MAVEN_MULTI_MODULE_PROJECT_DIRECTORY, project.getBasedir().toString());
            if (goalsList.get(0).equals(GOAL_CLEAN)) {
                deleteBuildDir(project.getBuild().getDirectory());
                goalsList.remove(0);
            }
            request.setGoals(goalsList);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                return; // Exit if the thread is interrupted
            }
            log.info(message);
            try {
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    if (!buildReloadTask.isCancelled()) {
                        log.info("Auto-build failed with exit code: " + result.getExitCode());
                        WebDriverFactory.updateTitle("Build failed", project, start.getDriver(), log);
                    }
                } else {
                    log.info("Auto-build successful for " + project.getName());
                    cleanPending.set(false);
                    sourceUpdatedPending.clear();

                    reload(rebootRequired);
                    cleanPending.set(false);
                    sourceUpdatedPending.clear();
                }
            } catch (MavenInvocationException ex) {
                log.error("Error invoking Maven", ex);
            } catch (Throwable ex) {
                log.error("Error invoking Maven", ex);
            }
        });
    }

    public abstract void reload(boolean rebootRequired);

    public void deleteBuildDir(String filePath) {
        try {
            Path fileToDelete = Paths.get(filePath);
            Files.walkFileTree(fileToDelete, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new DeleteFileVisitor());
        } catch (IOException e) {
            log.error("Error occurred while deleting the file: ", e);
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
