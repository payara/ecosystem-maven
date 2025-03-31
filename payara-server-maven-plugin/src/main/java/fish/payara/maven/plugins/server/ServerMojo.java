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
import java.util.Properties;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromConfiguration.ArtifactItem;

/**
 *
 * @author Gaurav Gupta
 */
public abstract class ServerMojo extends BasePayaraMojo {

    /**
     * Path to the Java executable used to run Payara.
     */
    @Parameter(property = "payara.java.home", defaultValue = "${env.PAYARA_JAVA_HOME}")
    protected String javaHome;

    /**
     * Version of the Payara server.
     */
    @Parameter(property = "payara.server.version", defaultValue = "${env.PAYARA_SERVER_VERSION}")
    protected String payaraServerVersion;

    /**
     * Absolute path to the Payara server installation directory.
     */
    @Parameter(property = "payara.server.path", defaultValue = "${env.PAYARA_SERVER_PATH}")
    protected String payaraServerPath;

    /**
     * Payara server domain to be used.
     */
    @Parameter(property = "payara.domain.name", defaultValue = "${env.PAYARA_DOMAIN_NAME}")
    protected String domainName;

    /**
     * Specifies the artifact item to be deployed.
     */
    @Parameter
    protected ArtifactItem artifactItem;

    /**
     * If true, deploys the application in an exploded (unpacked) format.
     */
    @Parameter(property = "payara.exploded", defaultValue = "${env.PAYARA_EXPLODED}")
    protected boolean exploded;

    /**
     * The context root under which the application is deployed.
     */
    @Parameter(property = "payara.context.root", defaultValue = "${env.PAYARA_CONTEXT_PATH}")
    protected String contextRoot;

    /**
     * If true, connects to a remote Payara server instance.
     */
    @Parameter(property = "payara.remote", defaultValue = "${env.PAYARA_REMOTE}")
    protected boolean remote;

    /**
     * Hostname or IP address of the Payara server.
     */
    @Parameter(property = "payara.host.name", defaultValue = "${env.PAYARA_HOST_NAME}")
    protected String hostName;

    /**
     * Admin port for Payara server administration.
     */
    @Parameter(property = "payara.admin.port", defaultValue = "${env.PAYARA_ADMIN_PORT}")
    protected String adminPort;

    /**
     * HTTP port for accessing deployed applications.
     */
    @Parameter(property = "payara.http.port", defaultValue = "${env.PAYARA_HTTP_PORT}")
    protected String httpPort;

    /**
     * HTTPS port for accessing deployed applications securely.
     */
    @Parameter(property = "payara.https.port", defaultValue = "${env.PAYARA_HTTPS_PORT}")
    protected String httpsPort;

    /**
     * Protocol to be used for server communication (e.g., HTTP or HTTPS).
     */
    @Parameter(property = "payara.protocol", defaultValue = "${env.PAYARA_PROTOCOL}")
    protected String protocol;

    /**
     * Password for the Payara admin user.
     */
    @Parameter(property = "payara.admin.password", defaultValue = "${env.PAYARA_ADMIN_PASSWORD}")
    protected String adminPassword;

    /**
     * Path to the file containing the Payara admin password.
     */
    @Parameter(property = "payara.admin.password.file", defaultValue = "${env.PAYARA_ADMIN_PASSWORD_FILE}")
    protected String adminPasswordFile;

    /**
     * Username for Payara server administration.
     */
    @Parameter(property = "payara.admin.user", defaultValue = "${env.PAYARA_ADMIN_USER}")
    protected String adminUser;

    /**
     * Name of the Payara server instance.
     */
    @Parameter(property = "payara.instance.name", defaultValue = "${env.PAYARA_INSTANCE}")
    protected String instanceName;
    
    public ServerMojo() {
        if (domainName == null || domainName.isEmpty()) {
            domainName = "domain1";
        }
    }

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
        if (adminPassword == null) {
            adminPassword = getAdminPasswordFromFile();
        }
        return adminPassword;
    }
}
