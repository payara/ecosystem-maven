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
package fish.payara.maven.plugins.server.manager;

import fish.payara.maven.plugins.server.parser.JDKVersion;
import fish.payara.maven.plugins.server.parser.PortReader;
import static fish.payara.maven.plugins.server.Configuration.DAS_NAME;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Gaurav Gupta
 */
public class PayaraServerLocalInstance extends PayaraServerInstance {
    
    
    public static final String GLASSFISH_DIR = "glassfish";
    public static final String MODULES_DIR = "modules";
    public static final String DOMAINS_DIR = "domains";
    public static final String CONFIG_DIR = "config";
    public static final String LOGS_DIR = "logs";
    public static final String SERVER_LOG = "server.log";
    public static final String DOMAIN_XML = "domain.xml";
    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final String LOCALHOST = "localhost";

    private PortReader portReader;
    private Process logStream;

    private final String path;
    private final String domainName;
    private String jdkHome;

    public PayaraServerLocalInstance(String jdkHome, String path, String domainName) {
        this.jdkHome = jdkHome;
        this.path = path;
        this.domainName = domainName;
    }

    public String getId() {
        return getDomainPath();
    }

    public String getPath() {
        return path;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getServerRoot() {
        return getPath();
    }

    public String getServerHome() {
        return Paths.get(getServerRoot(), GLASSFISH_DIR).toString();
    }

    public String getServerModules() {
        return Paths.get(getServerHome(), MODULES_DIR).toString();
    }

    public String getDomainsFolder() {
        return Paths.get(getServerHome(), DOMAINS_DIR).toString();
    }

    public String getDomainPath() {
        return Paths.get(getDomainsFolder(), getDomainName()).toString();
    }

    public String getDomainXml() {
        return Paths.get(getDomainPath(), CONFIG_DIR, DOMAIN_XML).toString();
    }

    public String getServerLog() {
        return Paths.get(getDomainPath(), LOGS_DIR, SERVER_LOG).toString();
    }

    public String readServerLog() throws IOException {
        return readServerLog(5000);
    }

    public String readServerLog(int maxLength) throws IOException {
        String logContent = readFileWithReplacement(getServerLog());
        return logContent.length() > maxLength ? logContent.substring(0, maxLength) : logContent;
    }

    public String readDomainXml() throws IOException {
        return readFileWithReplacement(getDomainXml());
    }

    /**
     * Helper to read any file as UTF-8, replacing malformed or unmappable input.
     */
    private String readFileWithReplacement(String filePath) throws IOException {
        byte[] raw = Files.readAllBytes(Paths.get(filePath));

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(raw));
        return decoded.toString();
    }

    public String getJDKHome() {
        if (this.jdkHome != null) {
            return this.jdkHome;
        }
        return JDKVersion.getDefaultJDKHome();
    }

    public void setJdkHome(String jdkHome) {
        this.jdkHome = jdkHome;
    }

    @Override
    public String getProtocol() {
        if (protocol == null) {
            return HTTP;
        }
        return protocol;
    }

    @Override
    public String getHost() {
        return host == null ? LOCALHOST : host;
    }

    @Override
    public int getHttpPort() {
        if (httpPort > 0) {
            return httpPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpPort();
    }

    @Override
    public int getHttpsPort() {
        if (httpsPort > 0) {
            return httpsPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpsPort();
    }

    @Override
    public int getAdminPort() {
        if (adminPort > 0) {
            return adminPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getAdminPort();
    }

    private PortReader createPortReader() {
        return new PortReader(getDomainXml(), DAS_NAME);
    }

    public void disconnectOutput() {
        if (logStream != null) {
            logStream.destroy();
        }
    }
}
