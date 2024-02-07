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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Gaurav Gupta
 */
public class LogUtils {

    private static final String WARNING_LEVEL = "WARNING";
    private static final String SEVERE_LEVEL = "SEVERE";
    private static final String LOG_REGEX = "\\[([^\\[\\]]*)\\].*(\\[.*(INFO|WARNING|SEVERE).*\\]).*\\[levelValue\\: \\d+\\](.*)";
    private static final String WHITE_COLOR_CODE = "\033[97m" + '[';
    private static final String YELLOW_COLOR_CODE = "\033[93m" + '[';
    private static final String RED_COLOR_CODE = "\033[91m" + '[';
    private static final String RESET_COLOR_CODE = ']' + "\033[0m ";

    public static String trimLog(String line) {
        Pattern pattern = Pattern.compile(LOG_REGEX);
        Matcher matcher = pattern.matcher(line);
        boolean find = matcher.find();
        if (find) {
            String timeStamp = matcher.group(1).trim();
            String level = matcher.group(3).trim();
            String content = matcher.group(4).trim();
            switch (level) {
                case WARNING_LEVEL:
                    return warning(getTimestamp(timeStamp) + " ", content);
                case SEVERE_LEVEL:
                    return severe(getTimestamp(timeStamp) + " ", content);
                default:
                    return getTimestamp(timeStamp) + " " + content;
            }
        } else {
            return line;
        }
    }

    public static String highlight(String text) {
        int leadingSpaces = 0;
        while (leadingSpaces < text.length() && Character.isWhitespace(text.charAt(leadingSpaces))) {
            leadingSpaces++;
        }
        String highlightedText = "\033[44m\033[97m" + text.substring(leadingSpaces) + "\033[0m";
        return " ".repeat(leadingSpaces) + highlightedText;
    }

    private static String warning(String timeStamp, String line) {
        return timeStamp + YELLOW_COLOR_CODE + WARNING_LEVEL + RESET_COLOR_CODE + line;
    }

    private static String severe(String timeStamp, String line) {
        return timeStamp + RED_COLOR_CODE + SEVERE_LEVEL + RESET_COLOR_CODE + line;
    }

    private static String getTimestamp(String timestampString) {
        int timeStartIndex = timestampString.indexOf('T') + 1;
        int offsetSignIndex = timestampString.indexOf('+', timeStartIndex);

        // If '+' sign is not found, try '-' sign
        if (offsetSignIndex == -1) {
            offsetSignIndex = timestampString.indexOf('-', timeStartIndex);
        }

        String timeString = timestampString.substring(timeStartIndex, offsetSignIndex);

        return WHITE_COLOR_CODE + timeString + RESET_COLOR_CODE;
    }
}
