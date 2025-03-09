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

import fish.payara.maven.plugins.server.parser.JDKVersion;
import fish.payara.maven.plugins.server.parser.PortReader;
import static fish.payara.maven.plugins.server.Configuration.DAS_NAME;
import java.nio.file.Paths;

/**
 *
 * @author Gaurav Gupta
 */
public class PayaraServerLocalInstance extends PayaraServerInstance {

    private PortReader portReader;
    private Process logStream;

    private String path;
    private String domainName;
    private String jdkHome;

    public PayaraServerLocalInstance(String domainName, String path) {
        this.path = path;
        this.domainName = domainName;
    }

    public String getId() {
        return getDomainPath();
    }

    public String getPath() {
        return path;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getServerRoot() {
        return getPath();
    }

    public String getServerHome() {
        return Paths.get(getServerRoot(), "glassfish").toString();
    }

    public String getServerModules() {
        return Paths.get(getServerHome(), "modules").toString();
    }

    public String getDomainsFolder() {
        return Paths.get(getServerHome(), "domains").toString();
    }

    public String getDomainPath() {
        return Paths.get(getDomainsFolder(), getDomainName()).toString();
    }

    public String getDomainXmlPath() {
        return Paths.get(getDomainPath(), "config", "domain.xml").toString();
    }

    public String getServerLog() {
        return Paths.get(getDomainPath(), "logs", "server.log").toString();
    }

    public String getJDKHome() {
        if (this.jdkHome != null) {
            return this.jdkHome;
        }
        return JDKVersion.getDefaultJDKHome();
    }

    public String getProtocol() {
        return protocol == null ? "http" : protocol;
    }

    public String getHost() {
        return host == null ? "localhost" : host;
    }

    public int getHttpPort() {
        if (httpPort > 0) {
            return httpPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpPort();
    }

    public int getHttpsPort() {
        if (httpsPort > 0) {
            return httpsPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpsPort();
    }

    public int getAdminPort() {
        if (adminPort > 0) {
            return adminPort;
        }
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getAdminPort();
    }

    private PortReader createPortReader() {
        return new PortReader(getDomainXmlPath(), DAS_NAME);
    }
//
//    public void checkAliveStatusUsingJPS(Runnable callback) throws IOException {
//        String javaHome = getJDKHome();
//        if (javaHome == null) {
//            throw new IllegalStateException("Java home path not found.");
//        }
//
//        String javaProcessExe = JavaUtils.javaProcessExecutableFullPath(javaHome);
//        if (!Files.exists(Paths.get(javaProcessExe))) {
//            throw new IllegalStateException("Java Process " + javaProcessExe + " executable for " + getName() + " was not found.");
//        }
//
//        Process process = new ProcessBuilder(javaProcessExe, "-m", "-l", "-v").start();
//        List<String> lines = Files.readAllLines(process.getInputStream().toPath());
//        for (String line : lines) {
//            String[] result = line.split(" ");
//            if (result.length >= 6 && result[1].equals(ServerUtils.PF_MAIN_CLASS)
//                    && result[3].equals(getDomainName()) && result[5].equals(getDomainPath())) {
//                callback.run();
//                break;
//            }
//        }
//    }

//    public void showLog() throws IOException {
//        Files.readAllLines(Paths.get(getServerLog())).forEach(line -> getOutputChannel().appendLine(line));
//    }
//    public void connectOutput() throws IOException {
//        if (logStream == null && Files.exists(Paths.get(getServerLog()))) {
//            List<String> command = new ArrayList<>();
//            if (JavaUtils.IS_WIN) {
//                command.add("powershell.exe");
//                command.add("Get-Content");
//                command.add("-Tail");
//                command.add("20");
//                command.add("-Wait");
//                command.add("-literalpath");
//                command.add(getServerLog());
//            } else {
//                command.add("tail");
//                command.add("-f");
//                command.add("-n");
//                command.add("20");
//                command.add(getServerLog());
//            }
//
//            logStream = new ProcessBuilder(command).start();
//            logStream.getOutputStream().transferTo(getOutputChannel().getOutputStream());
//        }
//    }
    public void disconnectOutput() {
        if (logStream != null) {
            logStream.destroy();
        }
    }
}
