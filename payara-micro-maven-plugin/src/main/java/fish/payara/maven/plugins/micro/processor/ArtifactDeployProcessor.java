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

import java.io.File;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.logging.Log;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @author mertcaliskan
 */
public class ArtifactDeployProcessor extends BaseProcessor {

    private Boolean autoDeployArtifact;
    private String autoDeployContextRoot;
    private Boolean autoDeployEmptyContextRoot;
    private String packaging;
    
    private Log log;

    public ArtifactDeployProcessor(Log log) {
        this.log = log;
    }

    @Override
    public void handle(MojoExecutor.ExecutionEnvironment environment) throws MojoExecutionException {

        Build build = environment.getMavenProject().getBuild();
        String finalName = build.getFinalName(); // this is never null, maven provides a default 
                                                 // if finalName not specified in pom.xml

        if (autoDeployArtifact && WAR_EXTENSION.equalsIgnoreCase(packaging)) {

            String contextRoot = autoDeployContextRoot;
            boolean contextRootSet = (contextRoot != null);
            boolean contextRootSetButEmpty = contextRootSet && contextRoot.isEmpty();
            if (!contextRootSet || contextRootSetButEmpty) {
                if (autoDeployEmptyContextRoot 
                        || contextRootSetButEmpty
                        || finalName.isEmpty()) {
                    contextRoot = "ROOT";
                } else {
                    contextRoot = finalName;
                }
            }

            String projectArtifactName = contextRoot + "." + WAR_EXTENSION;

            List<Element> elements = new ArrayList<>();

            elements.add(element("groupId", "${project.groupId}"));
            elements.add(element("artifactId", "${project.artifactId}"));
            elements.add(element("version", "${project.version}"));
            elements.add(element("type", "${project.packaging}"));
            elements.add(element("destFileName", projectArtifactName));

            executeMojo(dependencyPlugin,
                    goal("copy"),
                    configuration(
                            element(name("artifactItems"),
                                    element(name("artifactItem"),
                                            elements.toArray(new Element[0])
                                    )
                            ),
                            element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DEPLOY_FOLDER)
                    ),
                    environment
            );
            // Payara Micro deploys based on last modified date, so make sure that the artifact file is deployed last
            if (projectArtifactName != null) {
                String copiedFileName = OUTPUT_FOLDER + MICROINF_DEPLOY_FOLDER + File.separator + projectArtifactName;
                copiedFileName = copiedFileName.replace("${project.build.directory}", environment.getMavenProject().getBuild().getDirectory());
                File copiedFile = new File(copiedFileName);
                if (copiedFile.exists()) {
                    copiedFile.setLastModified(System.currentTimeMillis());
                    log.info("Updated timestamp of deployment file [" + copiedFile.getAbsolutePath() + "]");
                } else {
                    log.warn("Deployment file [" + copiedFile.getAbsolutePath() + "] doesn't exist, won't update its timestamp");
                }
            }
        }

        gotoNext(environment);
    }

    public BaseProcessor set(Boolean autoDeployArtifact, String autoDeployContextRoot,
            Boolean autoDeployEmptyContextRoot, String packaging) {
        this.autoDeployArtifact = autoDeployArtifact;
        this.autoDeployContextRoot = autoDeployContextRoot;
        this.autoDeployEmptyContextRoot = autoDeployEmptyContextRoot;
        this.packaging = packaging;
        return this;
    }
}
