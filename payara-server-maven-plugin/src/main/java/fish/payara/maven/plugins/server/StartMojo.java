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

import fish.payara.maven.plugins.server.parser.JDKVersion;
import fish.payara.maven.plugins.server.parser.JvmOption;
import fish.payara.maven.plugins.server.parser.JvmConfigReader;
import fish.payara.maven.plugins.server.utils.JavaUtils;
import fish.payara.maven.plugins.server.utils.ServerUtils;
import fish.payara.maven.plugins.server.utils.StringUtils;
import fish.payara.maven.plugins.AutoDeployHandler;
import fish.payara.maven.plugins.PropertiesUtils;
import fish.payara.maven.plugins.StartTask;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.toolchain.Toolchain;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fish.payara.maven.plugins.server.Configuration.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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

            try {
                ProcessBuilder processBuilder = startServer(new PayaraServerInstance("domain1", path));
                getLog().info("Starting Payara Server [" + path + "] with the these arguments: " + processBuilder.command());
                serverProcess = processBuilder.start();//re.exec(actualArgs.toArray(new String[0]));

                if (daemon) {
                    redirectStream(serverProcess.getInputStream(), System.out);
                    redirectStream(serverProcess.getErrorStream(), System.err);
                } else {
                    redirectStreamToGivenOutputStream(serverProcess.getInputStream(), System.out);
                    redirectStreamToGivenOutputStream(serverProcess.getErrorStream(), System.err);
                }

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
            Runtime.getRuntime().addShutdownHook(getShutdownHook());
            serverProcessorThread.run();

//            if (autoDeploy) {
//                while (autoDeployHandler.isAlive()) {
//                    serverProcessorThread.run();
//                }
//            }
        }
    }

    public ProcessBuilder startServer(PayaraServerInstance payaraServer) throws Exception {
        JvmConfigReader jvmConfigReader = new JvmConfigReader(payaraServer.getDomainXmlPath(), DAS_NAME);

        String javaHome = payaraServer.getJDKHome();
        if (javaHome == null) {
            throw new Exception("Java home path not found.");
        }

        JDKVersion javaVersion = JDKVersion.getJDKVersion(javaHome);
        if (javaVersion == null) {
            throw new Exception("Java version not found.");
        }

        List<String> optList = new ArrayList<>();
        for (JvmOption jvmOption : jvmConfigReader.getJvmOptions()) {
            if (JDKVersion.isCorrectJDK(javaVersion, jvmOption.getVendor(), jvmOption.getMinVersion(), jvmOption.getMaxVersion())) {
                optList.add(jvmOption.getOption());
            }
        }

        Map<String, String> propMap = jvmConfigReader.getPropMap();
        addJavaAgent(payaraServer, jvmConfigReader);

        String bootstrapJar = Paths.get(payaraServer.getServerModules(), "glassfish.jar").toString();
        if (!Files.exists(Paths.get(bootstrapJar))) {
            throw new Exception("No bootstrap jar exists.");
        }

        String classPath = "";
        String javaOpts;
        String payaraArgs;

        Map<String, String> varMap = varMap(payaraServer, javaHome);

        String debugOpt = propMap.get("debug-options");
        if (debug != null && !debug.equalsIgnoreCase("false") && debugOpt != null) {
            if (Boolean.parseBoolean(debug)) {
                if (isValidPort(debugPort)) {
                    debugOpt = debugOpt.replaceAll("address=\\d+", "address=" + debugPort);
                }
                optList.add(debugOpt);
            } else if (!"false".equals(debug)) {
                optList.add(debug);
            }
            optList.add(debugOpt);
        }

        javaOpts = appendOptions(optList, varMap);
        javaOpts += appendVarMap(varMap);
        payaraArgs = appendPayaraArgs(getPayaraArgs(payaraServer));

        String javaVmExe = JavaUtils.javaVmExecutableFullPath(javaHome);
        if (!Files.exists(Paths.get(javaVmExe))) {
            throw new Exception("Java VM executable for " + payaraServer.getPath() + " was not found.");
        }

        String allArgs = String.join(" ",
                javaVmExe,
                javaOpts,
                "-jar", bootstrapJar,
                "--classpath", classPath,
                payaraArgs);
        List<String> args = JavaUtils.parseParameters(allArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(new File(payaraServer.getPath()));
        return processBuilder;
    }

    private boolean isValidPort(String portStr) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return false;
        }
        try {
            int port = Integer.parseInt(portStr.trim());
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addJavaAgent(PayaraServerInstance payaraServer, JvmConfigReader jvmConfigReader) throws Exception {
        List<JvmOption> optList = jvmConfigReader.getJvmOptions();
        String serverHome = payaraServer.getServerHome();

        File monitor = Paths.get(serverHome, "lib", "monitor").toFile();
        File btrace = Paths.get(monitor.getPath(), "btrace-agent.jar").toFile();
        File flight = Paths.get(monitor.getPath(), "flashlight-agent.jar").toFile();

        if (jvmConfigReader.isMonitoringEnabled()) {
            if (btrace.exists()) {
                optList.add(new JvmOption("-javaagent:" + StringUtils.quote(btrace.getPath()) + "=unsafe=true,noServer=true"));
            } else if (flight.exists()) {
                optList.add(new JvmOption("-javaagent:" + StringUtils.quote(flight.getPath())));
            }
        }
    }

    private Map<String, String> varMap(PayaraServerInstance payaraServer, String javaHome) {
        Map<String, String> varMap = new HashMap<>();
        varMap.put(ServerUtils.PF_HOME_PROPERTY, payaraServer.getServerHome());
        varMap.put(ServerUtils.PF_DOMAIN_ROOT_PROPERTY, payaraServer.getDomainPath());
        varMap.put(ServerUtils.PF_JAVA_ROOT_PROPERTY, javaHome);
        varMap.put(JavaUtils.PATH_SEPARATOR, File.pathSeparator);
        return varMap;
    }

    private String appendOptions(List<String> optList, Map<String, String> varMap) {
        StringBuilder argumentBuf = new StringBuilder();
        List<String> moduleOptions = new ArrayList<>();
        Map<String, String> keyValueArgs = new HashMap<>();
        List<String> keyOrder = new ArrayList<>();

        for (String opt : optList) {
            opt = StringUtils.doSub(opt.trim(), varMap);
            int splitIndex = opt.indexOf('=');
            String name, value = null;
            if (splitIndex != -1 && !opt.startsWith("-agentpath:")) {
                name = opt.substring(0, splitIndex);
                value = StringUtils.quote(opt.substring(splitIndex + 1));
            } else {
                name = opt;
            }

            if (name.startsWith("--add-")) {
                moduleOptions.add(opt);
            } else {
                if (!keyValueArgs.containsKey(name)) {
                    keyOrder.add(name);
                }
                keyValueArgs.put(name, value);
            }
        }

        argumentBuf.append(String.join(" ", moduleOptions));
        for (String key : keyOrder) {
            argumentBuf.append(" ").append(key);
            if (keyValueArgs.get(key) != null) {
                argumentBuf.append("=").append(keyValueArgs.get(key));
            }
        }
        return argumentBuf.toString();
    }

    private String appendVarMap(Map<String, String> varMap) {
        StringBuilder javaOpts = new StringBuilder();
        varMap.forEach((key, value) -> javaOpts.append(" ").append(JavaUtils.systemProperty(key, value)));
        return javaOpts.toString();
    }

    private List<String> getPayaraArgs(PayaraServerInstance payaraServer) {
        List<String> payaraArgs = new ArrayList<>();
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_ARG, payaraServer.getDomainName()));
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_DIR_ARG, StringUtils.quote(payaraServer.getDomainPath())));
        return payaraArgs;
    }

    private String appendPayaraArgs(List<String> payaraArgsList) {
        return String.join(" ", payaraArgsList).trim();
    }

    private Thread getShutdownHook() {
        return new Thread(threadGroup, () -> {
            if (serverProcess != null && serverProcess.isAlive()) {
                try {
                    serverProcess.destroy();
                    serverProcess.waitFor(1, TimeUnit.MINUTES);
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
