/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class ServerMojo extends BasePayaraMojo {

    @Parameter(property = "javaPath")
    protected String javaPath;

    @Parameter(property = "payaraVersion", defaultValue = "6.2024.12")
    protected String payaraVersion;

    @Parameter(property = "payaraServerAbsolutePath")
    protected String payaraServerAbsolutePath;
    
    @Parameter(property = "domain", defaultValue = "domain1")
    protected String domain;

    @Parameter(property = "artifactItem")
    protected ArtifactItem artifactItem;
    
    @Parameter(property = "exploded", defaultValue = "false")
    protected boolean exploded;

    @Parameter(property = "contextRoot")
    protected String contextRoot;
    
    @Parameter(property = "remote", defaultValue = "false")
    protected boolean remote;
    
    @Parameter(property = "host", defaultValue = "${env.PAYARA_HOST}")
    protected String host;
    
    @Parameter(property = "adminPort", defaultValue = "${env.PAYARA_ADMIN_PORT}")
    protected String adminPort;
    
    @Parameter(property = "httpPort", defaultValue = "${env.PAYARA_HTTP_PORT}")
    protected String httpPort;
    
    @Parameter(property = "httpsPort", defaultValue = "${env.PAYARA_HTTPS_PORT}")
    protected String httpsPort;

    @Parameter(property = "protocol", defaultValue = "${env.PAYARA_PROTOCOL}")
    protected String protocol;

    @Parameter(property = "adminPassword", defaultValue = "${env.PAYARA_ADMIN_PASSWORD}")
    protected String adminPassword;

    @Parameter(property = "adminPasswordFile", defaultValue = "${env.PAYARA_ADMIN_PASSWORD_FILE}")
    protected String adminPasswordFile;

    @Parameter(property = "adminUser", defaultValue = "${env.PAYARA_ADMIN_USER}")
    protected String adminUser;
    
    @Parameter(property = "instanceName")
    protected String instanceName;

    protected String getAdminPasswordFromFile() {
        if (adminPasswordFile != null) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(adminPasswordFile)) {
                props.load(fis);
                return props.getProperty("adminPassword");
            } catch (Exception ex) {
                return null;
            }
        }
        return adminPassword;
    }
    
    protected String getAdminPassword() {
        if(adminPassword == null) {
            adminPassword = getAdminPasswordFromFile();
        }
        return adminPassword;
    }
}
