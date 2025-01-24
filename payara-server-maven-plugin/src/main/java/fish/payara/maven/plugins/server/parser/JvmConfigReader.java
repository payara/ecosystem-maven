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

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JvmConfigReader {

    private final String serverName;
    private final List<JvmOption> jvmOptions = new ArrayList<>();
    private final Map<String, String> propMap = new HashMap<>();
    private boolean monitoringEnabled = false;
    private String serverConfigName = "";
    private boolean readConfig = false;

    public JvmConfigReader(String domainXmlPath, String serverName) {
        this.serverName = serverName;
        parseDomainXML(domainXmlPath);
    }

    private void parseDomainXML(String domainXmlPath) {
        try {
            File xmlFile = new File(domainXmlPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList servers = doc.getElementsByTagName("server");
            NodeList configs = doc.getElementsByTagName("config");

            // Find the server with the matching name
            for (int i = 0; i < servers.getLength(); i++) {
                Node serverNode = servers.item(i);
                if (serverNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element serverElement = (Element) serverNode;
                    if (serverName.equals(serverElement.getAttribute("name"))) {
                        serverConfigName = serverElement.getAttribute("config-ref");
                        break;
                    }
                }
            }

            // Find the config with the matching name
            for (int i = 0; i < configs.getLength(); i++) {
                Node configNode = configs.item(i);
                if (configNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element configElement = (Element) configNode;
                    if (serverConfigName.equals(configElement.getAttribute("name"))) {
                        NodeList javaConfigList = configElement.getElementsByTagName("java-config");
                        if (javaConfigList.getLength() > 0) {
                            Element javaConfig = (Element) javaConfigList.item(0);

                            // Parse jvm-options
                            NodeList jvmOptionsList = javaConfig.getElementsByTagName("jvm-options");
                            for (int j = 0; j < jvmOptionsList.getLength(); j++) {
                                Node jvmOptionNode = jvmOptionsList.item(j);
                                if (jvmOptionNode.getNodeType() == Node.ELEMENT_NODE) {
                                    String value = jvmOptionNode.getTextContent().trim();
                                    jvmOptions.add(new JvmOption(value));
                                }
                            }

                            // Parse attributes into propMap
                            NamedNodeMap attributes = javaConfig.getAttributes();
                            for (int k = 0; k < attributes.getLength(); k++) {
                                Node attribute = attributes.item(k);
                                propMap.put(attribute.getNodeName(), attribute.getNodeValue());
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse file " + domainXmlPath + " : " + e.getMessage(), e);
        }
    }

    public List<JvmOption> getJvmOptions() {
        return jvmOptions;
    }

    public Map<String, String> getPropMap() {
        return propMap;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

}
