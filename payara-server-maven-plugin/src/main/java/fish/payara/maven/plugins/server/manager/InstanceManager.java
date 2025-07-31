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
import fish.payara.maven.plugins.server.response.PlainResponse;
import fish.payara.maven.plugins.server.response.JsonResponse;
import fish.payara.maven.plugins.server.response.Response;
import static fish.payara.maven.plugins.server.manager.LocalInstanceManager.HTTP_RETRY_DELAY;
import static fish.payara.maven.plugins.server.manager.LocalInstanceManager.PARAM_ASSIGN_VALUE;
import static fish.payara.maven.plugins.server.manager.LocalInstanceManager.PARAM_SEPARATOR;
import static fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance.HTTP;
import static fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance.HTTPS;
import static fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance.HTTPS_PREFIX;
import static fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance.HTTP_PREFIX;
import static fish.payara.maven.plugins.server.manager.PayaraServerLocalInstance.LOCATION_HEADER;
import fish.payara.maven.plugins.server.utils.ServerUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Gaurav Gupta
 */
public class InstanceManager<X extends PayaraServerInstance> {

    protected Log log;

    protected X payaraServer;

    private static final String HTTP_GET_METHOD = "GET";
    private static final String HTTP_POST_METHOD = "POST";

    public static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_STREAM = "application/octet-stream";
    private static final String CONTENT_TYPE_ZIP = "application/zip";
    public static final String CONTENT_TYPE_PLAIN_TEXT = "text/plain";
    public static final String CONTENT_TYPE_HTML_TEXT = "text/html";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    private static final int MAX_RETRIES = 100;
    /**
     * Delay before administration command execution will be retried.
     */
    public static final int HTTP_RETRY_DELAY = 3000;

    private static final String DEPLOY_COMMAND = "deploy";
    private static final String UNDEPLOY_COMMAND = "undeploy";
    private static final String GET_COMMAND = "get";
    protected static final String VIEW_LOG_COMMAND = "view-log";
    protected static final String LOCATIONS_COMMAND = "__locations";
    protected static final String ASADMIN_PATH = "/__asadmin/";
    protected static final String MANAGEMENT_PATH = "/management/domain/";
    protected static final String VERSION_COMMAND = "version";

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
     * View Log command <code>start</code> parameter name.
     */
    private static final String START_PARAM = "start";

    /**
     * View Log command <code>instanceName</code> parameter name.
     */
    private static final String INSTANCE_NAME_PARAM = "instanceName";

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

    public InstanceManager(X payaraServer, Log log) {
        this.payaraServer = payaraServer;
        this.log = log;
    }

    public void connectWithServer() throws MojoExecutionException {
        boolean pingSuccess = false;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Thread.sleep(HTTP_RETRY_DELAY);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            try {
                if (pingServer()) {
                    pingSuccess = true;
                    break;
                }
            } catch (Exception e) {
                if (i == MAX_RETRIES - 1) {
                    throw new MojoExecutionException("Failed to ping Payara Server after " + MAX_RETRIES + " attempts.", e);
                }
            }
        }
        if (!pingSuccess) {
            log.error("Error pinging the server");
        }
    }

    public boolean pingServer() throws Exception {
        Command command = new Command(MANAGEMENT_PATH, VERSION_COMMAND, null);
        Response response = invokeServer(payaraServer, command);
        return response != null && response.isExitCodeSuccess();
    }

    public boolean isServerAlreadyRunning() {
        Command command = new Command(ASADMIN_PATH, LOCATIONS_COMMAND, null);
        Response serverRunning;
        try {
            serverRunning = invokeServer(payaraServer, command);
            if (serverRunning != null
                    && serverRunning.isExitCodeSuccess()) {
                log.info("Server already running on " + serverRunning.toString());
                return true;
            }
        } catch (Exception ex) {
            // skip
        }
        return false;
    }
    
    public List<String> getAsAdminEndpoints(String name) {
        return List.of(
                ASADMIN_PATH + LOCATIONS_COMMAND,
                ASADMIN_PATH+ GET_COMMAND+ "?pattern=applications.application." + name + ".context-root"
                );
    }

    public URI deployApplication(String name, String appPath, String instanceName, String contextRoot, boolean exploded, boolean hotDeploy) {
        Command command = new Command(ASADMIN_PATH, DEPLOY_COMMAND, name);
        command.setPath(appPath);
        command.setContextRoot(contextRoot);
        command.setInstanceName(instanceName);
        command.setQuery(query(command));
        command.setDirDeploy(exploded);
        command.setHotDeploy(hotDeploy);
        Response deploy;
        try {
            deploy = invokeServer(payaraServer, command);
            if (deploy != null && deploy.isExitCodeSuccess()) {
                Response response = getApplicationInfo(name);
                if (response != null && response.isExitCodeSuccess()) {
                    URI app = new URI(payaraServer.getProtocol(), null,
                            payaraServer.getHost(),
                            payaraServer.getProtocol().equals(HTTP) ? payaraServer.getHttpPort() : payaraServer.getHttpsPort(),
                            getContextRoot(((JsonResponse) response).getJsonBody()), null, null);
                    log.info(name + " application deployed successfully : " + app.toString());
                    return app;
                } else {
                    log.info(name + " application deployed successfully.");
                }
            } else if (deploy != null) {
                if (deploy.getCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                    log.error("Failed to deploy application. " + deploy.toString());
                } else {
                    log.error("Failed to deploy application. " + deploy.getHeaderFields());
                }
            } else {
                log.error("Failed to deploy application. ");
            }
        } catch (Exception ex) {
            log.error("Error deploying the application: " + ex.getMessage());
        }
        return null;
    }
    
    public Response getApplicationInfo(String name) throws Exception {
        Command command = new Command(ASADMIN_PATH, GET_COMMAND, "applications.application." + name + ".context-root");
        command.setQuery(query(command));
        return invokeServer(payaraServer, command);
    }

    public String getContextRoot(JSONObject body) {
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

    public void undeployApplication(String name, String instanceName) {
        Command command = new Command(ASADMIN_PATH, UNDEPLOY_COMMAND, name);
        command.setQuery(query(command));
        command.setInstanceName(instanceName);
        try {
            invokeServer(payaraServer, command);
        } catch (Exception ex) {
            log.error("Error undeploying the application: " + ex.getMessage());
        }
    }

    protected Response invokeServer(PayaraServerInstance instance, Command command) throws Exception {
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
                response.append('\n').append(inputLine);
            }
        }
        if(respCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String location = hconn.getHeaderField(LOCATION_HEADER);
            if (location.startsWith(HTTPS_PREFIX) && hconn.getURL().toString().startsWith(HTTP_PREFIX)) {
                URL secureUrl = new URL(location);
                URLConnection newConn = secureUrl.openConnection();
                instance.setProtocol(HTTPS);
                return handleHTTPConnection(instance, command, newConn, secureUrl);
            }
        }
        if(respCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return new PlainResponse("Unauthorized Access: " + response.toString(), respCode, hconn.getHeaderFields());
        }
        if (command.getContentType().equals(CONTENT_TYPE_PLAIN_TEXT)) {
            return new PlainResponse(response.toString(), respCode, hconn.getHeaderFields());
        }
        return new JsonResponse(response.toString(), respCode, hconn.getHeaderFields());
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
        props.put(CONTENT_TYPE_HEADER, CONTENT_TYPE_STREAM);
        props.list(new java.io.PrintStream(baos));
        return baos.toByteArray();
    }

