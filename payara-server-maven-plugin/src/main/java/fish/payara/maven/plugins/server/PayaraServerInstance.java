/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.maven.plugins.server;

import fish.payara.maven.plugins.server.parser.JDKVersion;
import fish.payara.maven.plugins.server.parser.PortReader;
import static fish.payara.maven.plugins.server.Configuration.DAS_NAME;
import java.nio.file.Paths;

/**
 *
 * @author Gaurav Gupta
 */
public class PayaraServerInstance {
    
        private PortReader portReader;
    private Process logStream;

    private String path;
    private String domainName;
    private String jdkHome;
    
    public PayaraServerInstance(String domainName, String path) {
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

//    @Override
//    public boolean isMatchingLocation(String baseRoot, String domainRoot) {
//        Path basePath = Paths.get(baseRoot, "glassfish").normalize();
//        Path domainPath = Paths.get(domainRoot).getFileName();
//        return basePath.equals(Paths.get(getPath(), "glassfish").normalize())
//                && domainRoot.endsWith(getDomainName());
//    }

    public String getHost() {
        return "localhost";
    }

    public int getHttpPort() {
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpPort();
    }

    public int getHttpsPort() {
        if (portReader == null) {
            portReader = createPortReader();
        }
        return portReader.getHttpsPort();
    }

    public int getAdminPort() {
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
