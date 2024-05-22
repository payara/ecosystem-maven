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
package fish.payara.maven.plugins.cloud;

import fish.payara.cloud.client.ApplicationResource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import fish.payara.maven.plugins.micro.AutoDeployHandler;
import fish.payara.maven.plugins.micro.PropertiesUtils;
import fish.payara.maven.plugins.micro.StartTask;
import fish.payara.maven.plugins.micro.WebDriverFactory;
import fish.payara.tools.cloud.ApplicationContext;
import fish.payara.tools.cloud.DeployApplication;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.project.MavenProject;
import org.openqa.selenium.WebDriver;

/**
 * DevMojo is a Maven Mojo for running the Payara application in development mode.
 * It allows for automatic deployment to Payara Cloud and live reloading of applications.
 * 
 * @autor Gaurav Gupta
 */
@Mojo(name = "dev")
public class DevMojo extends BasePayaraMojo implements StartTask {

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.war", property = "applicationPath")
    protected File applicationPath;

    @Parameter(property = "autoDeploy")
    protected Boolean autoDeploy;

    @Parameter(property = "liveReload")
    protected Boolean liveReload;

    @Parameter(property = "browser")
    protected String browser;

    private String appUrl;
    private AutoDeployHandler autoDeployHandler;
    private WebDriver driver;
    private DeployApplication controller;
    private ApplicationContext context;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            context = getApplicationContextBuilder().build();
            if (autoDeploy == null) {
                autoDeploy = true;
            }
            if (liveReload == null) {
                liveReload = true;
            }
            if (autoDeploy && autoDeployHandler == null) {
                autoDeployHandler = new CloudAutoDeployHandler(this, applicationPath);
                Thread devModeThread = new Thread(autoDeployHandler);
                devModeThread.setDaemon(true);
                devModeThread.start();
            } else {
                autoDeployHandler = null;
            }
            controller = new DeployApplication(context, applicationPath);
            try {
                deploy();
            } catch (Exception ex) {
                context.getOutput().error("Deployment failed with an exception.", ex);
            }
            if (autoDeploy) {
                while (autoDeployHandler.isAlive()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        context.getOutput().error("Thread interrupted while waiting for auto-deployment.", ex);
                    }
                }
            }

        } finally {
            if (autoDeployHandler != null) {
                autoDeployHandler.stop();
            }
            if (driver != null) {
                try {
                    PropertiesUtils.saveProperties(appUrl, driver.getCurrentUrl());
                } catch (Throwable t) {
                    context.getOutput().error("Failed to save properties after execution.", t);
                } finally {
                    try {
                        driver.quit();
                    } catch (Throwable t) {
                        getLog().debug(t);
                    }
                }
            }
        }
    }

    public void deploy() {
        try {
            Optional<ApplicationResource> res = controller.call();
            if (res.isPresent()
                    && res.get().representation() != null
                    && (appUrl = res.get().representation().getString("applicationEndpoint")) != null) {
                if (driver == null) {
                    String url = PropertiesUtils.getProperty(appUrl, appUrl);
                    try {
                        driver = WebDriverFactory.createWebDriver(browser, getLog());
                        driver.get(url);
                    } catch (Exception ex) {
                        context.getOutput().error("Error in running WebDriver", ex);
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(new URI(appUrl));
                            }
                        } catch (IOException | URISyntaxException e) {
                            context.getOutput().error("Error in running Desktop browse", e);
                        } finally {
                            driver = null;
                        }
                    }
                } else {
                    if (res.get().representation().getString("status").equals("RUNNING")) {
                        driver.navigate().refresh();
                    } else if (res.isPresent()) {
                        if (res.get().representation() != null) {
                            context.getOutput().warning("The application is not running. Current status: " + res.get().representation().toString());
                        } else {
                            context.getOutput().warning("The application resource is empty.");
                        }
                    } else {
                        context.getOutput().warning("The application resource is not present.");
                    }
                }
            } else if (res.isPresent()) {
                if (res.get().representation() != null) {
                    context.getOutput().warning("The application resource is present with representation. Current status: " + res.get().representation().toString());
                } else {
                    context.getOutput().warning("The application resource is present but its representation is null.");
                }
            } else {
                context.getOutput().warning("No application resource present.");
            }
        } catch (Exception ex) {
            context.getOutput().error("Deployment failed due to an unexpected exception.", ex);
        }
    }

    @Override
    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public MavenProject getProject() {
        return this.getEnvironment().getMavenProject();
    }

    @Override
    public List<String> getRebootOnChange() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

}
