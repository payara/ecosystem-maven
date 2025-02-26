/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.maven.plugins.server;

import fish.payara.maven.plugins.server.manager.PayaraServerRemoteInstance;
import fish.payara.maven.plugins.server.manager.RemoteInstanceManager;
import fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance;
import fish.payara.maven.plugins.server.manager.LocalInstanceManager;
import fish.payara.maven.plugins.server.manager.InstanceManager;
import fish.payara.maven.plugins.AutoDeployHandler;
import fish.payara.maven.plugins.StartTask;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fish.payara.maven.plugins.server.Configuration.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.project.MavenProject;
import org.openqa.selenium.WebDriver;

/**
 * Run mojo that executes payara-server
 *
 * @author Gaurav Gupta
 */
@Mojo(name = "start")
public class StartMojo extends ServerMojo implements StartTask {

    private static final String ERROR_MESSAGE = "Errors occurred while executing payara-server.";
    private static final String REMOTE_INSTANCE_NOT_RUNNING_MESSAGE = "The remote Payara server instance is not running.";

    @Parameter(property = "daemon", defaultValue = "false")
    private boolean daemon;

    @Parameter(property = "immediateExit", defaultValue = "false")
    private boolean immediateExit;

    /**
     * Attach a debugger. If set to "true", the process will suspend and wait
     * for a debugger to attach on port 5005. If set to other value, will be
     * appended to the argLine, allowing you to configure custom debug options.
     *
     */
    @Parameter(property = "debug", defaultValue = "false")
    protected String debug;

    @Parameter(property = "debugPort")
    protected String debugPort;

    private Process serverProcess;
    private Thread serverProcessorThread;
    private final ThreadGroup threadGroup;
    private Toolchain toolchain;

    private AutoDeployHandler autoDeployHandler;
    private final List<String> rebootOnChange = new ArrayList<>();
    private WebDriver driver;
    private String payaraServerURL;
    private InstanceManager serverManager;
    private String appPath, projectName;

    StartMojo() {
        threadGroup = new ThreadGroup(SERVER_THREAD_NAME);
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Start mojo execution is skipped");
            return;
        }

        toolchain = getToolchain();
        final String path = decideOnWhichServerToUse();

