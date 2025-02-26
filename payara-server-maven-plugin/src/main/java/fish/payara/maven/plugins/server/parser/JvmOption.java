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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JvmOption {

    private static final String PATTERN = "^\\[(.*)\\|(.*)\\](.*)";
    private String option;
    private String vendor;
    private JDKVersion minVersion;
    private JDKVersion maxVersion;

    public JvmOption(String option) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(option);

        if (matcher.matches() && matcher.groupCount() == 3) {
            // [Azul-1.8.0|1.8.0u120]-Xbootclasspath
            String vendorString = matcher.group(1);
            if (vendorString.contains("-") && vendorString.matches(".*[a-zA-Z].*")) {
                String[] parts = vendorString.split("-");
                this.vendor = parts[0];
                this.minVersion = JDKVersion.toValue(parts[1], null);
            } else {
                this.vendor = null;
                this.minVersion = JDKVersion.toValue(vendorString, null);
            }
            this.maxVersion = JDKVersion.toValue(matcher.group(2), null);
            this.option = matcher.group(3);
        } else {
            this.option = option;
        }
    }

    public String getOption() {
        return option;
    }

    public String getVendor() {
        return vendor;
    }

    public JDKVersion getMinVersion() {
        return minVersion;
    }

    public JDKVersion getMaxVersion() {
        return maxVersion;
    }

    @Override
    public String toString() {
        return "JvmOption{"
                + "option='" + option + '\''
                + ", vendor='" + vendor + '\''
                + ", minVersion=" + minVersion
                + ", maxVersion=" + maxVersion
                + '}';
    }
}
