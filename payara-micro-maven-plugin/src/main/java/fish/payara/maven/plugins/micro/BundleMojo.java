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

import fish.payara.maven.plugins.micro.processor.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.List;

/**
 * Bundle mojo incorporates payara-micro with the produced artifact by following steps given as follows:
 * <ul>
 *  <li>Fetch payara-micro from repository and open it to a folder. The default version is <i>4.1.1.171</i>. Specific version can be provided with @{code payaraVersion} parameter</li>
 *  <li>Fetch user specified jars from repository</li>
 *  <li>Copy any existing @{code domain.xml}, @{code keystore.jks}, @{code login.conf } and @{code login.properties} files from resources folder into /MICRO-INF/domain folder</li>
 *  <li>Copy any existing @{code pre-boot-commands.txt}, @{code post-boot-commands.txt} and @{code post-deploy-commands.txt} files from resources folder into /MICRO-INF folder</li>
 *  <li>Copy produced artifact into /MICRO-INF/deploy folder if its extension is <b>war</b></li>
 *  <li>Copy user specified artifacts into /MICRO-INF/deploy folder</li>
 *  <li>Replace {@code Start-Class} entry in the manifest file with a custom bootstrap class if it's provided by user</li>
 *  <li>Append system properties to @{code MICRO-INF/payara-boot.properties}</li>
 *  <li>Bundle aggregated content as artifactName-microbundle.jar under target folder</li>
 * </ul>
 *
 * @author mertcaliskan
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.INSTALL)
public class BundleMojo extends BasePayaraMojo {

    /**
     * By default this mojo fetches payara-micro with version 4.1.1.171. It can be overridden with this parameter.
     */
    @Parameter(property = "payaraVersion", defaultValue = "4.1.1.171")
    private String payaraVersion;

    /**
     * User specified jars that will be copied into the MICRO-INF/lib directory
     */
    @Parameter
    private List<ArtifactItem> customJars;

    /**
     * User specified artifacts that will be deployed by copying into the MICRO-INF/deploy directory
     */
    @Parameter
    private List<ArtifactItem> deployArtifacts;

    /**
     * If the extension of the produced artifact is <b>war</b>, it will be copied automatically to MICRO-INF/deploy folder.
     * This behaviour can be disabled by setting @{code autoDeployArtifact} to false.
     */
    @Parameter(property = "autoDeployArtifact", defaultValue =  "true")
    private Boolean autoDeployArtifact;

    /**
     * Replaces the @{code Start-Class} definition that resides in MANIFEST.MF file with the provided class.
     */
    @Parameter(property = "startClass")
    private String startClass;

    /**
     * Appends all system properties defined into the @{code payara-boot.properties} file
     */
    @Parameter(property = "appendSystemProperties", defaultValue = "true")
    private Boolean appendSystemProperties;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Bundle mojo execution is skipped");
            return;
        }

        MojoExecutor.ExecutionEnvironment environment = getEnvironment();
        BaseProcessor processor = constructProcessorChain();
        processor.handle(environment);
    }

    private BaseProcessor constructProcessorChain() throws MojoExecutionException {
        MicroFetchProcessor microFetchProcessor = new MicroFetchProcessor();
        CustomJarCopyProcessor customJarCopyProcessor = new CustomJarCopyProcessor();
        CustomFileCopyProcessor customFileCopyProcessor = new CustomFileCopyProcessor();
        BootCommandFileCopyProcessor bootCommandFileCopyProcessor = new BootCommandFileCopyProcessor();
        ArtifactDeployProcessor artifactDeployProcessor = new ArtifactDeployProcessor();
        DefinedArtifactDeployProcessor definedArtifactDeployProcessor = new DefinedArtifactDeployProcessor();
        StartClassReplaceProcessor startClassReplaceProcessor = new StartClassReplaceProcessor();
        SystemPropAppendProcessor systemPropAppendProcessor = new SystemPropAppendProcessor();
        MicroJarBundleProcessor microJarBundleProcessor = new MicroJarBundleProcessor();

        microFetchProcessor.set(payaraVersion).next(customJarCopyProcessor);
        customJarCopyProcessor.set(customJars).next(customFileCopyProcessor);
        customFileCopyProcessor.next(bootCommandFileCopyProcessor);
        bootCommandFileCopyProcessor.next(artifactDeployProcessor);
        artifactDeployProcessor.set(autoDeployArtifact, mavenProject.getPackaging(), mavenProject.getBuild().getFinalName()).next(definedArtifactDeployProcessor);
        definedArtifactDeployProcessor.set(deployArtifacts).next(startClassReplaceProcessor);
        startClassReplaceProcessor.set(startClass).next(systemPropAppendProcessor);
        systemPropAppendProcessor.set(appendSystemProperties).next(microJarBundleProcessor);

        return microFetchProcessor;
    }
}
