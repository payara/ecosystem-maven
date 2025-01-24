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
package fish.payara.maven.plugins.server.utils;

import java.io.File;

public class ServerUtils {

    /** 
     * Payara server Java VM root property name.
     */
    public static final String PF_JAVA_ROOT_PROPERTY = "com.sun.aas.javaRoot";

    /** 
     * Payara server home property name.
     *
     * It's value says it is server installation root but in reality it is just
     * <code>payara</code> subdirectory under server installation root which
     * we usually call server home.
     */
    public static final String PF_HOME_PROPERTY = "com.sun.aas.installRoot";

    /** 
     * Payara server domain root property name.
     *
     * It's value says it is server instance root which is the same. 
     */
    public static final String PF_DOMAIN_ROOT_PROPERTY = "com.sun.aas.instanceRoot";

    /** 
     * Payara server Derby root property name.
     */
    public static final String PF_DERBY_ROOT_PROPERTY = "com.sun.aas.derbyRoot";

    /** 
     * Payara server domain name command line argument.
     */
    public static final String PF_DOMAIN_ARG = "--domain";
    
    
    public static final String ASADMIN = "asadmin";
    public static final String STOP_DOMAIN = "stop-domain";

    /** 
     * Payara server domain directory command line argument.
     */
    public static final String PF_DOMAIN_DIR_ARG = "--domaindir";

    /** Payara main class to be started when using classpath. */
    public static final String PF_MAIN_CLASS = "com.sun.enterprise.glassfish.bootstrap.ASMain";

    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "";
    public static final String MASTER_PASSWORD = "changeit";
    public static final int DEFAULT_ADMIN_PORT = 4848;
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final String DEFAULT_HOST = "localhost";

    /** Default name of the DAS server. */
    public static final String DAS_NAME = "server";

    /** Default retry count to check alive status of server. */
    public static final int DEFAULT_RETRY_COUNT = 30;

    /** Default sleep time in millisecond before retry to check alive status of server. */
    public static final int DEFAULT_WAIT = 3000;

    /**
     * Builds command line argument containing argument identifier, space
     * and argument value, e.g. <code>--name value</code>.
     *
     * @param name      Command line argument name including dashes at
     *                  the beginning.
     * @param value     Value to be appended prefixed with single space.
     * @return Command line argument concatenated together.
     */
    public static String cmdLineArgument(String name, String value) {
        return name + " " + value;
    }

    /**
     * Checks if the provided server path is valid by ensuring the existence of
     * the necessary Payara server files.
     *
     * @param serverPath Path to the server directory.
     * @return True if the server path is valid, otherwise false.
     */
    public static boolean isValidServerPath(String serverPath) {
        File asadminInGlassfish = new File(serverPath + "/glassfish/bin/asadmin");
        File asadminInBin = new File(serverPath + "/bin/asadmin");
        return asadminInGlassfish.exists() && asadminInBin.exists();
    }
}
