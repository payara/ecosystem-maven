/*
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.maven.plugins.micro;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import fish.payara.maven.plugins.AutoDeployHandler;
import fish.payara.maven.plugins.Source;
import fish.payara.maven.plugins.WebDriverFactory;

/**
 *
 * @author Gaurav Gupta
 */
public class MicroAutoDeployHandler extends AutoDeployHandler {

    private final StartMojo start;

    public MicroAutoDeployHandler(StartMojo start, File webappDirectory) {
        super(start, webappDirectory);
        this.start = start;

    }

    @Override
    public void reload(boolean rebootRequired) {
        if (rebootRequired) {
            if (start.getMicroProcess().isAlive()) {
                WebDriverFactory.updateTitle("Restarting", project, start.getDriver(), log);
                start.getMicroProcess().destroy();
            }
        } else {
            WebDriverFactory.updateTitle(RELOADING, project, start.getDriver(), log);
            ReloadMojo reloadMojo = new ReloadMojo(project, log);
            reloadMojo.setDevMode(true);
            if (start.contextRoot != null) {
                reloadMojo.setContextRoot(start.contextRoot);
            }
            reloadMojo.setKeepState(start.keepState);
            if (start.hotDeploy) {
                Path rootPath = project.getBasedir().toPath();
                List<String> sourcesChanged = new ArrayList<>();
                reloadMojo.setHotDeploy(start.hotDeploy);
                for (Source source : sourceUpdatedPending) {
                    String extension = source.getPath().toString().substring(source.getPath().toString().lastIndexOf('.') + 1);
                    if (extension.equals("xml") || extension.equals("properties")) {
                        reloadMojo.setMetadataChanged(true);
                    }
                    Path relativePath = rootPath.relativize(source.getPath());
                    sourcesChanged.add(relativePath.toString().replace(File.separator, "/"));
                }
                log.debug("SourcesChanged: " + sourcesChanged);
                reloadMojo.setSourcesChanged(String.join(", ", sourcesChanged));
            }
            try {
                reloadMojo.execute();
            } catch (MojoExecutionException ex) {
                log.error("Error invoking Reload", ex);
            }

        }
    }

}
