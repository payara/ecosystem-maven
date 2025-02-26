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

import fish.payara.maven.plugins.server.utils.JavaUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class JDKVersion {

    private int major;
    private Integer minor;
    private Integer subminor;
    private Integer update;
    private String vendor;

    private static final int MAJOR_INDEX = 0;
    private static final int MINOR_INDEX = 1;
    private static final int SUBMINOR_INDEX = 2;
    private static final int UPDATE_INDEX = 3;
    private static final int DEFAULT_VALUE = 0;
    private static final String VERSION_MATCHER = "(\\d+(\\.\\d+)*)([_u\\-]+[\\S]+)*";

    public JDKVersion(int major, Integer minor, Integer subminor, Integer update, String vendor) {
        this.major = major;
        this.minor = minor;
        this.subminor = subminor;
        this.update = update;
        this.vendor = vendor;
    }

    // Getters
    public int getMajor() {
        return major;
    }

    public Integer getMinor() {
        return minor;
    }

    public Integer getSubMinor() {
        return subminor;
    }

    public Integer getUpdate() {
        return update;
    }

    public String getVendor() {
        return vendor;
    }

    public boolean gt(JDKVersion version) {
        if (this.major > version.getMajor()) {
            return true;
        } else if (this.major == version.getMajor()) {
            if (gtNumber(this.minor, version.getMinor())) {
                return true;
            } else if (eq(this.minor, version.getMinor())) {
                if (gtNumber(this.subminor, version.getSubMinor())) {
                    return true;
                } else if (eq(this.subminor, version.getSubMinor())) {
                    return gtNumber(this.update, version.getUpdate());
                }
            }
        }
        return false;
    }

    public boolean lt(JDKVersion version) {
        if (this.major < version.getMajor()) {
            return true;
        } else if (this.major == version.getMajor()) {
            if (ltNumber(this.minor, version.getMinor())) {
                return true;
            } else if (eq(this.minor, version.getMinor())) {
                if (ltNumber(this.subminor, version.getSubMinor())) {
                    return true;
                } else if (eq(this.subminor, version.getSubMinor())) {
                    return ltNumber(this.update, version.getUpdate());
                }
            }
        }
        return false;
    }

    public boolean ge(JDKVersion version) {
        return this.gt(version) || this.equals(version);
    }

    public boolean le(JDKVersion version) {
        return this.lt(version) || this.equals(version);
    }

    private boolean gtNumber(Integer v1, Integer v2) {
        if (v1 == null) {
            v1 = DEFAULT_VALUE;
        }
        if (v2 == null) {
            v2 = DEFAULT_VALUE;
        }
        return v1 > v2;
    }

    private boolean ltNumber(Integer v1, Integer v2) {
        if (v1 == null) {
            v1 = DEFAULT_VALUE;
        }
        if (v2 == null) {
            v2 = DEFAULT_VALUE;
        }
        return v1 < v2;
    }

    private boolean eq(Integer v1, Integer v2) {
        if (v1 == null) {
            v1 = DEFAULT_VALUE;
        }
        if (v2 == null) {
            v2 = DEFAULT_VALUE;
        }
        return v1.equals(v2);
    }

    public boolean equals(JDKVersion other) {
        if (other == null) {
            return false;
        }
        return this.major == other.getMajor()
                && eq(this.minor, other.getMinor())
                && eq(this.subminor, other.getSubMinor())
                && eq(this.update, other.getUpdate());
    }

    @Override
    public String toString() {
        StringBuilder value = new StringBuilder(Integer.toString(major));
        if (minor != null) {
            value.append(minor);
        }
        if (subminor != null) {
            value.append(subminor);
        }
        if (update != null) {
            value.append(update);
        }
        return value.toString();
    }

    public static JDKVersion toValue(String version, String vendor) {
        if (version != null && !version.isEmpty()) {
            int[] versions = parseVersions(version);
            int major = versions[MAJOR_INDEX];
            Integer minor = versions[MINOR_INDEX];
            Integer subminor = versions[SUBMINOR_INDEX];
            Integer update = versions[UPDATE_INDEX];
            return new JDKVersion(major, minor, subminor, update, vendor);
        }
        return null;
    }

    public static int[] parseVersions(String javaVersion) {
        int[] versions = {1, 0, 0, 0};
        if (javaVersion != null && !javaVersion.isEmpty()) {
            String[] javaVersionSplit = javaVersion.split("-");
            String[] split = javaVersionSplit[0].split("\\.");
            if (split.length > 0) {
                versions[MAJOR_INDEX] = Integer.parseInt(split[0]);
                if (split.length > 1) {
                    versions[MINOR_INDEX] = Integer.parseInt(split[1]);
                }
                if (split.length > 2) {
                    String[] subSplit = split[2].split("[_u]+");
                    versions[SUBMINOR_INDEX] = Integer.parseInt(subSplit[0]);
                    if (subSplit.length > 1) {
                        versions[UPDATE_INDEX] = Integer.parseInt(subSplit[1]);
                    }
                }
            }
        }
        return versions;
    }

    // Example method to get default JDK path (similar to original JavaScript functionality)
    public static String getDefaultJDKHome() {
        String javaHome = System.getenv("JDK_HOME");
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }
        return javaHome;
    }

    public static JDKVersion getJDKVersion(String javaHome) throws IOException {
        String javaVersion = "";
        String implementor = null;

        String javaVmExe = JavaUtils.javaVmExecutableFullPath(javaHome);
        // Check if the Java executable exists.
        if (!new File(javaVmExe).exists()) {
            throw new IOException("Java VM executable not found at: " + javaVmExe);
        }

        // Run the command to get Java properties and version.
        Process process = new ProcessBuilder(javaVmExe, "-XshowSettings:properties", "-version")
                .redirectErrorStream(true)
                .start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("java.version =")) {
                javaVersion = extractValue(line);
            } else if (line.contains("java.vendor =")) {
                implementor = extractValue(line);
            }
        }

        reader.close();

        if (!javaVersion.isEmpty()) {
            return JDKVersion.toValue(javaVersion, implementor);
        }

        return null;
    }

    private static String extractValue(String line) {
        String[] keyValue = line.split("=");
        if (keyValue.length == 2) {
            return keyValue[1].trim();
        }
        return "";
    }

    public static boolean isCorrectJDK(JDKVersion jdkVersion, String vendor, JDKVersion minVersion, JDKVersion maxVersion) {
        boolean correctJDK = true;
        if (vendor != null) {
            String jdkVendor = jdkVersion.getVendor();
            if (jdkVendor != null && !jdkVendor.contains(vendor)) {
                correctJDK = false;
            }
        }
        if (correctJDK && minVersion != null) {
            correctJDK = jdkVersion.ge(minVersion);
        }
        if (correctJDK && maxVersion != null) {
            correctJDK = jdkVersion.le(maxVersion);
        }
        return correctJDK;
    }
}
