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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mertcaliskan
 */
public class CustomFileCopyProcessor extends BaseProcessor {

    /**
     * User specified configuration files that will be copied into the
     * MICRO-INF/domain directory
     */
    private List<File> customConfigs;

    /**
     * Logger
     */
    private final Log log;

    /**
     * Create a new Custom File Copy Processor instance.
     * 
     * @param log the logger to use
     */
    public CustomFileCopyProcessor(Log log) {
        this.log = log;
    }

    @Override
    public void handle(MojoExecutor.ExecutionEnvironment environment) throws MojoExecutionException {

        executeMojo(resourcesPlugin,
                goal("copy-resources"),
                configuration(
                        element(name("outputDirectory"), OUTPUT_FOLDER + MICROINF_DOMAIN_FOLDER),
                        element(name("resources"),
                                constructResourceElements()
                        )
                ),
                environment
        );

        gotoNext(environment);
    }

    /**
     * Helper method to build the "resource" elements to pass to copy-resources.
     * 
     * This will include the default resources, and any custom configuration files
     * which have been specified.
     * 
     * @return the array of elements. This will always be non-null and contain at
     *         least one element.
     */
    protected MojoExecutor.Element[] constructResourceElements() {

        List<MojoExecutor.Element> elements = new ArrayList<>();
        /* Add in the default items */
        elements.add(
            element(name("resource"), 
                element(name("directory"), "${project.build.resources[0].directory}"),
                element(name("includes"), 
                    element(name("include"), "domain.xml"),
                    element(name("include"), "keystore.jks"), 
                    element(name("include"), "login.conf"),
                    element(name("include"), "logging.properties")
                )
            )
        );

        /* Add in the custom configuration file entries */
        if (customConfigs != null && !customConfigs.isEmpty()) {

            for (File file : customConfigs) {
                if (file.exists()) {
                    log.debug("Including custom configuration file [" + file.getAbsolutePath() + "]");

                    elements.add(
                        element(name("resource"),
                            element(name("directory"), file.getParent()), 
                            element(name("include"), file.getName())
                        )
                    );
                } else {
                    log.warn("Custom configuration file [" + file.getAbsolutePath()
                            + "] doesn't exist, won't be copied");
                }
            }
        }

        return elements.toArray(new MojoExecutor.Element[elements.size()]);
    }

    /**
     * Set the configuration files that will be copied in addition to the default
     * entries.
     * 
     * @param customConfigs list of files to copy. This can be null.
     * @return this instance.
     */
    public BaseProcessor set(List<File> customConfigs) {
        this.customConfigs = customConfigs;
        return this;
    }
}