private InputStream getInputStream(Command command) {
    if (command.isDirDeploy()) {
        return null;
    } else if (command.getPath() != null) {
        File file = new File(command.getPath());
        if (!file.exists() || !file.canRead()) {
            log.error("File not found or cannot be read: " + command.getPath());
            return null;
        }
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            log.error("Exception while opening file: " + fnfe.getMessage());
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
        conn.setConnectTimeout(server.getHttpConnectionTimeout());
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
        conn.setRequestProperty("Accept", command.getContentType());
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
        if(command.getCommand().startsWith(HTTP)) {
            return command.getCommand();
        }
        URI uri;
        try {
            uri = new URI(server.getProtocol(), null, server.getHost(), server.getAdminPort(),
                    command.getRootPath() + command.getCommand(), command.getQuery(), null);
        } catch (URISyntaxException use) {
            throw new IllegalStateException(use);
        }
        return uri.toASCIIString().replace("+", "%2b");
    }

    /**
     * Builds deploy query string for given command.
     *
     * @param command Payara server administration deploy command entity.
     * @return Deploy query string for given command.
     */
    protected static String query(Command command) {
        StringBuilder sb = new StringBuilder();
        switch (command.getCommand()) {
            case DEPLOY_COMMAND:
                sb.append(DEFAULT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getPath());
                sb.append(PARAM_SEPARATOR);
                sb.append(FORCE_PARAM).append(PARAM_ASSIGN_VALUE).append(FORCE_VALUE);
                if (command.getValue() != null && command.getValue().length() > 0) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(NAME_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getValue());
                }
                if (command.getInstanceName() != null) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(TARGET_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getInstanceName());
                }
                if (command.getContextRoot() != null && command.getContextRoot().length() > 0) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(CTXROOT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getContextRoot());
                }
                if (command.isHotDeploy()) {
                    sb.append(PARAM_SEPARATOR);
                    sb.append(HOT_DEPLOY_PARAM);
                    sb.append(PARAM_ASSIGN_VALUE).append(command.isHotDeploy());
                }
                break;
            case UNDEPLOY_COMMAND:
                sb.append(DEFAULT_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getValue());
                break;
            case GET_COMMAND:
                sb.append(PATTERN_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getValue());
                break;
            case VIEW_LOG_COMMAND:
                sb.append(START_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getValue());
                if (command.getInstanceName() != null) {
                    sb.append(INSTANCE_NAME_PARAM).append(PARAM_ASSIGN_VALUE).append(command.getInstanceName());
                }
                break;
            default:
                break;
        }

        return sb.toString();
    }

    public Response runEndpoint(String endpoint) {
        Command command = new Command("", endpoint, null);
        Response serverRunning = null;
        try {
            serverRunning = invokeServer(payaraServer, command);
            if (serverRunning != null
                    && serverRunning.isExitCodeSuccess()) {
                return serverRunning;
            }
        } catch (Exception ex) {
            // skip
        }
        return serverRunning;
    }
}
