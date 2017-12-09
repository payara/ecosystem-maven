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
package fish.payara.maven.plugins.micro.processor;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @author mertcaliskan
 */
public class ArtifactDeployProcessor extends BaseProcessor {

    private Boolean autoDeployArtifact;
    private String packaging;
    private String finalName;

    @Override
    public void handle(MojoExecutor.ExecutionEnvironment environment) throws MojoExecutionException {
        if (autoDeployArtifact && WAR_EXTENSION.equalsIgnoreCase(packaging)) {
            if (StringUtils.isNotEmpty(finalName)) {
                executeMojo(resourcesPlugin,
                        goal("copy-resources"),
                        configuration(
                                element(name("resources"),
                                        element(name("resource"),
                                                element("directory", "${project.build.directory}"),
                                                element(name("includes"),
                                                        element("include", finalName + "." + packaging)
                                                )
                                        )
                                ),
                                element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DEPLOY_FOLDER)
                        ),
                        environment
                );
            }
            else {
                executeMojo(dependencyPlugin,
                        goal("copy"),
                        configuration(
                                element(name("artifactItems"),
                                        element(name("artifactItem"),
                                                element("groupId", "${project.groupId}"),
                                                element("artifactId", "${project.artifactId}"),
                                                element("version", "${project.version}"),
                                                element("type", "${project.packaging}")
                                        )
                                ),
                                element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DEPLOY_FOLDER)
                        ),
                        environment
                );
            }
        }

        gotoNext(environment);
    }

    public BaseProcessor set(Boolean autoDeployArtifact, String packaging, String finalName) {
        this.autoDeployArtifact = autoDeployArtifact;
        this.packaging = packaging;
        this.finalName = finalName;
        return this;
    }
}