        serverProcessorThread = new Thread(threadGroup, () -> {
            if (remote) {
                PayaraServerRemoteInstance instance = new PayaraServerRemoteInstance(host);
                instance.setAdminUser(adminUser);
                instance.setAdminPassword(getAdminPassword());
                if (adminPort != null) {
                    instance.setAdminPort(Integer.parseInt(adminPort));
                }
                if (httpPort != null) {
                    instance.setHttpPort(Integer.parseInt(httpPort));
                }
                if (httpsPort != null) {
                    instance.setHttpsPort(Integer.parseInt(httpsPort));
                }
                if (protocol != null) {
                    instance.setProtocol(protocol);
                }
                serverManager = new RemoteInstanceManager(instance, getLog());
                if (serverManager.isServerAlreadyRunning()) {
                    Thread logThread = streamRemoteServerLog();
                    String appPath = evaluateProjectArtifactAbsolutePath("." + mavenProject.getPackaging());
                    String projectName = mavenProject.getName().replaceAll("\\s+", "");
                    serverManager.undeployApplication(projectName, instanceName);
                    serverManager.deployApplication(projectName, appPath, instanceName, contextRoot);
                    try {
                        logThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new RuntimeException(REMOTE_INSTANCE_NOT_RUNNING_MESSAGE);
                }
            } else {
                try {
                    PayaraServerLocalInstance instance = new PayaraServerLocalInstance(domain, path);
                    instance.setAdminUser(adminUser);
                    instance.setAdminPassword(getAdminPassword());
                    if (adminPort != null) {
                        instance.setAdminPort(Integer.parseInt(adminPort));
                    }
                    if (httpPort != null) {
                        instance.setHttpPort(Integer.parseInt(httpPort));
                    }
                    if (httpsPort != null) {
                        instance.setHttpsPort(Integer.parseInt(httpsPort));
                    }
                    if (protocol != null) {
                        instance.setProtocol(protocol);
                    }
                    serverManager = new LocalInstanceManager(instance, getLog());
                    if (!serverManager.isServerAlreadyRunning()) {
                        ProcessBuilder processBuilder = ((LocalInstanceManager) serverManager).startServer(debug, debugPort);
                        getLog().info("Starting Payara Server [" + path + "] with the these arguments: " + processBuilder.command());
                        serverProcess = processBuilder.start();

                        if (daemon) {
                            redirectStream(serverProcess.getInputStream(), System.out);
                            redirectStream(serverProcess.getErrorStream(), System.err);
                        } else {
                            redirectStreamToGivenOutputStream(serverProcess.getInputStream(), System.out);
                            redirectStreamToGivenOutputStream(serverProcess.getErrorStream(), System.err);
                        }
                        serverManager.connectWithServer();
                    } else {
                        streamLocalServerLog(instance);
                    }
                    if (exploded) {
                        appPath = evaluateProjectArtifactAbsolutePath("");
                    } else {
                        appPath = evaluateProjectArtifactAbsolutePath("." + mavenProject.getPackaging());
                    }
                    projectName = mavenProject.getName().replaceAll("\\s+", "");
                    serverManager.undeployApplication(projectName, instanceName);
                    serverManager.deployApplication(projectName, appPath, instanceName, contextRoot);
                    watchAsadminCommand();
                    int exitCode = serverProcess.waitFor();
                    if (exitCode != 0) { // && !autoDeploy
                        throw new MojoFailureException(ERROR_MESSAGE);
                    }
                } catch (InterruptedException ignored) {
                } catch (Exception e) {
                    throw new RuntimeException(ERROR_MESSAGE, e);
                } finally {
                    if (!daemon) {
                        closeServerProcess();
                    }
                }
            }
        });

        if (daemon) {
            serverProcessorThread.setDaemon(true);
            serverProcessorThread.start();

            if (!immediateExit) {
                try {
                    serverProcessorThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Runtime.getRuntime().addShutdownHook(killServerProcess());
            serverProcessorThread.run();

//            if (autoDeploy) {
//                while (autoDeployHandler.isAlive()) {
//                    serverProcessorThread.run();
//                }
//            }
        }
    }

    private Thread killServerProcess() {
        return new Thread(threadGroup, () -> {
            if (serverProcess != null && serverProcess.isAlive()) {
                try {
                    serverManager.undeployApplication(projectName, instanceName);
                    serverProcess.destroy();
                    serverProcess.waitFor(15, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                } finally {
                    serverProcess.destroyForcibly();
                }
            }
        });
    }

    private String evaluateJavaPath() {
        String javaToUse = JAVA_EXECUTABLE;

        if (org.apache.commons.lang.StringUtils.isNotEmpty(javaPath)) {
            javaToUse = javaPath;
        } else if (toolchain != null) {
            javaToUse = toolchain.findTool(JAVA_EXECUTABLE);
        }
        return javaToUse;
    }

    private String decideOnWhichServerToUse() throws MojoExecutionException {
        if (payaraServerAbsolutePath != null) {
            return payaraServerAbsolutePath;
        }

        if (artifactItem.getGroupId() != null) {
            DefaultArtifact artifact = new DefaultArtifact(artifactItem.getGroupId(),
                    artifactItem.getArtifactId(),
                    artifactItem.getVersion(),
                    null,
                    artifactItem.getType(),
                    artifactItem.getClassifier(),
                    new DefaultArtifactHandler("zip"));
            String targetDir = getBaseDir() + File.separator + "payara-server-" + payaraVersion;
            // Check if the target directory already exists
            File extractedDir = new File(targetDir + File.separator + "payara" + artifactItem.getVersion().charAt(0));
            if (!extractedDir.exists()) {
                try {
                    extractZipFile(findLocalPathOfArtifact(artifact), targetDir);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to extract Payara Server zip file", e);
                }
            }

            return extractedDir.getAbsolutePath();
        }

        if (payaraVersion != null) {
            MojoExecutor.ExecutionEnvironment environment = getEnvironment();
            ServerFetchProcessor serverFetchProcessor = new ServerFetchProcessor();
            serverFetchProcessor.set(payaraVersion).handle(environment);

            // after downloading extract the zip
            DefaultArtifact artifact = new DefaultArtifact(SERVER_GROUPID, SERVER_ARTIFACTID,
                    payaraVersion,
                    null,
                    "zip",
                    null,
                    new DefaultArtifactHandler("zip"));
            String targetDir = getBaseDir() + File.separator + "payara-server-" + payaraVersion;
            // Check if the target directory already exists
            File extractedDir = new File(targetDir + File.separator + "payara" + payaraVersion.charAt(0));
            if (!extractedDir.exists()) {
                try {
                    extractZipFile(findLocalPathOfArtifact(artifact), targetDir);
                } catch (IOException e) {
                    throw new MojoExecutionException("Failed to extract Payara Server zip file", e);
                }
            }

            return extractedDir.getAbsolutePath();
        }

        throw new MojoExecutionException("Could not determine Payara Server path. Please set it by defining either \"useUberJar\", \"payaraServerAbsolutePath\" or \"artifactItem\" configuration options.");
    }

    private String findLocalPathOfArtifact(DefaultArtifact artifact) {
        Artifact payaraServerArtifact = mavenSession.getLocalRepository().find(artifact);
        return payaraServerArtifact.getFile().getAbsolutePath();
    }

    private void extractZipFile(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) {
            dir.mkdirs(); // Create destination directory if it doesn't exist
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // If the entry is a file, extract it
                    extractFile(zipIn, filePath);
                } else {
                    // If the entry is a directory, create it
                    File dirEntry = new File(filePath);
                    dirEntry.mkdirs();
                }
                zipIn.closeEntry();
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zipIn.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }

    protected String getBaseDir() {
        return mavenProject.getBuild().getDirectory();
    }

    private String evaluateProjectArtifactAbsolutePath(String extension) {
        String projectJarAbsolutePath = getBaseDir() + File.separator;
        projectJarAbsolutePath += evaluateExecutorName(extension);
        return projectJarAbsolutePath;
    }

    private String evaluateExecutorName(String extension) {
        if (org.apache.commons.lang.StringUtils.isNotEmpty(mavenProject.getBuild().getFinalName())) {
            return mavenProject.getBuild().getFinalName() + extension;
        }
        return mavenProject.getArtifactId() + '-' + mavenProject.getVersion() + extension;
    }

    void closeServerProcess() {
        if (serverProcess != null) {
            try {
                serverProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                serverProcess.destroy();
                getLog().info("Terminated payara-server.");
            }
        }
    }

    Process getServerProcess() {
        return this.serverProcess;
    }

    private void watchAsadminCommand() {
        Thread thread = new Thread(threadGroup, () -> {
            try (Scanner scanner = new Scanner(System.in)) {
                String userQuery = null;

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    Thread.currentThread().interrupt();
                }));

                while (!Thread.currentThread().isInterrupted() && !"exit".equals(userQuery)) {
                    try {
                        if (scanner.hasNextLine()) {
                            userQuery = scanner.nextLine();
                        } else {
                            Thread.currentThread().interrupt();
                            break; // Exit if input stream closes
                        }
                    } catch (java.util.NoSuchElementException nsee) {
                        break;
                    }
                    try {
                        if (userQuery.startsWith("asadmin")) {
                            if (serverManager instanceof LocalInstanceManager) {
                               String repsonse = ((LocalInstanceManager) serverManager).runAsadminCommand(userQuery.substring(8));
                               getLog().info(repsonse);
                            }
                        } else if (userQuery.equals("deploy")) {
                            serverManager.undeployApplication(projectName, instanceName);
                            serverManager.deployApplication(projectName, appPath, instanceName, contextRoot);
                        } else if (userQuery.equals("undeploy")) {
                            serverManager.undeployApplication(projectName, instanceName);
                        } else if (userQuery.equals("exit")) {
                            Thread.currentThread().interrupt();
                            getLog().info("watchAsadminCommand exit");
                            killServerProcess().start();
                            break;
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(StartMojo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        thread.setDaemon(false);
        thread.start();
    }

    private Thread streamRemoteServerLog() {
        final Thread thread = new Thread(threadGroup, () -> {
            try {
                while (true) {
                    String log = ((RemoteInstanceManager) serverManager).fetchLogs(instanceName);
                    if (log != null && !log.isEmpty()) {
                        System.out.println(log);
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setDaemon(false);
        thread.start();
        return thread;
    }

    private void streamLocalServerLog(PayaraServerLocalInstance instance) {
        final Thread thread = new Thread(threadGroup, () -> {
            File logFile = new File(instance.getServerLog());
            if (logFile.exists()) {
                try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                    raf.seek(raf.length());
                    String line;
                    while (true) {
                        while ((line = raf.readLine()) != null) {
                            System.out.println(line);
                        }
                        Thread.sleep(1000);
                    }
                } catch (IOException e) {
                    getLog().error("Error occurred while streaming server.log", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                getLog().warn("Log file does not exist: " + logFile.getAbsolutePath());
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    private void redirectStream(final InputStream inputStream, final PrintStream printStream) {
        final Thread thread = new Thread(threadGroup, () -> {
            BufferedReader br;
            StringBuilder sb = new StringBuilder();

            String line;
            try {
                br = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    printStream.println(line);
                    if (!immediateExit && sb.toString().contains(SERVER_READY_MESSAGE)) {
                        serverProcessorThread.interrupt();
                        br.close();
                        break;
                    }
                }
            } catch (IOException e) {
                getLog().error(ERROR_MESSAGE, e);
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    private void redirectStreamToGivenOutputStream(final InputStream inputStream, final OutputStream outputStream) {
        Thread thread = new Thread(threadGroup, () -> {
            try {
                if (outputStream instanceof PrintStream) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    PrintStream printStream = (PrintStream) outputStream;

                    while ((line = br.readLine()) != null) {
                        printStream.println(line);
                    }
                } else {
                    IOUtils.copy(inputStream, outputStream);
                }
            } catch (IOException e) {
                getLog().error("Error occurred while reading stream", e);
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public MavenProject getProject() {
        return this.getEnvironment().getMavenProject();
    }

    @Override
    public List<String> getRebootOnChange() {
        return rebootOnChange;
    }

    public WebDriver getDriver() {
        return null;
    }

    public boolean isLocal() {
        return true;
    }
}
