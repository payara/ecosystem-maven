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
package fish.payara.maven.plugins.server;

import static fish.payara.maven.plugins.server.Configuration.DAS_NAME;
import fish.payara.maven.plugins.server.parser.JvmConfigReader;
import fish.payara.maven.plugins.server.parser.JvmOption;
import fish.payara.maven.plugins.server.utils.JavaUtils;
import fish.payara.maven.plugins.server.utils.ServerUtils;
import fish.payara.maven.plugins.server.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import fish.payara.maven.plugins.server.parser.JDKVersion;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Gaurav Gupta
 */
public class ServerManager {

    private final Log log;

    private final PayaraServerInstance payaraServer;

    // Constant definitions
    private static final String HTTP_GET_METHOD = "GET";

    private static final String HTTP_POST_METHOD = "POST";

    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final String CONTENT_TYPE_ZIP = "application/zip";

    private static final String ERROR_JAVA_HOME_NOT_FOUND = "Java home path not found.";

    private static final String ERROR_JAVA_VERSION_NOT_FOUND = "Java version not found.";

    private static final String ERROR_BOOTSTRAP_JAR_NOT_FOUND = "No bootstrap jar exists.";

    private static final String ERROR_JAVA_VM_EXECUTABLE_NOT_FOUND = "Java VM executable for %s was not found.";

    
    /**
     * Delay before administration command execution will be retried.
     */
    public static final int HTTP_RETRY_DELAY = 3000;
    
    /**
     * Socket connection timeout (in miliseconds).
     */
    public static final int HTTP_CONNECTION_TIMEOUT = 3000;
    /**
     * Socket read timeout (in miliseconds).
     */
    public static final int HTTP_READ_TIMEOUT = 3000;

    /**
     * Character used to separate individual parameters.
     */
    static final char PARAM_SEPARATOR = '&';

    /**
     * Character used to assign value to parameter.
     */
    static final char PARAM_ASSIGN_VALUE = '=';

    /**
     * Deploy command <code>DEFAULT</code> parameter name.
     */
    private static final String DEFAULT_PARAM = "DEFAULT";

    /**
     * Deploy command <code>target</code> parameter name.
     */
    private static final String TARGET_PARAM = "target";

    /**
     * Deploy command <code>name</code> parameter name.
     */
    private static final String NAME_PARAM = "name";
    /**
     * Get command <code>pattern</code> parameter name.
     */
    private static final String PATTERN_PARAM = "pattern";

    /**
     * Deploy command <code>contextroot</code> parameter name.
     */
    private static final String CTXROOT_PARAM = "contextroot";

    /**
     * Deploy command <code>force</code> parameter name.
     */
    private static final String FORCE_PARAM = "force";

    /**
     * Deploy command <code>properties</code> parameter name.
     */
    private static final String PROPERTIES_PARAM = "properties";

    /**
     * Deploy command <code>libraries</code> parameter name.
     */
    private static final String LIBRARIES_PARAM = "libraries";

    /**
     * Deploy command <code>hotDeploy</code> parameter name.
     */
    private static final String HOT_DEPLOY_PARAM = "hotDeploy";

    /**
     * Deploy command <code>force</code> parameter value.
     */
    private static final boolean FORCE_VALUE = true;

    public ServerManager(PayaraServerInstance payaraServer, Log log) {
        this.log = log;
        this.payaraServer = payaraServer;
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

    private void addJavaAgent(PayaraServerInstance payaraServer, JvmConfigReader jvmConfigReader) throws Exception {
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

    private Map<String, String> varMap(PayaraServerInstance payaraServer, String javaHome) {
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

    private List<String> getPayaraArgs(PayaraServerInstance payaraServer) {
        List<String> payaraArgs = new ArrayList<>();
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_ARG, payaraServer.getDomainName()));
        payaraArgs.add(ServerUtils.cmdLineArgument(ServerUtils.PF_DOMAIN_DIR_ARG, StringUtils.quote(payaraServer.getDomainPath())));
        return payaraArgs;
    }

    private String appendPayaraArgs(List<String> payaraArgsList) {
        return String.join(" ", payaraArgsList).trim();
    }

    public void connectWithServer() throws MojoExecutionException {
        int retries = 20;
        boolean pingSuccess = false;
        for (int i = 0; i < retries; i++) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
            try {
                if (pingServer()) {
                    pingSuccess = true;
                    break;
                }
            } catch (Exception e) {
                if (i == retries - 1) {
                    throw new MojoExecutionException("Failed to ping Payara Server after " + retries + " attempts.", e);
                }
            }
        }
        if (!pingSuccess) {
            log.error("Error pinging the server");
        }
    }

