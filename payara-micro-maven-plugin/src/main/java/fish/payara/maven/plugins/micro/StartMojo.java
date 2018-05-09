/*
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fish.payara.maven.plugins.micro.Configuration.*;

/**
 * Run mojo that executes payara-micro
 *
 * @author mertcaliskan
 */
@Mojo(name = "start")
public class StartMojo extends BasePayaraMojo {

    String ERROR_MESSAGE = "Errors occurred while executing payara-micro.";

    @Parameter(property = "javaPath", defaultValue = "java")
    private String javaPath;

    @Parameter(property = "payaraVersion", defaultValue = "4.1.2.181")
    private String payaraVersion;

    @Parameter(property = "payaraMicroAbsolutePath")
    private String payaraMicroAbsolutePath;

    @Parameter(property = "daemon", defaultValue = "false")
    private Boolean daemon;

    @Parameter(property = "immediateExit", defaultValue = "false")
    private Boolean immediateExit;

    @Parameter(property = "artifactItem")
    private ArtifactItem artifactItem;

    @Parameter(property = "useUberJar", defaultValue = "false")
    private Boolean useUberJar;

    @Parameter(property = "deployWar", defaultValue = "false")
    private Boolean deployWar;

    @Parameter(property = "copySystemProperties", defaultValue = "false")
    private Boolean copySystemProperties;

    @Parameter(property = "commandLineOptions")
    private List<Option> commandLineOptions;

    @Parameter(property = "javaCommandLineOptions")
    private List<Option> javaCommandLineOptions;

    private Process microProcess;
    private Thread microProcessorThread;
    private ThreadGroup threadGroup;

    StartMojo() {
        threadGroup = new ThreadGroup(MICRO_THREAD_NAME);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Start mojo execution is skipped");
            return;
        }

        final String path = decideOnWhichMicroToUse();

        microProcessorThread = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                List<String> systemProps = new ArrayList<>();
                if (copySystemProperties) {
                    for (String property : mavenSession.getSystemProperties().stringPropertyNames()) {
                        String value = System.getProperty(property);
                        if (value != null) {
                            String prop = String.format("%s=%s", property, value);
                            systemProps.add(prop);
                        }
                    }
                }

                final List<String> actualArgs = new ArrayList<>();
                getLog().info("Starting payara-micro from path: " + path);
                int indice = 0;
                actualArgs.add(indice++, javaPath);
                if (javaCommandLineOptions != null) {
                    for (Option option : javaCommandLineOptions) {
                        if (option.getKey() != null && option.getValue() != null) {
                            String systemProperty = String.format("%s=%s", option.getKey(), option.getValue());
                            actualArgs.add(indice++, systemProperty);
                        }
                        else if (option.getValue() != null) {
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
                actualArgs.add(indice++, "-jar");
                actualArgs.add(indice++, path);
                if (deployWar && WAR_EXTENSION.equalsIgnoreCase(mavenProject.getPackaging())) {
                    actualArgs.add(indice++, "--deploy");
                    actualArgs.add(indice++, evaluateProjectArtifactAbsolutePath(false));
                }
                if (commandLineOptions != null) {
                    for (Option option : commandLineOptions) {
                        if (option.getKey() != null) {
                            actualArgs.add(indice++, option.getKey());
                        }
                        if (option.getValue() != null) {
                            actualArgs.add(indice++, option.getValue());
                        }
                    }
                }

                try {
                    final Runtime re = Runtime.getRuntime();
                    microProcess = re.exec(actualArgs.toArray(new String[actualArgs.size()]), systemProps.isEmpty()
                            ? null : systemProps.toArray(new String[systemProps.size()]));

                    if (daemon) {
                        redirectStream(microProcess.getInputStream(), System.out);
                        redirectStream(microProcess.getErrorStream(), System.err);
                    } else {
                        redirectStreamToGivenOutputStream(microProcess.getInputStream(), System.out);
                        redirectStreamToGivenOutputStream(microProcess.getErrorStream(), System.err);
                    }

                    int exitCode = microProcess.waitFor();
                    if (exitCode != 0) {
                        throw new MojoFailureException(ERROR_MESSAGE);
                    }
                }
                catch (InterruptedException ignored) {
                }
                catch (Exception e) {
                    throw new RuntimeException(ERROR_MESSAGE, e);
                }
                finally {
                    if (!daemon) {
                        closeMicroProcess();
                    }
                }
            }
        });

        final Thread shutdownHook = new Thread(threadGroup,new Runnable() {
            @Override
            public void run() {
                if (microProcess != null && microProcess.isAlive()) {
                    try {
                        microProcess.destroy();
                        microProcess.waitFor(1, TimeUnit.MINUTES);
                    } catch (InterruptedException ignored) {
                    } finally {
                        microProcess.destroyForcibly();
                    }
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
        }
        else {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            microProcessorThread.run();
        }
    }

    private String decideOnWhichMicroToUse() throws MojoExecutionException {
        if (useUberJar) {
            String path = evaluateProjectArtifactAbsolutePath(true);

            if (!Files.exists(Paths.get(path))) {
                throw new MojoExecutionException("\"useUberJar\" option was set to \"true\" but detected path " + path + " does not exist. You need to execute the \"bundle\" goal before using this option.");
            }

            return path;
        }

        if (payaraMicroAbsolutePath != null) {
            return payaraMicroAbsolutePath;
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

        throw new MojoExecutionException("Could not determine Payara Micro path. Please set it by defining either \"useUberJar\", \"payaraMicroAbsolutePath\" or \"artifactItem\" configuration options.");
    }

    private String findLocalPathOfArtifact(DefaultArtifact artifact) {
        Artifact payaraMicroArtifact = mavenSession.getLocalRepository().find(artifact);
        return payaraMicroArtifact.getFile().getAbsolutePath();
    }

    private String evaluateProjectArtifactAbsolutePath(Boolean withExtension) {
        String projectJarAbsolutePath = mavenProject.getBuild().getDirectory() + "/";
        projectJarAbsolutePath += evaluateExecutorName(withExtension);
        return projectJarAbsolutePath;
    }

    private String evaluateExecutorName(Boolean withExtension) {
        String extension;
        if (withExtension) {
            extension = "-" + Configuration.MICROBUNDLE_EXTENSION + ".jar";
        }
        else {
            extension = "." + mavenProject.getPackaging();
        }
        if (StringUtils.isNotEmpty(mavenProject.getBuild().getFinalName())) {
            return mavenProject.getBuild().getFinalName() + extension;
        }
        return mavenProject.getArtifact().getArtifactId() + mavenProject.getVersion() + extension;
    }

    private void closeMicroProcess() {
        if (microProcess != null) {
            try {
                microProcess.exitValue();
            } catch (IllegalThreadStateException e) {
                microProcess.destroy();
                getLog().info("Terminated payara-micro.");
            }
        }
    }

    private void redirectStream(final InputStream inputStream, final PrintStream printStream) {
        final Thread thread = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
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
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    private void redirectStreamToGivenOutputStream(final InputStream inputStream, final OutputStream outputStream) {
        Thread thread = new Thread(threadGroup, new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.copy(inputStream, outputStream);
                } catch (IOException e) {
                    getLog().error("Error occurred while reading stream", e);
                }
            }
        });
        thread.setDaemon(false);
        thread.start();
    }
}
