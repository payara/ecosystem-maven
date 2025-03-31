/*
 *
 * Copyright (c) 2017-2024 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.maven.plugins.LogUtils;
import fish.payara.maven.plugins.AutoDeployHandler;
import fish.payara.maven.plugins.PropertiesUtils;
import fish.payara.maven.plugins.StartTask;
import fish.payara.maven.plugins.WebDriverFactory;
import fish.payara.maven.plugins.micro.processor.MicroFetchProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.toolchain.Toolchain;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fish.payara.maven.plugins.micro.Configuration.*;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.WebDriver;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Run mojo that executes payara-micro
 *
 * @author mertcaliskan, Gaurav Gupta
 */
@Mojo(name = "start")
public class StartMojo extends BasePayaraMojo implements StartTask {

    private static final String ERROR_MESSAGE = "Errors occurred while executing payara-micro.";
    private static final String PRE_BOOT = "--prebootcommandfile";
    private static final String POST_BOOT = "--postbootcommandfile";
    private static final String POST_DEPLOY = "--postdeploycommandfile";

    @Parameter(property = "payara.java.home", defaultValue = "${env.PAYARA_JAVA_HOME}")
    private String javaHome;

    @Parameter(property = "payara.micro.version", defaultValue = "${env.PAYARA_MICRO_VERSION}")
    private String payaraMicroVersion;

    @Parameter(property = "payara.micro.path", defaultValue = "${env.PAYARA_MICRO_PATH}")
    private String payaraMicroPath;

    @Deprecated
    @Parameter(property = "javaPath")
    private String javaPath;

    @Deprecated
    @Parameter(property = "payaraVersion")
    private String payaraVersion;

    @Deprecated
    @Parameter(property = "payaraMicroAbsolutePath")
    private String payaraMicroAbsolutePath;

    @Parameter(property = "payara.daemon", defaultValue = "${env.PAYARA_DAEMON}")
    private boolean daemon;

    @Parameter(property = "payara.immediate.exit", defaultValue = "${env.PAYARA_IMMEDIATE_EXIT}")
    private boolean immediateExit;

    @Parameter
    private ArtifactItem artifactItem;

    @Parameter(property = "payara.use.uber.jar", defaultValue = "${env.PAYARA_USE_UBER_JAR}")
    private boolean useUberJar;

    @Parameter(property = "payara.deploy.war", defaultValue = "${env.PAYARA_DEPLOY_WAR}")
    protected boolean deployWar;

    @Parameter(property = "payara.exploded", defaultValue = "${env.PAYARA_EXPLODED}")
    protected boolean exploded;

    @Parameter(property = "payara.auto.deploy", defaultValue = "${env.PAYARA_AUTO_DEPLOY}")
    protected Boolean autoDeploy;

    @Parameter(property = "payara.keep.state", defaultValue = "${env.PAYARA_KEEP_STATE}")
    protected Boolean keepState;

    @Parameter(property = "payara.live.reload", defaultValue = "${env.PAYARA_LIVE_RELOAD}")
    protected Boolean liveReload;

    @Parameter(property = "payara.browser", defaultValue = "${env.PAYARA_BROWSER}")
    protected String browser;

    @Parameter(property = "payara.trim.log", defaultValue = "${env.PAYARA_TRIM_LOG}")
    protected Boolean trimLog;

    @Parameter(property = "payara.debug", defaultValue = "${env.PAYARA_DEBUG}")
    protected String debug;

    @Parameter(property = "payara.context.root", defaultValue = "${env.PAYARA_CONTEXT_ROOT}")
    protected String contextRoot;

    @Parameter(property = "payara.hot.deploy", defaultValue = "${env.PAYARA_HOT_DEPLOY}")
    protected boolean hotDeploy;

    /**
     * The directory where the webapp is built, default value is exploded war.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
    protected File webappDirectory;

    /**
     * Property passed by Apache NetBeans IDE to set contextRoot of the
     * application
     */
    @Parameter(property = "netbeans.deploy.clientUrlPart")
    private String clientUrlPart;

    @Deprecated
    @Parameter(property = "copySystemProperties", defaultValue = "false")
    private boolean copySystemProperties;

    @Parameter
    protected List<Option> commandLineOptions;

    @Parameter
    private List<Option> javaCommandLineOptions;

    @Parameter
    private List<ArtifactItem> classpathArtifactItems;