    public boolean pingServer() throws Exception {
        URI uri = new URI(payaraServer.getProtocol(), null, payaraServer.getHost(), payaraServer.getAdminPort(),
                            "management/domain/version", null, null);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(HTTP_GET_METHOD);
        connection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
        connection.setReadTimeout(HTTP_READ_TIMEOUT);
        int responseCode = connection.getResponseCode();
        return (responseCode == HttpURLConnection.HTTP_OK);
    }

    public boolean isServerAlreadyRunning() {
        Command command = new Command("__locations", null);
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
            log.error(ex);
        }
        return false;
    }
        
    public void deployApplication(String name, String appPath) {
        Command command = new Command("deploy", name);
        command.setPath(appPath);
        command.setQuery(query(command));
        Response deploy;
        try {
            deploy = invokeServer(payaraServer, command);
            if (deploy != null && deploy.isExitCodeSuccess()) {
                log.info(name + " application deployed successfully.");
                command = new Command("get", "applications.application." + name + ".context-root");
                command.setQuery(query(command));
                Response contextRoot = invokeServer(payaraServer, command);
                if (contextRoot != null && contextRoot.isExitCodeSuccess()) {
                    URI app = new URI(payaraServer.getProtocol(), null, payaraServer.getHost(), payaraServer.getHttpPort(),
                            contextRoot.getContextRoot(), null, null);
                    log.info(name + " application available on : " + app.toString());
                }
            } else {
                log.error("Failed to deploy application.");
            }
        } catch (Exception ex) {
            log.error("Error deploying the application: " + ex.getMessage());
        }
    }
    
    
    public void undeployApplication(String name) {
        Command command = new Command("undeploy", name);
        command.setQuery(query(command));
        try {
            invokeServer(payaraServer, command);
        } catch (Exception ex) {
            log.error("Error undeploying the application: " + ex.getMessage());
        }
    }


    /**
     * Builds deploy query string for given command.
     * 
     * @param command Payara server administration deploy command entity.
     * @return Deploy query string for given command.
     */
    private static String query(Command command) {
        StringBuilder sb = new StringBuilder();
        switch (command.getCommand()) {
            case "deploy":
                sb.append(DEFAULT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getPath());
                sb.append(PARAM_SEPARATOR);
                sb.append(FORCE_PARAM).append(PARAM_ASSIGN_VALUE).append(FORCE_VALUE);
                if (command.getName() != null && command.getName().length() > 0) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(NAME_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getName());
                }   if (command.getTarget() != null) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(TARGET_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getTarget());
                }   if (command.getContextRoot() != null && command.getContextRoot().length() > 0) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(CTXROOT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getContextRoot());
                }   if (command.isHotDeploy()) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(HOT_DEPLOY_PARAM);
                    sb.append(PARAM_ASSIGN_VALUE).append(command.isHotDeploy());
                }   break;
            case "undeploy":
                sb.append(DEFAULT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getName());
                break;
            case "get":
                sb.append(PATTERN_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getName());
                break;
            default:
                break;
        }

        return sb.toString();
    }

    private Response invokeServer(PayaraServerInstance instance, Command command) throws Exception {
        boolean httpSucceeded = false;
        String commandUrl = constructCommandUrl(instance, command);
        int retries = 1;
        Response response = null;
        try {
            URL urlToConnectTo = new URL(commandUrl);
            while (!httpSucceeded && retries-- > 0) {
                HttpURLConnection hconn = null;
                try {
                    URLConnection conn = openURLConnection(urlToConnectTo);
                    if (conn instanceof HttpURLConnection) {
                        hconn = (HttpURLConnection) conn;
                        response = handleHTTPConnection(instance, command, conn, urlToConnectTo);
                        httpSucceeded = true;
                    }
                } catch (ProtocolException | ConnectException | RuntimeException ex) {
                    log.error(ex);
                    return response;
                } catch (IOException ex) {
                    if (retries <= 0) {
                        return response;
                    }
                } finally {
                    if (null != hconn) {
                        if (hconn.getInputStream() != null) {
                            try {
                                hconn.getInputStream().close();
                            } catch (IOException ioe) {
                            }
                        }
                        hconn.disconnect();
                    }
                }

                if (!httpSucceeded && retries > 0) {
                    try {
                        Thread.sleep(HTTP_RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        log.info("sleepInterrupted", ie);
                    }
                }
            }
        } catch (MalformedURLException ex) {
            log.warn(ex);
        }
        return response;
    }

    /**
     * Creates {@link URLConnection} instance that represents a connection to
     * Payara server administration interface.
     *
     * @param urlToConnectTo Payara server administration interface URL.
     * @return server administration interface URL connection.
     * @throws IOException IOException if an I/O error occurs while opening the
     * connection.
     */
    private static URLConnection openURLConnection(
            final URL urlToConnectTo) throws IOException {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(urlToConnectTo.getHost());
        } catch (UnknownHostException ex) {
            addr = null;
        }
        if (addr != null && addr.isLoopbackAddress()) {
            return urlToConnectTo.openConnection(Proxy.NO_PROXY);
        }
        return urlToConnectTo.openConnection();
    }

    /**
     * Handle HTTP connections to server.
     *
     * @return State change request when <code>call()</code> method should exit.
     */
    private Response handleHTTPConnection(PayaraServerInstance instance, Command command, URLConnection conn, URL urlToConnectTo) throws IOException {
        HttpURLConnection hconn = (HttpURLConnection) conn;
        if (conn instanceof HttpsURLConnection) {
            handleSecureConnection((HttpsURLConnection) conn);
        }
        prepareHttpConnection(instance, command, hconn);
        // Connect to server.
        hconn.connect();
        // Send data to server if necessary.
        handleSend(command, hconn);
        int respCode = hconn.getResponseCode();
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(hconn.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return new Response(response.toString(), respCode, hconn.getHeaderFields());
    }

    private void handleSend(Command command, HttpURLConnection hconn) throws IOException {
        InputStream istream = getInputStream(command);
        if (istream != null) {
            ZipOutputStream ostream = null;
            try {
                File file = new File(command.getPath());
                ostream = new ZipOutputStream(new BufferedOutputStream(
                        hconn.getOutputStream(), 1024 * 1024));
                ZipEntry e = new ZipEntry(file.getName());
                e.setExtra(getExtraProperties(file));
                ostream.putNextEntry(e);
                byte[] buffer = new byte[1024 * 1024];
                while (true) {
                    int n = istream.read(buffer);
                    if (n < 0) {
                        break;
                    }
                    ostream.write(buffer, 0, n);
                }
                ostream.closeEntry();
                ostream.flush();
            } finally {
                try {
                    istream.close();
                } catch (IOException ex) {
                    log.error(ex);
                }
                if (ostream != null) {
                    try {
                        ostream.close();
                    } catch (IOException ex) {
                    log.error(ex);
                    }
                }
            }
        }
    }
    
    /**
     * Get extra properties for ZIP entries.
     * <p/>
     * @return Extra properties for ZIP entries.
     */
    private byte[] getExtraProperties(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Properties props = new Properties();
        props.setProperty("data-request-type", "file-xfer");
        props.setProperty("last-modified", Long.toString(file.lastModified()));
        props.put("data-request-name", "DEFAULT");
        props.put("data-request-is-recursive", "true");
        props.put("Content-Type", "application/octet-stream");
        props.list(new java.io.PrintStream(baos));
        return baos.toByteArray();
    }

    private InputStream getInputStream(Command command) {
        if (command.isDirDeploy()) {
            return null;
        } else if (command.getPath() != null) {
            try {
                return new FileInputStream(command.getPath());
            } catch (FileNotFoundException fnfe) {
                log.error(fnfe);
                return null;
            }
        }
        return null;
    }


    /**
     * Prepare headers for HTTP connection. This handles all common headers for
     * all implemented command interfaces (REST, HTTP, ...).
     *
     * @param conn Target HTTP connection.
     * @throws IllegalStateException if there is a problem with setting the
     * headers.
     */
    private void prepareHttpConnection(PayaraServerInstance server, Command command, HttpURLConnection conn)
            throws IllegalStateException {
        conn.setAllowUserInteraction(false);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
        String adminUser = server.getAdminUser();
        String adminPassword = server.getAdminPassword();
        try {
            conn.setRequestMethod(command.isDirDeploy() || command.getPath() == null ? HTTP_GET_METHOD : HTTP_POST_METHOD);
        } catch (ProtocolException pe) {
            throw new IllegalStateException(pe);
        }
        conn.setDoOutput(!command.isDirDeploy());
        String contentType = command.isDirDeploy() || command.getPath() == null ? null : CONTENT_TYPE_ZIP;
        if (contentType != null && contentType.length() > 0) {
            conn.setRequestProperty("Content-Type", contentType);
            conn.setChunkedStreamingMode(0);
        }
        conn.setRequestProperty("Accept", CONTENT_TYPE_JSON);
        if (adminPassword != null && adminPassword.length() > 0) {
            String authString = ServerUtils.basicAuthCredentials(
                    adminUser, adminPassword);
            conn.setRequestProperty("Authorization", "Basic " + authString);
        }
    }

    private void handleSecureConnection(final HttpsURLConnection conn) {
        TrustManager[] tm = new TrustManager[]{
            new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] arg0,
                        String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0,
                        String arg1) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }
        };

        SSLContext context;
        try {
            context = SSLContext.getInstance("SSL");
            context.init(null, tm, null);
            conn.setSSLSocketFactory(context.getSocketFactory());
            conn.setHostnameVerifier((String string, SSLSession ssls) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            log.error(conn.getURL().toString(), ex);
        }
    }

    private String constructCommandUrl(PayaraServerInstance server, Command command) throws IllegalStateException {
        String path = "/__asadmin/";
        URI uri;
        try {
            uri = new URI(server.getProtocol(), null, server.getHost(), server.getAdminPort(),
                    path + command.getCommand(), command.getQuery(), null);
        } catch (URISyntaxException use) {
            throw new IllegalStateException(use);
        }
        return uri.toASCIIString().replace("+", "%2b");
    }

}

