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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

/**
 * Stop mojo that terminates running payara-micro invoked by @{code run} mojo
 *
 * @author mertcaliskan
 */
@Mojo(name = "stop")
public class StopMojo extends BasePayaraMojo {

    private static final String ERROR_MESSAGE = "Error occurred while terminating payara-micro";

    @Parameter(property = "artifactItem")
    private ArtifactItem artifactItem;

    @Parameter(property = "processId")
    private String processId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (processId != null) {
            killProcess(processId);
        }
        String executorName;
        if (artifactItem.getGroupId() != null) {
            executorName = artifactItem.getArtifactId() + "-" + artifactItem.getVersion() + ".jar";
        }
        else {
            executorName = evaluateExecutorName(true);
        }

        final Runtime re = Runtime.getRuntime();
        try {
            Process jpsProcess = re.exec("jps");
            InputStream inputStream = jpsProcess.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String processId = null;
            while((line = in.readLine()) != null) {
                if (line.contains(executorName)) {
                    String[] split = line.split(" ");
                    processId = split[0];
                }
            }
            if (StringUtils.isNotEmpty(processId)) {
                killProcess(processId);
            }
            else {
                getLog().warn("Could not find process of running payara-micro?");
            }
        }
        catch (IOException e) {
            getLog().error(ERROR_MESSAGE, e);
        }
    }

    private void killProcess(String processId) {
        try {
            final Runtime re = Runtime.getRuntime();
            Process killProcess = re.exec("kill -2 " + processId);
            int result = killProcess.waitFor();
            if (result != 0) {
                getLog().error(ERROR_MESSAGE);
            }
        }
        catch (IOException |InterruptedException e) {
            getLog().error(ERROR_MESSAGE, e);
        }
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
}