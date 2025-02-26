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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JavaUtils {

    /**
     * Java executables directory under Java home.
     */
    private static final String JAVA_BIN_DIR = "bin";

    /**
     * Java VM executable file name (without path).
     */
    private static final String JAVA_VM_EXE = "java";

    /**
     * Java Process file name (without path).
     */
    private static final String JAVA_PROCESS_EXE = "jps";

    /**
     * Java SE JDK class path option.
     */
    public static final String VM_CLASSPATH_OPTION = "-cp";

    /**
     * Java VM system property option.
     */
    private static final String VM_SYS_PROP_OPT = "-D";

    /**
     * Java VM system property quoting character.
     */
    private static final String VM_SYS_PROP_QUOTE = "\"";

    /**
     * Java VM system property assignment.
     */
    private static final String VM_SYS_PROP_ASSIGN = "=";

    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Append quoted Java VM system property
     * <code>-D"&lt;name&gt;=&lt;value&gt;"</code> into a StringBuilder.
     *
     * @param name Java VM system property name.
     * @param value Java VM system property value.
     * @return formatted system property string.
     */
    public static String systemProperty(String name, String value) {
        return VM_SYS_PROP_OPT + VM_SYS_PROP_QUOTE + name + VM_SYS_PROP_ASSIGN + value + VM_SYS_PROP_QUOTE;
    }

    /**
     * Build Java VM executable full path from Java Home directory.
     *
     * @param javaHome Full path to Java Home directory.
     * @return Java VM executable full path.
     */
    public static String javaVmExecutableFullPath(String javaHome) {
        return javaExecutableFullPath(javaHome, JAVA_VM_EXE);
    }

    /**
     * Build Java Process executable full path from Java Home directory.
     *
     * @param javaHome Full path to Java Home directory.
     * @return Java Process executable full path.
     */
    public static String javaProcessExecutableFullPath(String javaHome) {
        return javaExecutableFullPath(javaHome, JAVA_PROCESS_EXE);
    }

    private static String javaExecutableFullPath(String javaHome, String type) {
        Path javaHomePath = Paths.get(javaHome);
        String javaExecStr = javaHomePath.toString();

        if (!javaExecStr.endsWith(System.getProperty("file.separator"))) {
            javaExecStr += System.getProperty("file.separator");
        }

        javaExecStr += JAVA_BIN_DIR + System.getProperty("file.separator") + type;

        if (IS_WIN) {
            javaExecStr += ".exe";
        }

        return javaExecStr;
    }

    /**
     * Parses parameters from a given string in a shell-like manner and appends
     * them to the executable file.
     *
     * Users of the Bourne shell (e.g., on Unix) will already be familiar with
     * the behavior. For example, you should be able to: Include command names
     * with embedded spaces, such as
     * <code>c:\Program Files\jdk\bin\javac</code>. Include extra command
     * arguments, such as <code>-Dname=value</code>. Do anything else which
     * might require unusual characters or processing. For example:      <code>
     * "c:\program files\jdk\bin\java" -Dmessage="Hello /\\/\\ there!" -Xmx128m
     * </code>
     *
     * This example would create the following executable name and arguments:
     *
     * <code>c:\program files\jdk\bin\java</code>
     * <code>-Dmessage=Hello /\/\ there!</code> <code>-Xmx128m</code>
     *
     * Note that the command string does not escape its backslashes--under the
     * assumption that Windows users will not think to do this, meaningless
     * escapes are just left as backslashes plus the following character.
     *
     * Caveat: even after parsing, Windows programs (such as the Java launcher)
     * may not fully honor certain characters, such as quotes, in command names
     * or arguments. This is because programs under Windows frequently perform
     * their own parsing and unescaping (since the shell cannot be relied on to
     * do this). On Unix, this problem should not occur.
     *
     * @param args A string to parse.
     * @return A list of executable file and parameters to be passed to it.
     */
    public static List<String> parseParameters(String args) {
        int NULL = 0;
        int INPARAM = 1;
        int INPARAMPENDING = 2;
        int STICK = 4;
        int STICKPENDING = 8;

        List<String> params = new ArrayList<>();
        StringBuilder buff = new StringBuilder();
        int state = NULL;
        int slength = args.length();

        for (int i = 0; i < slength; i++) {
            char c = args.charAt(i);
            if (Character.isWhitespace(c)) { // check for whitespace
                if (state == NULL) {
                    if (buff.length() > 0) {
                        params.add(buff.toString());
                        buff.setLength(0);  // reset buffer
                    }
                } else if (state == STICK) {
                    params.add(buff.toString());
                    buff.setLength(0);  // reset buffer
                    state = NULL;
                } else if (state == STICKPENDING) {
                    buff.append('\\');
                    params.add(buff.toString());
                    buff.setLength(0);  // reset buffer
                    state = NULL;
                } else if (state == INPARAMPENDING) {
                    state = INPARAM;
                    buff.append('\\').append(c);
                } else { // INPARAM
                    buff.append(c);
                }
                continue;
            }

            if (c == '\\') {
                if (state == NULL) {
                    ++i;
                    if (i < slength) {
                        char cc = args.charAt(i);
                        if (cc == '\"' || cc == '\\') {
                            buff.append(cc);
                        } else if (Character.isWhitespace(cc)) { // check whitespace
                            buff.append(c);
                            --i;
                        } else {
                            buff.append(c).append(cc);
                        }
                    } else {
                        buff.append('\\');
                        break;
                    }
                    continue;
                } else if (state == INPARAM) {
                    state = INPARAMPENDING;
                } else if (state == INPARAMPENDING) {
                    buff.append('\\');
                    state = INPARAM;
                } else if (state == STICK) {
                    state = STICKPENDING;
                } else if (state == STICKPENDING) {
                    buff.append('\\');
                    state = STICK;
                }
                continue;
            }

            if (c == '\"') {
                if (state == NULL) {
                    state = INPARAM;
                } else if (state == INPARAM) {
                    state = STICK;
                } else if (state == STICK) {
                    state = INPARAM;
                } else if (state == STICKPENDING) {
                    buff.append('\"');
                    state = STICK;
                } else { // INPARAMPENDING
                    buff.append('\"');
                    state = INPARAM;
                }
                continue;
            }

            if (state == INPARAMPENDING) {
                buff.append('\\');
                state = INPARAM;
            } else if (state == STICKPENDING) {
                buff.append('\\');
                state = STICK;
            }
            buff.append(c);
        }

        // Collect remaining parameter
        if (state == INPARAM) {
            params.add(buff.toString());
        } else if ((state & (INPARAMPENDING | STICKPENDING)) != 0) {
            buff.append('\\');
            params.add(buff.toString());
        } else {
            if (buff.length() > 0) {
                params.add(buff.toString());
            }
        }

        return params;
    }

    public static void main(String[] args) {
        // Test the methods
        String javaHome = "/path/to/java/home";
        System.out.println("Java VM Full Path: " + javaVmExecutableFullPath(javaHome));
        System.out.println("Java Process Full Path: " + javaProcessExecutableFullPath(javaHome));
        System.out.println("System Property: " + systemProperty("my.property", "value"));

        String commandLineArgs = "\"C:\\Program Files\\Java\\jdk1.8.0_171\\bin\\java\" -Dproperty=value -Xmx128m";
        List<String> parsedArgs = parseParameters(commandLineArgs);
        for (String arg : parsedArgs) {
            System.out.println("Parsed Arg: " + arg);
        }
    }
}
