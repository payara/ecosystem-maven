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

import fish.payara.maven.plugins.server.Command;
import fish.payara.maven.plugins.server.response.Response;
import static fish.payara.maven.plugins.server.Configuration.DAS_NAME;
import fish.payara.maven.plugins.server.parser.JvmConfigReader;
import fish.payara.maven.plugins.server.parser.JvmOption;
import fish.payara.maven.plugins.server.utils.JavaUtils;
import fish.payara.maven.plugins.server.utils.ServerUtils;
import fish.payara.maven.plugins.server.utils.StringUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import fish.payara.maven.plugins.server.parser.JDKVersion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 *
 * @author Gaurav Gupta
 */
public class LocalInstanceManager extends InstanceManager<PayaraServerLocalInstance> {

    private static final String ERROR_JAVA_HOME_NOT_FOUND = "Java home path not found.";
    private static final String ERROR_JAVA_VERSION_NOT_FOUND = "Java version not found.";
    private static final String ERROR_BOOTSTRAP_JAR_NOT_FOUND = "No bootstrap jar exists.";
    private static final String ERROR_JAVA_VM_EXECUTABLE_NOT_FOUND = "Java VM executable for %s was not found.";

    public LocalInstanceManager(PayaraServerLocalInstance payaraServer, Log log) {
        super(payaraServer, log);
    }

    public ProcessBuilder startServer(String debug, String debugPort) throws Exception {
        JvmConfigReader jvmConfigReader = new JvmConfigReader(payaraServer.getDomainXmlPath(), DAS_NAME);
        String javaHome = payaraServer.getJDKHome();
        if (javaHome == null) {
            throw new Exception(ERROR_JAVA_HOME_NOT_FOUND);
        }
        JDKVersion javaVersion = JDKVersion.getJDKVersion(javaHome);
        if (javaVersion == null) {
            throw new Exception(ERROR_JAVA_VERSION_NOT_FOUND);
        }
        List<String> optList = new ArrayList<>();
        for (JvmOption jvmOption : jvmConfigReader.getJvmOptions()) {
            if (JDKVersion.isCorrectJDK(javaVersion, jvmOption.getVendor(), jvmOption.getMinVersion(), jvmOption.getMaxVersion())) {
                optList.add(jvmOption.getOption());
            }
        }
        Map<String, String> propMap = jvmConfigReader.getPropMap();
        addJavaAgent(payaraServer, jvmConfigReader);
        String bootstrapJar = Paths.get(payaraServer.getServerModules(), "glassfish.jar").toString();
        if (!Files.exists(Paths.get(bootstrapJar))) {
            throw new Exception(ERROR_BOOTSTRAP_JAR_NOT_FOUND);
        }
        String classPath = "";
        String javaOpts;
        String payaraArgs;
        Map<String, String> varMap = varMap(payaraServer, javaHome);
        String debugOpt = propMap.get("debug-options");
        if (debug != null && !debug.equalsIgnoreCase(Boolean.FALSE.toString()) && debugOpt != null) {
            if (Boolean.parseBoolean(debug)) {
                if (isValidPort(debugPort)) {
                    debugOpt = debugOpt.replaceAll("address=\\d+", "address=" + debugPort);
                }
                optList.add(debugOpt);
            } else if (!Boolean.FALSE.toString().equals(debug)) {
                optList.add(debug);
            }
            optList.add(debugOpt);
        }
        javaOpts = appendOptions(optList, varMap);
        javaOpts += appendVarMap(varMap);
        payaraArgs = appendPayaraArgs(getPayaraArgs(payaraServer));
        String javaVmExe = JavaUtils.javaVmExecutableFullPath(javaHome);
        if (!Files.exists(Paths.get(javaVmExe))) {
            throw new Exception(String.format(ERROR_JAVA_VM_EXECUTABLE_NOT_FOUND, payaraServer.getPath()));
        }
        String allArgs = String.join(" ", javaVmExe, javaOpts, "-jar", bootstrapJar, "--classpath", classPath, payaraArgs);
        List<String> args = JavaUtils.parseParameters(allArgs);
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(new File(payaraServer.getPath()));
        return processBuilder;
    }