class Command {

    private final String name;
    private final String command;
    private String path;
    private String query;
    private boolean dirDeploy;
    private String target;
    private String contextRoot;
    private boolean hotDeploy;

    public Command(String command, String name) {
        this.command = command;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getCommand() {
        return command;
    }

    public String getQuery() {
        return query;
    }

    public boolean isDirDeploy() {
        return dirDeploy;
    }

    public void setDirDeploy(boolean dirDeploy) {
        this.dirDeploy = dirDeploy;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTarget() {
        return target;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public boolean isHotDeploy() {
        return hotDeploy;
    }

}

class Response {
    private final JSONObject body;
    private final Map<String, List<String>> headerFields;
    private final int code;

    public Response(String jsonString, int code, Map<String, List<String>> headerFields) {
        this.body = new JSONObject(jsonString);
        this.headerFields = headerFields;
        this.code = code;
    }

    public String getContextRoot() {
        JSONArray resultArray = body.getJSONArray("result");
        if (resultArray.length() > 0) {
            String nameValue = resultArray.getJSONObject(0).getString("name");
            String[] parts = nameValue.split("context-root=");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    public boolean isExitCodeSuccess() {
        return "SUCCESS".equalsIgnoreCase(body.getString("exit_code"));
    }
    
    @Override
    public String toString() {
        return body.getString("name") != null ? body.getString("name") : body.toString();
    }

    public JSONObject getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    public int getCode() {
        return code;
    }

}
