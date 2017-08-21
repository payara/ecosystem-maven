/*
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static fish.payara.maven.plugins.micro.Configuration.PAYARA_MICRO_THREAD_NAME;
import static fish.payara.maven.plugins.micro.Configuration.WAR_EXTENSION;

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

    @Parameter(property = "payaraMicroAbsolutePath")
    private String payaraMicroAbsolutePath;

    @Parameter(property = "deamon", defaultValue = "false")
    private Boolean daemon;

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

    private Process microProcess;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final String path = decideOnWhichMicroToUse();

        ThreadGroup threadGroup = new ThreadGroup(PAYARA_MICRO_THREAD_NAME);
        Thread microProcessorThread = new Thread(threadGroup, new Runnable() {
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
                actualArgs.add(indice++, "-jar");
                actualArgs.add(indice++, path);
                if (deployWar && WAR_EXTENSION.equalsIgnoreCase(mavenProject.getPackaging())) {
                    actualArgs.add(indice++, "--deploy");
                    actualArgs.add(indice++, evaluateProjectArtifactAbsolutePath(false));
                }
                for (Option option : commandLineOptions) {
                    actualArgs.add(indice++, option.getKey());
                    actualArgs.add(indice++, option.getValue());
                }

                try {
                    final Runtime re = Runtime.getRuntime();
                    microProcess = re.exec(actualArgs.toArray(new String[actualArgs.size()]), systemProps.isEmpty()
                            ? null : systemProps.toArray(new String[systemProps.size()]));
                    redirectStream(microProcess.getInputStream(), System.out);
                    redirectStream(microProcess.getErrorStream(), System.err);

                    int exitCode = microProcess.waitFor();

                    if (exitCode != 0) {
                        throw new MojoFailureException(ERROR_MESSAGE);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(ERROR_MESSAGE, e);
                }
                finally {
                    closeMicroProcess();
                }
            }
        });
        if (daemon) {
            microProcessorThread.setDaemon(true);
            microProcessorThread.start();
        }
        else {
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

        if (artifactItem.getGroupId() != null) {
            DefaultArtifact artifact = new DefaultArtifact(artifactItem.getGroupId(),
                    artifactItem.getArtifactId(),
                    artifactItem.getVersion(),
                    null,
                    artifactItem.getType(),
                    artifactItem.getClassifier(),
                    new DefaultArtifactHandler("jar"));
            Artifact payaraMicroArtifact = mavenSession.getLocalRepository().find(artifact);
            return payaraMicroArtifact.getFile().getAbsolutePath();
        }

        throw new MojoExecutionException("Could not determine Payara Micro path. Please set it by defining either \"useUberJar\", \"payaraMicroAbsolutePath\" or \"artifactItem\" configuration options.");
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

    private void redirectStream(final InputStream inputStream, final OutputStream outputStream) {
        Thread thread = new Thread(new Runnable() {
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