    private Process microProcess;
    private Thread microProcessorThread;
    private final ThreadGroup threadGroup;
    private Toolchain toolchain;

    private AutoDeployHandler autoDeployHandler;
    private final List<String> rebootOnChange = new ArrayList<>();
    private WebDriver driver;
    private String payaraMicroURL;
    private String hostIp, hostPort;
    private final Map<String, String> contextRoots = new HashMap<>();

    StartMojo() {
        threadGroup = new ThreadGroup(MICRO_THREAD_NAME);
        
        // Backward compatibility for params
        if (javaPath != null) {
            javaHome = javaPath;
        }
        if (payaraVersion != null) {
            payaraMicroVersion = payaraVersion;
        }
        if (payaraMicroAbsolutePath != null) {
            payaraMicroPath = payaraMicroAbsolutePath;
        }
        if (System.getProperty("daemon") != null) {
            daemon = Boolean.parseBoolean(System.getProperty("daemon"));
        }
        if (System.getProperty("immediateExit") != null) {
            immediateExit = Boolean.parseBoolean(System.getProperty("immediateExit"));
        }
        if (System.getProperty("deployWar") != null) {
            deployWar = Boolean.parseBoolean(System.getProperty("deployWar"));
        }
        if (System.getProperty("exploded") != null) {
            exploded = Boolean.parseBoolean(System.getProperty("exploded"));
        }
        if (System.getProperty("autoDeploy") != null) {
            autoDeploy = Boolean.valueOf(System.getProperty("autoDeploy"));
        }
        if (System.getProperty("keepState") != null) {
            keepState = Boolean.valueOf(System.getProperty("keepState"));
        }
        if (System.getProperty("liveReload") != null) {
            liveReload = Boolean.valueOf(System.getProperty("liveReload"));
        }
        if (System.getProperty("browser") != null) {
            browser = System.getProperty("browser");
        }
        if (System.getProperty("trimLog") != null) {
            trimLog = Boolean.valueOf(System.getProperty("trimLog"));
        }
        if (System.getProperty("debug") != null) {
            debug = System.getProperty("debug");
        }
        if (System.getProperty("contextRoot") != null) {
            contextRoot = System.getProperty("contextRoot");
        }
        if (System.getProperty("hotDeploy") != null) {
            hotDeploy = Boolean.parseBoolean(System.getProperty("hotDeploy"));
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (trimLog == null) {
            trimLog = false;
        }
        if (autoDeploy == null) {
            autoDeploy = false;
        }
        if (liveReload == null) {
            liveReload = false;
        }
        if (keepState == null) {
            keepState = false;
        }
        if (autoDeploy && autoDeployHandler == null) {
            autoDeployHandler = new MicroAutoDeployHandler(this, webappDirectory);
            Thread devModeThread = new Thread(autoDeployHandler);
            devModeThread.setDaemon(true);
            devModeThread.start();
        } else {
            autoDeployHandler = null;
        }

        if (copySystemProperties) {
            getLog().warn("copySystemProperties is deprecated. "
                    + "System properties of the regarding maven execution "
                    + "will be passed to the payara-micro automatically.");
        }

        if (skip) {
            getLog().info("Start mojo execution is skipped");
            return;
        }

        toolchain = getToolchain();
        final String path = decideOnWhichMicroToUse();

        microProcessorThread = new Thread(threadGroup, () -> {

            final List<String> actualArgs = new ArrayList<>();
            getLog().info("Starting payara-micro from path: " + path);
            int indice = 0;
            actualArgs.add(indice++, evaluateJavaPath());

            if (debug != null && !debug.equalsIgnoreCase("false")) {
                if (Boolean.parseBoolean(debug)) {
                    actualArgs.add(indice++, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
                } else {
                    actualArgs.add(indice++, debug);
                }
            }

            if (javaCommandLineOptions != null) {
                for (Option option : javaCommandLineOptions) {
                    if (option.getKey() != null && option.getValue() != null) {
                        String systemProperty = String.format("%s=%s", option.getKey(), option.getValue());
                        actualArgs.add(indice++, systemProperty);
                    } else if (option.getValue() != null) {
                        actualArgs.add(indice++, option.getValue());
                    }
                }
            }

            String execArgs = mavenSession.getRequest().getUserProperties().getProperty("exec.args");
            if (execArgs != null && !execArgs.trim().isEmpty()) {
                for (String execArg : execArgs.split("\\s+")) {
                    actualArgs.add(indice++, execArg);
                }
            }

            actualArgs.add(indice++, "-Dgav=" + getProjectGAV());
            if (classpathArtifactItems != null && !classpathArtifactItems.isEmpty()) {
                actualArgs.add(indice++, "-cp");
                List<String> artifactsPath = new ArrayList<>();
                for (ArtifactItem classpathArtifactItem : classpathArtifactItems) {
                    DefaultArtifact artifact = new DefaultArtifact(classpathArtifactItem.getGroupId(),
                            classpathArtifactItem.getArtifactId(),
                            classpathArtifactItem.getVersion(),
                            null,
                            JAR_EXTENSION,
                            null,
                            new DefaultArtifactHandler(JAR_EXTENSION));
                    artifactsPath.add(findLocalPathOfArtifact(artifact));
                }
                artifactsPath.add(path);
                actualArgs.add(indice++, StringUtils.join(artifactsPath, File.pathSeparator));
                actualArgs.add(indice++, "fish.payara.micro.PayaraMicro");
            } else {
                actualArgs.add(indice++, "-jar");
                actualArgs.add(indice++, path);
            }
            if (deployWar && WAR_EXTENSION.equalsIgnoreCase(mavenProject.getPackaging())) {
                if (useUberJar) {
                    getLog().warn("useUberJar and deployWar are both set to true! You'll probably have "
                            + "your application tried to deploy twice: 1. as uber jar 2. as a separate war");
                }
                actualArgs.add(indice++, "--deploy");
                if (exploded) {
                    actualArgs.add(indice++, evaluateProjectArtifactAbsolutePath(""));
                } else {
                    actualArgs.add(indice++, evaluateProjectArtifactAbsolutePath("." + mavenProject.getPackaging()));
                }
            }
            if (clientUrlPart != null && !clientUrlPart.trim().isEmpty()) {
                actualArgs.add(indice++, "--contextroot");
                actualArgs.add(indice++, clientUrlPart.trim());
            } else if (contextRoot != null) {
                actualArgs.add(indice++, "--contextroot");
                actualArgs.add(indice++, contextRoot);
            }
            if (hotDeploy) {
                actualArgs.add(indice++, "--hotdeploy");
            }
            if (commandLineOptions != null) {
                for (Option option : commandLineOptions) {
                    if (option.getKey() != null) {
                        actualArgs.add(indice++, option.getKey());
                        if (autoDeploy
                                && option.getValue() != null
                                && !option.getValue().isEmpty()
                                && (option.getKey().equals(PRE_BOOT)
                                || option.getKey().equals(POST_BOOT)
                                || option.getKey().equals(POST_DEPLOY))) {
                            Path bootpath = Paths.get(option.getValue());
                            if (Files.exists(bootpath)) {
                                rebootOnChange.add(bootpath.getFileName().toString());
                            }
                        }
                    }
                    if (option.getValue() != null) {
                        actualArgs.add(indice++, option.getValue());
                    }
                }
            }

            try {
                getLog().info("Starting Payara Micro with the these arguments: " + actualArgs);
                final Runtime re = Runtime.getRuntime();
                microProcess = re.exec(actualArgs.toArray(new String[0]));

                if (daemon) {
                    redirectStream(microProcess.getInputStream(), System.out);
                    redirectStream(microProcess.getErrorStream(), System.err);
                } else {
                    redirectStreamToGivenOutputStream(microProcess.getInputStream(), System.out);
                    redirectStreamToGivenOutputStream(microProcess.getErrorStream(), System.err);
                }

                int exitCode = microProcess.waitFor();
                if (exitCode != 0 && !autoDeploy) {
                    throw new MojoFailureException(ERROR_MESSAGE);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(ERROR_MESSAGE, e);
            } finally {
                if (!daemon) {
                    closeMicroProcess();
                }
            }
        });

        if (daemon) {
            microProcessorThread.setDaemon(true);
            microProcessorThread.start();

            if (!immediateExit) {
                try {
                    microProcessorThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Runtime.getRuntime().addShutdownHook(getShutdownHook());
            microProcessorThread.run();

            if (autoDeploy) {
                while (autoDeployHandler.isAlive()) {
                    microProcessorThread.run();
                }
            }
        }
    }

    private Thread getShutdownHook() {
        return new Thread(threadGroup, () -> {
            if (microProcess != null && microProcess.isAlive()) {
                try {
                    microProcess.destroy();
                    microProcess.waitFor(1, TimeUnit.MINUTES);
                } catch (InterruptedException ignored) {
                } finally {
                    microProcess.destroyForcibly();
                }
            }
            if (autoDeployHandler != null) {
                autoDeployHandler.stop();
            }
            if (driver != null) {
                try {
                    PropertiesUtils.saveProperties(payaraMicroURL, driver.getCurrentUrl());
                } catch (Throwable t) {
                    getLog().debug(t);
                } finally {
                    try {
                        driver.quit();
                    } catch (Throwable t) {
                        getLog().debug(t);
                    }
                }
            }
        });
    }

    private String evaluateJavaPath() {
        String javaToUse = JAVA_EXECUTABLE;

        if (StringUtils.isNotEmpty(javaHome)) {
            javaToUse = javaHome;
        } else if (toolchain != null) {
            javaToUse = toolchain.findTool(JAVA_EXECUTABLE);
        }
        return javaToUse;
    }

    private String decideOnWhichMicroToUse() throws MojoExecutionException {
        if (useUberJar) {
            String path = evaluateProjectArtifactAbsolutePath("-" + uberJarClassifier + "." + JAR_EXTENSION);

            if (!Files.exists(Paths.get(path))) {
                throw new MojoExecutionException("\"useUberJar\" option was set to \"true\" but detected path " + path + " does not exist. You need to execute the \"bundle\" goal before using this option.");
            }

            return path;
        }

        if (payaraMicroPath != null) {
            return payaraMicroPath;
        }

        if (artifactItem.getGroupId() != null) {
            DefaultArtifact artifact = new DefaultArtifact(artifactItem.getGroupId(),
                    artifactItem.getArtifactId(),
                    artifactItem.getVersion(),
                    null,
                    artifactItem.getType(),
                    artifactItem.getClassifier(),
                    new DefaultArtifactHandler("jar"));
            return findLocalPathOfArtifact(artifact);
        }

        if (payaraMicroVersion == null) {
            PayaraMicroVersionSelector payaraMicroVersionSelector = new PayaraMicroVersionSelector(mavenProject, getLog());
            payaraMicroVersion = payaraMicroVersionSelector.fetchPayaraVersion();
        }
        if (payaraMicroVersion != null) {
            MojoExecutor.ExecutionEnvironment environment = getEnvironment();
            MicroFetchProcessor microFetchProcessor = new MicroFetchProcessor();
            microFetchProcessor.set(payaraMicroVersion).handle(environment);

            DefaultArtifact artifact = new DefaultArtifact(MICRO_GROUPID, MICRO_ARTIFACTID,
                    payaraMicroVersion,
                    null,
                    "jar",
                    null,
                    new DefaultArtifactHandler("jar"));
            return findLocalPathOfArtifact(artifact);
        }

        throw new MojoExecutionException("Could not determine Payara Micro path. Please set it by defining either \"useUberJar\", \"payaraMicroAbsolutePath\" or \"artifactItem\" configuration options.");
    }

    private String findLocalPathOfArtifact(DefaultArtifact artifact) {
        Artifact payaraMicroArtifact = mavenSession.getLocalRepository().find(artifact);
        return payaraMicroArtifact.getFile().getAbsolutePath();
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
        if (StringUtils.isNotEmpty(mavenProject.getBuild().getFinalName())) {
            return mavenProject.getBuild().getFinalName() + extension;
        }
        return mavenProject.getArtifactId() + '-' + mavenProject.getVersion() + extension;
    }

    void closeMicroProcess() {
        if (microProcess != null) {
            try {
                microProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                microProcess.destroy();
                getLog().info("Terminated payara-micro.");
            }
        }
    }

    Process getMicroProcess() {
        return this.microProcess;
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
                    if (!immediateExit && sb.toString().contains(MICRO_READY_MESSAGE)) {
                        microProcessorThread.interrupt();
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
                if (liveReload && outputStream instanceof PrintStream) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    PrintStream printStream = (PrintStream) outputStream;

                    while ((line = br.readLine()) != null) {
                        printStream.println(trimLog ? LogUtils.trimLog(line) : line);
                        if (hostIp == null && line.endsWith(INSTANCE_CONFIGURATION)) {
                            parseInstanceConfig(br, printStream);
                        } else if (payaraMicroURL == null && line.contains(PAYARA_MICRO_URLS)) {
                            parseMicroUrl(br, printStream);
                        } else if (line.contains(APP_DEPLOYMENT_FAILED)) {
                            WebDriverFactory.updateTitle(APP_DEPLOYMENT_FAILED_MESSAGE, getEnvironment().getMavenProject(), driver, this.getLog());
                        } else if (payaraMicroURL != null
                                && payaraMicroURL.isEmpty()
                                && driver == null
                                && line.contains(LOADING_APPLICATION)) {
                            parseContextRoot(line);
                        } else if (payaraMicroURL != null
                                && payaraMicroURL.isEmpty()
                                && driver == null
                                && line.contains(APP_DEPLOYED)) {
                            String appName = parseDeployedApp(line);
                            if (contextRoot == null) {
                                contextRoot = contextRoots.get(appName);
                            }
                            openApp();
                        } else if (payaraMicroURL != null
                                && !payaraMicroURL.isEmpty()
                                && driver != null
                                && line.contains(APP_DEPLOYED)) {
                            try {
                                driver.navigate().refresh();
                            } catch (Exception ex) {
                                getLog().debug("Error in refreshing with WebDriver", ex);
                            }
                        } else if (autoDeploy
                                && line.contains(INOTIFY_USER_LIMIT_REACHED_MESSAGE)) {
                            getLog().error(WATCH_SERVICE_ERROR_MESSAGE);
                        }
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

    private void openApp() {
        try {
            driver = WebDriverFactory.createWebDriver(browser, getLog());
            String url = PropertiesUtils.getProperty(payaraMicroURL, payaraMicroURL);
            if ((url == null || url.isEmpty()) && hostIp != null && hostPort != null) {
                url = "http://" + hostIp + ":" + hostPort;
                if (contextRoot != null) {
                    url = url + contextRoot;
                }
                payaraMicroURL = url;
            }
            driver.get(url);
        } catch (Exception ex) {
            getLog().error("Error in running WebDriver", ex);
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(payaraMicroURL));
                }
            } catch (IOException | URISyntaxException e) {
                getLog().error("Error in running Desktop browse", e);
            } finally {
                driver = null;
            }
        }
    }

    private void parseInstanceConfig(BufferedReader br, PrintStream printStream) throws IOException {
        String hostIpLine = br.readLine();
        String hostPortLine = br.readLine();
        printStream.println(LogUtils.highlight(hostIpLine));
        printStream.println(LogUtils.highlight(hostPortLine));
        // Extract IP address
        Pattern ipRegex = Pattern.compile(HOST_IP_PATTERN);
        Matcher ipMatcher = ipRegex.matcher(hostIpLine);
        if (ipMatcher.find()) {
            hostIp = ipMatcher.group(1);
        }

        // Extract port
        Pattern portRegex = Pattern.compile(HOST_PORT_PATTERN);
        Matcher portMatcher = portRegex.matcher(hostPortLine);
        if (portMatcher.find()) {
            hostPort = portMatcher.group(1);
        }
    }

    private void parseMicroUrl(BufferedReader br, PrintStream printStream) throws IOException {
        String line = br.readLine();
        if (line != null) {
            payaraMicroURL = line.trim();
            printStream.println(LogUtils.highlight(payaraMicroURL));
            if (!payaraMicroURL.isEmpty()) {
                openApp();
            }
        }
    }

    private void parseContextRoot(String line) {
        Pattern appLoadingPattern = Pattern.compile(LOADING_APPLICATION_PATTERN);
        Matcher appLoadingMatcher = appLoadingPattern.matcher(line);

        if (appLoadingMatcher.find()) {
            String applicationName = appLoadingMatcher.group(1);
            String appContextRoot = appLoadingMatcher.group(2);
            contextRoots.put(applicationName, appContextRoot);
        }
    }

    private String parseDeployedApp(String line) {
        Pattern deploymentPatternO = Pattern.compile(APP_DEPLOYED_PATTERN);
        Matcher deploymentMatcher = deploymentPatternO.matcher(line);

        if (deploymentMatcher.find()) {
            String applicationName = deploymentMatcher.group(1);
            return applicationName;
        }
        return null;
    }

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public MavenProject getProject() {
        return this.getEnvironment().getMavenProject();
    }
    
        @Override
    public List<String> getRebootOnChange() {
        return rebootOnChange;
    }

    @Override
    public boolean isLocal() {
        return exploded;
    }

}
