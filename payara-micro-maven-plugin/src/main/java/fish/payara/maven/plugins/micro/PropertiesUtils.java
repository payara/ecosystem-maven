/*
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.maven.plugins.micro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *
 * @author Gaurav Gupta
 */
public class PropertiesUtils {

    public static void saveProperties(String key, String value) {
        String tempDir = System.getProperty("java.io.tmpdir");

        // File path in the system's default temporary directory to store the properties
        String filePath = tempDir + File.separator + "payara-maven-config.properties";

        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream(filePath);

            // Set the key-value pair
            prop.setProperty(key, value);

            // Save properties to the file
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getProperty(String key, String defaultValue) {

        String tempDir = System.getProperty("java.io.tmpdir");

        // File path in the system's default temporary directory to store the properties
        String filePath = tempDir + File.separator + "payara-maven-config.properties";
        Properties prop = new Properties();
        InputStream input = null;
        String value = null;

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                saveProperties(key, key);
            }
            input = new FileInputStream(filePath);

            // Load the properties file
            prop.load(input);

            // Get the value for the provided key
            value = prop.getProperty(key);
            if (value == null) {
                value = defaultValue;
            }
        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

}
