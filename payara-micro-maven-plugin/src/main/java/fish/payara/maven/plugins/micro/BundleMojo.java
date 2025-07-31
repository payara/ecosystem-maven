/*
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bundle mojo incorporates payara-micro with the produced artifact by following steps given as follows:
 * <ul>
 *  <li>Fetch payara-micro from repository and open it to a folder. The default version is <i>6.2024.3</i>. Specific
 *  version can be provided with @{code payaraMicroVersion} parameter</li>
 *  <li>Fetch user specified jars from repository</li>
 *  <li>Copy any existing {@code domain.xml}, {@code keystore.jks}, {@code login.conf } and {@code login.properties} files from resources folder into /MICRO-INF/domain folder</li>
 *  <li>Copy any existing {@code pre-boot-commands.txt}, {@code post-boot-commands.txt} and {@code post-deploy-commands.txt} files from resources folder into /MICRO-INF folder</li>
 *  <li>Copy produced artifact into /MICRO-INF/deploy folder if its extension is <b>war</b></li>
 *  <li>Copy user specified artifacts into /MICRO-INF/deploy folder</li>
 *  <li>Replace {@code Start-Class} entry in the manifest file with a custom bootstrap class if it's provided by user</li>
 *  <li>Append system properties to @{code MICRO-INF/payara-boot.properties}</li>
 *  <li>Bundle aggregated content as artifactName-microbundle.jar under target folder</li>
 * </ul>
 * 
 * If {@code autoDeployArtifact} is {@code true}, then the context root of the application is constructed as:
 * <ol>
 *  <li>If {@code autoDeployContextRoot} is specified then it's the context root
 *  <li>If {@code autoDeployEmptyContextRoot} is {@code true} (default value) then the context root 
 *     is empty. In other words, the application path is just '/'</li>
 *  <li>If project's {@code finalName} is specified then the context root is equal to its value</li>
 *  <li>If nothing is specified then the context root is derived from the artifact metadata</li>
 * </ol>
 *
 * If context root is 'ROOT' then the application path is just '/' (same as if {@code autoDeployEmptyContextRoot} is set to {@code true}).
 * 
 * @author mertcaliskan
 */
@Mojo(name = "bundle", defaultPhase = LifecyclePhase.INSTALL)
public class BundleMojo extends BasePayaraMojo {

    @Parameter(property = "payara.micro.version", defaultValue = "${env.PAYARA_MICRO_VERSION}")
    private String payaraMicroVersion;

    /**
     * By default this mojo fetches payara-micro
     */
    @Deprecated
    @Parameter(property = "payaraVersion")
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
     * Replaces the @{code Start-Class} definition that resides in MANIFEST.MF file with the provided class.
     */
    @Parameter(property = "payara.start.class", defaultValue = "${env.PAYARA_START_CLASS}")
    private String startClass;

    /**
     * If the extension of the produced artifact is <b>war</b>, it will be copied automatically to MICRO-INF/deploy folder.
     * This behaviour can be disabled by setting @{code autoDeployArtifact} to false.
     */
    @Parameter(property = "payara.auto.deploy.artifact", defaultValue =  "true")
    private boolean autoDeployArtifact;
    
    /**
     * Sets context root of the deployed artifact to / which means there's no context root. Default true. 
     * If set to false, context root is derived from artifact name or finalName. Value of autoDeployContextRoot overrides this.
     */
    @Parameter(property = "payara.auto.deploy.empty.context.root", defaultValue = "true")
    private boolean autoDeployEmptyContextRoot;

    /**
     * Sets context root of the deployed artifact if autoDeployArtifact is true
     */
    @Parameter(property = "payara.auto.deploy.context.root", defaultValue = "")
    private String autoDeployContextRoot;

    /**
     * Appends all system properties defined into the @{code payara-boot.properties} file
     */
    @Parameter(property = "payara.append.system.properties", defaultValue = "true")
    private boolean appendSystemProperties;

    /**
     * Specify default command line options for bundled JAR
     */
    @Parameter
    private List<Property> systemProperties;
    
