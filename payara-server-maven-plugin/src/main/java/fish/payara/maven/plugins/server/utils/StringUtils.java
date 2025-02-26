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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    private static final String PATTERN = "\\$\\{([^}]+)\\}";

    /**
     * Add quotes to string if and only if it contains space characters.
     *
     * Note: does not handle generalized white space (tabs, localized white
     * space, etc.)
     *
     * @param path File path in string form.
     * @return Quoted path if it contains any space characters, otherwise same.
     */
    public static String quote(String path) {
        return path.indexOf(' ') == -1 ? path : "\"" + path + "\"";
    }

    /**
     * Utility method that finds all occurrences of variable references and
     * replaces them with their values. Values are taken from
     * <code>varMap</code> and escaped. If they are not present there, system
     * properties are queried. If not found there the variable reference is
     * replaced with the same string with special characters escaped.
     *
     * @param input String value where the variables have to be replaced with
     * values
     * @param varMap mapping of variable names to their values
     * @return String where the all the replacement was done
     */
    public static String doSub(String input, Map<String, String> varMap) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = varMap.getOrDefault(varName, System.getProperty(varName, ""));
            input = input.replace("${" + varName + "}", escapePath(replacement));
        }
        return input;
    }

    /**
     * Add escape characters for backslash and dollar sign characters in path
     * field.
     *
     * @param path file path in string form.
     * @return adjusted path with backslashes and dollar signs escaped with
     * backslash character.
     */
    public static String escapePath(String path) {
        return path.replace("\\", "\\\\").replace("$", "\\$");
    }
}
