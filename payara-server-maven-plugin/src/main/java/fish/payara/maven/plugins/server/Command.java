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

import static fish.payara.maven.plugins.server.manager.InstanceManager.CONTENT_TYPE_JSON;

/**
 *
 * @author Gaurav Gupta
 */
public class Command {

    private final String value;
    private final String rootPath;
    private final String command;
    private String instanceName;
    private String path;
    private String query;
    private boolean dirDeploy;
    private String contextRoot;
    private boolean hotDeploy;
    private String contentType = CONTENT_TYPE_JSON;

    public Command(String rootPath, String command, String value) {
        this.rootPath = rootPath;
        this.command = command;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getPath() {
        return path;
    }

    public String getCommand() {
        return command;
    }

    public String getQuery() {
        return query;
    }

    public boolean isDirDeploy() {
        return dirDeploy;
    }

    public void setDirDeploy(boolean dirDeploy) {
        this.dirDeploy = dirDeploy;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public boolean isHotDeploy() {
        return hotDeploy;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