    public BundleMojo() {
        if (System.getProperty("startClass") != null) {
            startClass = System.getProperty("startClass");
        }

        String autoDeployArtifactEnv = System.getenv("PAYARA_AUTO_DEPLOY_ARTIFACT");
        if (System.getProperty("autoDeployArtifact") != null) {
            autoDeployArtifact = Boolean.parseBoolean(System.getProperty("autoDeployArtifact"));
        } else if (autoDeployArtifactEnv != null && !autoDeployArtifactEnv.isEmpty()) {
            try {
                autoDeployArtifact = Boolean.parseBoolean(autoDeployArtifactEnv);
            } catch (NumberFormatException e) {
                getLog().warn("Invalid PAYARA_AUTO_DEPLOY_ARTIFACT value. Falling back to default: true", e);
                autoDeployArtifact = true;
            }
        }
        
        String autoDeployEmptyContextRootEnv = System.getenv("PAYARA_AUTO_DEPLOY_EMPTY_CONTEXT_ROOT");
        if (System.getProperty("autoDeployEmptyContextRoot") != null) {
            autoDeployEmptyContextRoot = Boolean.parseBoolean(System.getProperty("autoDeployEmptyContextRoot"));
        } else if (autoDeployEmptyContextRootEnv != null && !autoDeployEmptyContextRootEnv.isEmpty()) {
            try {
                autoDeployEmptyContextRoot = Boolean.parseBoolean(autoDeployEmptyContextRootEnv);
            } catch (NumberFormatException e) {
                getLog().warn("Invalid PAYARA_AUTO_DEPLOY_EMPTY_CONTEXT_ROOT value. Falling back to default: true", e);
                autoDeployEmptyContextRoot = true;
            }
        }
        
        String autoDeployContextRootEnv = System.getenv("PAYARA_AUTO_DEPLOY_CONTEXT_ROOT");
        if (System.getProperty("autoDeployContextRoot") != null) {
            autoDeployContextRoot = System.getProperty("autoDeployContextRoot");
        }else if (autoDeployContextRootEnv != null && !autoDeployContextRootEnv.isEmpty()) {
            try {
                autoDeployContextRoot = autoDeployContextRootEnv;
            } catch (NumberFormatException e) {
                getLog().warn("Invalid PAYARA_AUTO_DEPLOY_CONTEXT_ROOT value. Falling back to empty value.", e);
                autoDeployContextRoot = "";
            }
        }
        
        String appendSystemPropertiesEnv = System.getenv("PAYARA_APPEND_SYSTEM_PROPERTIES");
        if (System.getProperty("appendSystemProperties") != null) {
            appendSystemProperties = Boolean.parseBoolean(System.getProperty("appendSystemProperties"));
        } else if (appendSystemPropertiesEnv != null && !appendSystemPropertiesEnv.isEmpty()) {
            try {
                appendSystemProperties = Boolean.parseBoolean(appendSystemPropertiesEnv);
            } catch (NumberFormatException e) {
                getLog().warn("Invalid PAYARA_APPEND_SYSTEM_PROPERTIES value. Falling back to default: true", e);
                appendSystemProperties = true;
            }
        }
        
        
        if (payaraVersion != null) {
            getLog().warn("Parameter 'payaraVersion' is deprecated and has been replaced by 'payaraMicroVersion' to stay in sync with the Payara Server Maven Plugin.");
            payaraMicroVersion = payaraVersion;
        }
        
        if (payaraMicroVersion == null && mavenProject != null) {
            PayaraMicroVersionSelector payaraMicroVersionSelector = new PayaraMicroVersionSelector(mavenProject, getLog());
            try {
                payaraMicroVersion = payaraMicroVersionSelector.fetchPayaraVersion();
            } catch (MojoExecutionException ex) {
                Logger.getLogger(BundleMojo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Bundle mojo execution is skipped");
            return;
        }
        MojoExecutor.ExecutionEnvironment environment = getEnvironment();
        BaseProcessor processor = constructProcessorChain();
        processor.handle(environment);
    }

    private BaseProcessor constructProcessorChain() {
        MicroUnpackProcessor microUnpackProcessor = new MicroUnpackProcessor();

        microUnpackProcessor.set(payaraMicroVersion)
            .next(new CustomJarCopyProcessor()).set(customJars)
            .next(new CustomFileCopyProcessor())
            .next(new BootCommandFileCopyProcessor())
            .next(new DefinedArtifactDeployProcessor()).set(deployArtifacts)
            .next(new ArtifactDeployProcessor(getLog())).set(
                autoDeployArtifact,
                autoDeployContextRoot,
                autoDeployEmptyContextRoot,
                mavenProject.getPackaging())
            .next(new StartClassCopyReplaceProcessor()).set(startClass)
            .next(new SystemPropAppendProcessor()).set(appendSystemProperties).set(mavenProject)
            .next(new CustomPropPrependProcessor()).set(systemProperties).set(mavenProject)
            .next(new MicroJarBundleProcessor(uberJarClassifier));

        return microUnpackProcessor;
    }
}