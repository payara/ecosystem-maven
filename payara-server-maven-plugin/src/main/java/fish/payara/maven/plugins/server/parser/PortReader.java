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
package fish.payara.maven.plugins.server.parser;

import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class PortReader {

    private String serverName;
    private int httpPort = -1;
    private int httpsPort = -1;
    private int adminPort = -1;
    private String serverConfigName = "";

    public PortReader(String domainXmlPath, String serverName) {
        this.serverName = serverName;
        parseDomainXML(domainXmlPath);
    }

    private void parseDomainXML(String domainXmlPath) {
        try {
            File file = new File(domainXmlPath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            // Normalize the XML structure
            document.getDocumentElement().normalize();

            NodeList servers = document.getElementsByTagName("server");
            NodeList configs = document.getElementsByTagName("config");

            // Iterate over servers
            for (int i = 0; i < servers.getLength(); i++) {
                Node serverNode = servers.item(i);
                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element serverElement = (Element) serverNode;
                    String name = serverElement.getAttribute("name");
                    if (name.equals(serverName)) {
                        serverConfigName = serverElement.getAttribute("config-ref");
                    }
                }
            }

            // Iterate over configs
            for (int i = 0; i < configs.getLength(); i++) {
                Node configNode = configs.item(i);
                if (configNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element configElement = (Element) configNode;
                    String configName = configElement.getAttribute("name");
                    if (configName.equals(serverConfigName)) {
                        NodeList networkListeners = configElement.getElementsByTagName("network-listener");
                        for (int j = 0; j < networkListeners.getLength(); j++) {
                            Node networkListenerNode = networkListeners.item(j);
                            if (networkListenerNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element networkListenerElement = (Element) networkListenerNode;
                                String name = networkListenerElement.getAttribute("name");
                                int port = Integer.parseInt(networkListenerElement.getAttribute("port"));
                                if (name.equals("http-listener-1")) {
                                    httpPort = port;
                                } else if (name.equals("http-listener-2")) {
                                    httpsPort = port;
                                } else if (name.equals("admin-listener")) {
                                    adminPort = port;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException | ParserConfigurationException | SAXException ex) {
            throw new IllegalStateException("Unable to parse domain.xml: " + ex.getMessage());
        }
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public static void main(String[] args) {
        try {
            PortReader reader = new PortReader("path/to/domain.xml", "server-name");
            System.out.println("HTTP Port: " + reader.getHttpPort());
            System.out.println("HTTPS Port: " + reader.getHttpsPort());
            System.out.println("Admin Port: " + reader.getAdminPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
