/*
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

/**
 * Stop mojo that terminates a running Payara Server process started by the {@code start} or {@code dev} goal.
 */
@Mojo(name = "stop")
public class StopMojo extends BasePayaraMojo {

    private static final String ERROR_MESSAGE = "Error occurred while terminating Payara Server";

    @Parameter(property = "payara.process.id", defaultValue = "${env.PAYARA_PROCESS_ID}")
    private String processId;

    public StopMojo() {
        if (System.getProperty("processId") != null) {
            processId = System.getProperty("processId");
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Stop mojo execution is skipped");
            return;
        }

        if (processId == null || processId.isBlank()) {
            getLog().warn("No processId provided. Set -DprocessId=<pid> or PAYARA_PROCESS_ID environment variable.");
            return;
        }

        killProcess(processId);
    }

    private void killProcess(String processId) throws MojoExecutionException {
        String command;
        try {
            String osName = System.getProperty("os.name", "");
            if (osName.startsWith("Windows")) {
                command = "taskkill /PID " + processId + " /F /T";
            } else {
                command = "kill " + processId;
            }
            getLog().info("Stopping Payara Server process " + processId);
            Process killProcess = Runtime.getRuntime().exec(command);
            int result = killProcess.waitFor();
            if (result != 0) {
                getLog().error(ERROR_MESSAGE + " (exit code " + result + ")");
            }
        } catch (IOException | InterruptedException e) {
            getLog().error(ERROR_MESSAGE, e);
        }
    }
}
