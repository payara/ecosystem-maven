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

/**
 *
 * @author Gaurav Gupta
 */
public class TempDirectoryResolver {

    public static File resolvePayaraTempDir(String payaraServerVersion) {
        File[] baseCandidates = new File[] {
            new File(System.getProperty("java.io.tmpdir")),
            new File(System.getProperty("user.home"), ".payara-tmp"),
            getGlobalTempFallback()
        };

        for (File base : baseCandidates) {
            File versioned = tryCreateVersionedDir(base, "payara-server-" + payaraServerVersion);
            if (versioned != null) {
                return versioned;
            }
        }

        throw new RuntimeException("No writable temp directory could be created.");
    }

    private static File tryCreateVersionedDir(File baseDir, String versionedName) {
        System.out.println("base dir " + baseDir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return null;
        }

        if (baseDir.canWrite()) {
            File versionedDir = new File(baseDir, versionedName);
            if (!versionedDir.exists() && !versionedDir.mkdirs()) {
                return null;
            }
            return versionedDir;
        }

        return null;
    }

    private static File getGlobalTempFallback() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String winTmp = System.getenv("TEMP");
            if (winTmp != null && !winTmp.isEmpty()) {
                return new File(winTmp, "payara-global-tmp");
            }
            return new File("C:\\Temp\\payara-global-tmp");
        } else {
            return new File("/tmp/payara-global-tmp");
        }
    }
}
