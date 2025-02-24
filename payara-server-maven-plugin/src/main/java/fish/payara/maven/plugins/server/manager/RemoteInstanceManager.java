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

import fish.payara.maven.plugins.server.Command;
import fish.payara.maven.plugins.server.response.Response;
import java.util.List;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Gaurav Gupta
 */
public class RemoteInstanceManager extends InstanceManager<PayaraServerRemoteInstance> {
    
    public RemoteInstanceManager( PayaraServerRemoteInstance payaraServer, Log log) {
        super(payaraServer, log);
    }
    
    private String appendPreviousStart, appendnextStart;
    private static final String APPEND_NEXT_HEADER = "X-Text-Append-Next";

    public String fetchLogs(String instanceName) {
        Command command = new Command(MANAGEMENT_PATH, VIEW_LOG_COMMAND, "0");
        command.setContentType(CONTENT_TYPE_PLAIN_TEXT);
        command.setInstanceName(instanceName);
        if (appendnextStart == null) {
            command.setQuery(query(command));
        } else {
            command.setQuery(appendnextStart);
        }

        Response logResponse;
        try {
            logResponse = invokeServer(payaraServer, command);
            List<String> headerValues = logResponse.getHeaderFields().get(APPEND_NEXT_HEADER);
            if (headerValues != null && !headerValues.isEmpty()) {
                String headerValue = headerValues.get(0);
                int questionMarkIndex = headerValue.indexOf('?');
                if (questionMarkIndex != -1 && questionMarkIndex < headerValue.length() - 1) {
                    appendPreviousStart = appendnextStart;
                    appendnextStart = headerValue.substring(questionMarkIndex + 1);
                }
            }
            return  appendPreviousStart == null || appendnextStart.equals(appendPreviousStart)? "" : logResponse.toString();
        } catch (Exception ex) {
            log.error("Error retrieving log: " + ex.getMessage());
        }
        return null;
    }

}
