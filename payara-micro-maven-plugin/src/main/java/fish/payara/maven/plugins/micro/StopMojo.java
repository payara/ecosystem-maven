/*
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.toolchain.Toolchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static fish.payara.maven.plugins.micro.Configuration.JAR_EXTENSION;
import java.util.function.Predicate;

/**
 * Stop mojo that terminates running payara-micro invoked by @{code run} mojo
 *
 * @author mertcaliskan
 */
@Mojo(name = "stop")
public class StopMojo extends BasePayaraMojo {

    private static final String ERROR_MESSAGE = "Error occurred while terminating payara-micro";

    @Parameter
    private ArtifactItem artifactItem;

    @Parameter(property = "payara.process.id", defaultValue = "${env.PAYARA_PROCESS_ID}")
    private String processId;

    @Parameter(property = "payara.use.uber.jar", defaultValue = "${env.PAYARA_USE_UBER_JAR}")
    private boolean useUberJar;

    private Toolchain toolchain;

    @Parameter(property = "payara.max.stop.timeout.millis", defaultValue = "5000")
    private int maxStopTimeoutMillis;
    
    public StopMojo() {
        if (System.getProperty("processId") != null) {
            processId = System.getProperty("processId");
        }
        if (System.getProperty("useUberJar") != null) {
            useUberJar = Boolean.parseBoolean(System.getProperty("useUberJar"));
        }
        String envTimeout = System.getenv("PAYARA_MAX_STOP_TIMEOUT_MILLIS");
        if (System.getProperty("maxStopTimeoutMillis") != null) {
            maxStopTimeoutMillis = Integer.parseInt(System.getProperty("maxStopTimeoutMillis"));
        } else if (envTimeout != null && !envTimeout.isEmpty()) {
            try {
                maxStopTimeoutMillis = Integer.parseInt(envTimeout);
            } catch (NumberFormatException e) {
                getLog().warn("Invalid PAYARA_MAX_STOP_TIMEOUT_MILLIS value. Falling back to default: 5000", e);
                maxStopTimeoutMillis = 5000;
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Stop mojo execution is skipped");
            return;
        }

        toolchain = getToolchain();
        final Runtime re = Runtime.getRuntime();

        if (processId != null) {
            killProcess(processId);
            try {
                waitForProcessToStop(processId, re);
            } catch (IOException e) {
                getLog().error(ERROR_MESSAGE, e);
            }
        }

        String executorName;
        if (artifactItem != null 
                && artifactItem.getGroupId() != null
                && artifactItem.getArtifactId() != null) {
            executorName = artifactItem.getArtifactId();
        } else if (useUberJar) {
            executorName = evaluateExecutorName(true);
        } else {
            executorName = "-Dgav=" + getProjectGAV();
        }

        try {
            String pid = getProcessIdToKill(executorName, re);
            if (StringUtils.isNotEmpty(pid)) {
                killProcess(pid);
                waitForProcessToStop(pid, re);
            } else {
                getLog().warn("Could not find process of running payara-micro?");
            }
        } catch (IOException e) {
            getLog().error(ERROR_MESSAGE, e);
        }
    }

    private String getProcessIdToKill(String executorName, Runtime re) throws IOException {
        String lineWithPid = getLineFromJpsOutput(re, line -> line.contains(executorName));
        if (lineWithPid != null) {
            String[] split = lineWithPid.split(" ");
            String pid = split[0];
            return pid;
        }
        return null;
    }

    private boolean isProcessRunning(String pid, final Runtime re) throws IOException {
        String lineWithPid = getLineFromJpsOutput(re, line -> {
            String[] split = line.split(" ");
            return split[0].equals(pid);
        });
        return lineWithPid != null;
    }

    private String getLineFromJpsOutput(final Runtime re, Predicate<String> linePredicate) throws IOException {
        String jpsPath = "jps";
        if (toolchain != null) {
            jpsPath = toolchain.findTool("jps");
        }
        Process jpsProcess = re.exec(jpsPath + " -v");
        InputStream inputStream = jpsProcess.getInputStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (linePredicate.test(line)) {
                    return line;
                }
            }
            return null;
        }
    }

    private void waitForProcessToStop(String processId, final Runtime re) throws RuntimeException, IOException {
        long startedWaitingAtMillis = System.currentTimeMillis();
        boolean processRunning;
        do {
            processRunning = isProcessRunning(processId, re);
            if (processRunning) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } while (processRunning && System.currentTimeMillis() < startedWaitingAtMillis + maxStopTimeoutMillis);
        if (processRunning) {
            getLog().warn("Could not stop previously started payara-micro with process ID " + processId + " or waiting too long, proceeding further");
        }
    }

    private void killProcess(String processId) throws MojoExecutionException {
        String command = null;
        try {
            final Runtime re = Runtime.getRuntime();
            if (isUnix()) {
                command = "kill " + processId;
            } else if (isWindows()) {
                command = "taskkill /PID " + processId + " /F";
            }
            if (command == null) {
                throw new MojoExecutionException("Operation system not supported!");
            }
            Process killProcess = re.exec(command);
            int result = killProcess.waitFor();
            if (result != 0) {
                getLog().error(ERROR_MESSAGE);
            }
        } catch (IOException | InterruptedException e) {
            getLog().error(ERROR_MESSAGE, e);
        }
    }

    private String evaluateExecutorName(Boolean withExtension) {
        String extension;
        if (withExtension) {
            extension = "-" + uberJarClassifier + "." + JAR_EXTENSION;
        } else {
            extension = "." + mavenProject.getPackaging();
        }
        if (StringUtils.isNotEmpty(mavenProject.getBuild().getFinalName())) {
            return mavenProject.getBuild().getFinalName() + extension;
        }
        return mavenProject.getArtifact().getArtifactId() + mavenProject.getVersion() + extension;
    }

    private boolean isUnix() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Linux")
                || osName.startsWith("FreeBSD")
                || osName.startsWith("OpenBSD")
                || osName.startsWith("gnu")
                || osName.startsWith("gnu/kfreebsd")
                || osName.startsWith("netbsd")
                || osName.startsWith("Mac OS");
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Windows CE")
                || osName.startsWith("Windows");
    }
}
