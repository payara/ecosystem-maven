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

import fish.payara.maven.plugins.micro.processor.MicroFetchProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.openqa.selenium.WebDriver;

/**
 * Run mojo that executes payara-micro
 *
 * @author mertcaliskan, Gaurav Gupta
 */
@Mojo(name = "start")
public class StartMojo extends BasePayaraMojo {

    private static final String ERROR_MESSAGE = "Errors occurred while executing payara-micro.";
    private static final String PRE_BOOT = "--prebootcommandfile";
    private static final String POST_BOOT = "--postbootcommandfile";
    private static final String POST_DEPLOY = "--postdeploycommandfile";

    @Parameter(property = "javaPath")
    private String javaPath;

    @Parameter(property = "payaraVersion", defaultValue = "6.2023.1")
    private String payaraVersion;

    @Parameter(property = "payaraMicroAbsolutePath")
    private String payaraMicroAbsolutePath;

    @Parameter(property = "daemon", defaultValue = "false")
    private boolean daemon;

    @Parameter(property = "immediateExit", defaultValue = "false")
    private boolean immediateExit;

    @Parameter(property = "artifactItem")
    private ArtifactItem artifactItem;

    @Parameter(property = "useUberJar", defaultValue = "false")
    private boolean useUberJar;

    @Parameter(property = "deployWar", defaultValue = "false")
    protected boolean deployWar;

    /**
     * Use exploded artifact for deployment.
     */
    @Parameter(property = "exploded", defaultValue = "false")
    protected boolean exploded;

    @Parameter(property = "autoDeploy", defaultValue = "false")
    protected boolean autoDeploy;

    @Parameter(property = "liveReload", defaultValue = "false")
    protected boolean liveReload;

    @Parameter(property = "browser")
    protected String browser;

    @Parameter(property = "trimLog", defaultValue = "false")
    protected boolean trimLog;

    /**
     * Attach a debugger. If set to "true", the process will suspend and wait
     * for a debugger to attach on port 5005. If set to other value, will be
     * appended to the argLine, allowing you to configure custom debug options.
     *
     */
    @Parameter(property = "debug", defaultValue = "false")
    protected String debug;

    @Parameter(property = "contextRoot")
    private String contextRoot;

    @Parameter(property = "hotDeploy")
    protected boolean hotDeploy;

    /**
     * The directory where the webapp is built, default value is exploded war.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
    private File webappDirectory;

    /**
     * Property passed by Apache NetBeans IDE to set contextRoot of the
     * application
     */
    @Parameter(property = "netbeans.deploy.clientUrlPart")
    private String clientUrlPart;

    @Deprecated
    @Parameter(property = "copySystemProperties", defaultValue = "false")
    private boolean copySystemProperties;

    @Parameter(property = "commandLineOptions")
    private List<Option> commandLineOptions;

    @Parameter(property = "javaCommandLineOptions")
    private List<Option> javaCommandLineOptions;

    @Parameter(property = "classpathArtifactItems")
    private List<ArtifactItem> classpathArtifactItems;

    private Process microProcess;
    private Thread microProcessorThread;
    private final ThreadGroup threadGroup;
    private Toolchain toolchain;

    private AutoDeployHandler autoDeployHandler;
    private final List<String> rebootOnChange = new ArrayList<>();
    private WebDriver driver;
    private String payaraMicroURL;

    StartMojo() {
        threadGroup = new ThreadGroup(MICRO_THREAD_NAME);
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (autoDeploy && autoDeployHandler == null) {
            autoDeployHandler = new AutoDeployHandler(this, webappDirectory);
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
                getLog().debug("Starting Payara Micro with the these arguments: " + actualArgs);
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
                } catch (Exception ex) {
                        getLog().debug(ex);
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

        if (StringUtils.isNotEmpty(javaPath)) {
            javaToUse = javaPath;
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

        if (payaraMicroAbsolutePath != null) {
            return payaraMicroAbsolutePath;
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

        if (payaraVersion != null) {
            MojoExecutor.ExecutionEnvironment environment = getEnvironment();
            MicroFetchProcessor microFetchProcessor = new MicroFetchProcessor();
            microFetchProcessor.set(payaraVersion).handle(environment);

            DefaultArtifact artifact = new DefaultArtifact(MICRO_GROUPID, MICRO_ARTIFACTID,
                    payaraVersion,
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

    private String evaluateProjectArtifactAbsolutePath(String extension) {
        String projectJarAbsolutePath = mavenProject.getBuild().getDirectory() + File.separator;
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

    List<String> getRebootOnChange() {
        return rebootOnChange;
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
                        printStream.println(line);
                        printStream.println(trimLog? LogUtils.trimLog(line):line);
                        if (line.contains(PAYARA_MICRO_URLS)) {
                            line = br.readLine();
                            if (line != null) {
                                payaraMicroURL = line.trim();
                                printStream.println(LogUtils.highlightURL(payaraMicroURL));
                                if (!payaraMicroURL.isEmpty()) {
                                    try {
                                        driver = WebDriverFactory.createWebDriver(browser);
                                        driver.get(PropertiesUtils.getProperty(payaraMicroURL, payaraMicroURL));
                                    } catch (Exception ex) {
                                        getLog().error("Error in running WebDriver:" + ex.getMessage());
                                        try {
                                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                                Desktop.getDesktop().browse(new URI(payaraMicroURL));
                                            }
                                        } catch (IOException | URISyntaxException e) {
                                            getLog().error("Error in running Desktop browse:" + e.getMessage());
                                        } finally {
                                            driver = null;
                                        }
                                    }
                                }
                            }
                        } else if (payaraMicroURL != null
                                && !payaraMicroURL.isEmpty()
                                && driver != null
                                && line.contains(APP_DEPLOYED)) {
                            try {
                                driver.navigate().refresh();
                            } catch (Exception ex) {
                                getLog().error("Error in refreshing with WebDriver:" + ex.getMessage());
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

    public WebDriver getDriver() {
        return driver;
    }

}
