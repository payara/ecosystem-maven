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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Bundle mojo incorporates payara-micro with the produced artefact by following steps given as follows:
 * <ul>
 *  <li>Fetch payara-micro from repository and open it to a folder. The default version is <i>4.1.1.171</i>. Specific version can be provided with @{code payaraVersion} parameter</li>
 *  <li>Fetch user specified jars from repository</li>
 *  <li>Copy produced artefact into /MICRO-INF/deploy folder if its extension is <b>war</b></li>
 *  <li>Copy any existing @{code domain.xml}, @{code keystore.jks}, @{code login.conf } and @{code login.properties} file from resources folder into /MICRO-INF/domain folder</li>
 *  <li>Bundle aggregated content as artefactName-microbundle.jar under target folder</li>
 * </ul>
 *
 * @author mertcaliskan
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.INSTALL)
public class BundleMojo extends AbstractMojo {

    private static final String EXTRACTED_PAYARAMICRO_FOLDERNAME = "/extracted-payaramicro";
    private static final String MICROINF_DEPLOY_FOLDERNAME = "/MICRO-INF/deploy";
    private static final String MICROINF_LIB_FOLDERNAME = "/MICRO-INF/lib";
    private static final String MICROINF_DOMAIN_FOLDERNAME = "/MICRO-INF/domain";

    private static final String OUTPUT_FOLDER = "${project.build.directory}" + EXTRACTED_PAYARAMICRO_FOLDERNAME;

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * By default this mojo fetches payara-micro with version 4.1.1.171. It can be overridden with this parameter.
     */
    @Parameter(property = "payaraVersion", defaultValue = "4.1.1.171")
    private String payaraVersion;

    /**
     * User specified jars that will de copied into the MICRO-INF/lib directory
     */
    @Parameter
    private List<ArtifactItem> customJars;

    /**
     * If the extension of the produced artefact is <b>war</b>, it will be copied automatically to MICRO-INF/deploy folder.
     * This behaviour can be disabled by setting @{code autoDeployArtifact} to false.
     */
    @Parameter(property = "autoDeployArtifact", defaultValue =  "true")
    private Boolean autoDeployArtifact;

    /**
     * main method for <b>bundle</b> mojo
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        fetchMicroArtefact();

        if (customJars != null && !customJars.isEmpty()) {
            copyCustomJars();
        }

        copyCustomFiles();

        if (autoDeployArtifact && "war".equalsIgnoreCase(mavenProject.getPackaging())) {
            copyProjectArtefactUnderExplodedMicroArtefact();
        }

        bundleAggregatedContentAsJar();
    }

    private void fetchMicroArtefact() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("3.0.0")
                ),
                goal("unpack"),
                configuration(
                        element(name("artifactItems"),
                                element(name("artifactItem"),
                                        element("groupId", "fish.payara.extras"),
                                        element("artifactId", "payara-micro"),
                                        element("version", payaraVersion)
                                )
                        ),
                        element(name("outputDirectory"), OUTPUT_FOLDER)
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    private void copyCustomJars() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("3.0.0")
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"),
                                constructElementsForCustomJars()
                        ),
                        element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_LIB_FOLDERNAME)
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    private void copyCustomFiles() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-resources-plugin"),
                        version("3.0.2")
                ),
                goal("copy-resources"),
                configuration(
                        element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DOMAIN_FOLDERNAME),
                        element(name("resources"),
                                element(name("resource"),
                                        element(name("directory"),  "${project.build.resources[0].directory}"),
                                        element(name("includes"),
                                                element(name("include"), "domain.xml"),
                                                element(name("include"), "keystore.jks"),
                                                element(name("include"), "login.conf"),
                                                element(name("include"), "logging.properties")
                                        )
                                )
                        )
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    private Element[] constructElementsForCustomJars() {
        List<Element> elements = new ArrayList<Element>();
        for (ArtifactItem artifactItem : customJars) {
            Element element = element(name("artifactItem"),
                    element("groupId", artifactItem.getGroupId()),
                    element("artifactId", artifactItem.getArtifactId()),
                    element("version", artifactItem.getVersion())
            );
            elements.add(element);
        }
        return elements.toArray(new Element[elements.size()]);
    }

    private void copyProjectArtefactUnderExplodedMicroArtefact() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("3.0.0")
                ),
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
                        element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DEPLOY_FOLDERNAME)
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    private void bundleAggregatedContentAsJar() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version("3.0.2")
                ),
                goal("jar"),
                configuration(
                        element(name("classesDirectory"), OUTPUT_FOLDER),
                        element(name("classifier"), "microbundle"),
                        element(name("archive"),
                                element(name("manifestFile"), OUTPUT_FOLDER + "/META-INF/MANIFEST.MF")
                        )
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
}