    private boolean isValidPort(String portStr) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return false;
        }
        try {
            int port = Integer.parseInt(portStr.trim());
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addJavaAgent(PayaraServerLocalInstance payaraServer, JvmConfigReader jvmConfigReader) throws Exception {
        List<JvmOption> optList = jvmConfigReader.getJvmOptions();
        String serverHome = payaraServer.getServerHome();

        File monitor = Paths.get(serverHome, "lib", "monitor").toFile();
        File btrace = Paths.get(monitor.getPath(), "btrace-agent.jar").toFile();
        File flight = Paths.get(monitor.getPath(), "flashlight-agent.jar").toFile();

        if (jvmConfigReader.isMonitoringEnabled()) {
            if (btrace.exists()) {
                optList.add(new JvmOption("-javaagent:" + StringUtils.quote(btrace.getPath()) + "=unsafe=true,noServer=true"));
            } else if (flight.exists()) {
                optList.add(new JvmOption("-javaagent:" + StringUtils.quote(flight.getPath())));
            }
        }
    }

    private Map<String, String> varMap(PayaraServerLocalInstance payaraServer, String javaHome) {
        Map<String, String> varMap = new HashMap<>();
        varMap.put(ServerUtils.PF_HOME_PROPERTY, payaraServer.getServerHome());
        varMap.put(ServerUtils.PF_DOMAIN_ROOT_PROPERTY, payaraServer.getDomainPath());
        varMap.put(ServerUtils.PF_JAVA_ROOT_PROPERTY, javaHome);
        varMap.put(JavaUtils.PATH_SEPARATOR, File.pathSeparator);
        return varMap;
    }

    private String appendOptions(List<String> optList, Map<String, String> varMap) {
        StringBuilder argumentBuf = new StringBuilder();
        List<String> moduleOptions = new ArrayList<>();
        Map<String, String> keyValueArgs = new HashMap<>();
        List<String> keyOrder = new ArrayList<>();

        for (String opt : optList) {
            opt = StringUtils.doSub(opt.trim(), varMap);
            int splitIndex = opt.indexOf('=');
            String name, value = null;
            if (splitIndex != -1 && !opt.startsWith("-agentpath:")) {
                name = opt.substring(0, splitIndex);
                value = StringUtils.quote(opt.substring(splitIndex + 1));
            } else {
                name = opt;
            }

            if (name.startsWith("--add-")) {
                moduleOptions.add(opt);
            } else {
                if (!keyValueArgs.containsKey(name)) {
                    keyOrder.add(name);
                }
                keyValueArgs.put(name, value);
            }
        }

        argumentBuf.append(String.join(" ", moduleOptions));
        for (String key : keyOrder) {
            argumentBuf.append(" ").append(key);
            if (keyValueArgs.get(key) != null) {
                argumentBuf.append("=").append(keyValueArgs.get(key));
            }
        }
        return argumentBuf.toString();
    }

    private String appendVarMap(Map<String, String> varMap) {
        StringBuilder javaOpts = new StringBuilder();
        varMap.forEach((key, value) -> javaOpts.append(" ").append(JavaUtils.systemProperty(key, value)));
        return javaOpts.toString();
    }

    private List<String> getPayaraArgs(PayaraServerLocalInstance payaraServer) {
        List<String> payaraArgs = new ArrayList<>();
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_ARG, payaraServer.getDomainName()));
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_DIR_ARG, StringUtils.quote(payaraServer.getDomainPath())));
        return payaraArgs;
    }

    private String appendPayaraArgs(List<String> payaraArgsList) {
        return String.join(" ", payaraArgsList).trim();
    }

    public boolean isServerAlreadyRunning() {
        Command command = new Command(ASADMIN_PATH, LOCATIONS_COMMAND, null);
        Response serverRunning;
        try {
            serverRunning = invokeServer(payaraServer, command);
            if (serverRunning != null
                    && serverRunning.isExitCodeSuccess()
                    && serverRunning.toString().equals(payaraServer.getDomainPath())) {
                log.info("Server already running on " + serverRunning.toString());
                return true;
            }
        } catch (Exception ex) {
            // skip
        }
        return false;
    }

    public String runAsadminCommand(String command) throws Exception {
        String javaHome = payaraServer.getJDKHome();
        if (javaHome == null) {
            throw new Exception(ERROR_JAVA_HOME_NOT_FOUND);
        }

        String javaVmExe = JavaUtils.javaVmExecutableFullPath(javaHome);
        if (!Files.exists(Paths.get(javaVmExe))) {
            throw new Exception(String.format(ERROR_JAVA_VM_EXECUTABLE_NOT_FOUND, payaraServer.getPath()));
        }

        String asadminPath = Paths.get(payaraServer.getServerHome(), "bin", "asadmin").toString();
        if (!Files.exists(Paths.get(asadminPath))) {
            throw new Exception("asadmin executable not found at " + asadminPath);
        }

        List<String> commandList = new ArrayList<>();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            commandList.add("cmd.exe");
            commandList.add("/c");
        } else {
            commandList.add("/bin/sh");
            commandList.add("-c");
        }

        commandList.add(asadminPath);
        commandList.addAll(Arrays.asList(command.split("\\s+")));

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(Paths.get(payaraServer.getServerHome(), "bin").toFile());

        Process process = processBuilder.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("Error reading process output", e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading process error output", e);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("asadmin command failed with exit code " + exitCode);
        }
        return sb.toString();
    }

}
