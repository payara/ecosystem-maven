import java.nio.file.Files
import java.nio.file.Paths

// Step 1a: Delete all files and folders in $HOME/.payara
def homeDir = System.getProperty("user.home")
def payaraDir = new File(homeDir, ".payara")
if (payaraDir.exists()) {
    payaraDir.eachFileRecurse { file ->
        file.delete()
    }
}

// Step 1b: Load credentials from resource file
def credentialsFile = new File("src/test/resources/test-credentials.properties")
def props = new Properties()
credentialsFile.withInputStream { stream -> props.load(stream) }
def testUsername = props.getProperty("username")
def testPassword = props.getProperty("password")

// Step 1c: Change CLIENT_ID in Configuration.java
def pluginDir = new File("src/main/java/fish/payara/maven/plugins/cloud")
def configFile = new File(pluginDir, "Configuration.java")
def backupFile = new File(pluginDir, "Configuration.java.bak")

def configText = configFile.text
backupFile.text = configText // backup
def newConfigText = configText.replaceAll(/CLIENT_ID\s*=\s*".*?"/, 'CLIENT_ID="OPWL6h4SUxPHa1rMZ9flPStKkxnMQj8H"')
configFile.text = newConfigText

def isWin = System.getProperty("os.name").toLowerCase().contains("win")

// Step 1c: Extract version from pom.xml
def pomFile = new File("pom.xml")
def pomText = pomFile.text
def versionMatcher = pomText =~ /<version>([^<]+)<\/version>/
def pluginVersion = versionMatcher.find() ? versionMatcher.group(1) : "1.0.0-Alpha5-SNAPSHOT"
println "Detected payara-cloud-maven-plugin version: $pluginVersion"

// Step 2: Compile payara-cloud-maven-plugin
def compileCmd = isWin ?
    ["cmd", "/c", "mvn", "clean", "install"] :
    ["mvn", "clean", "install"]
def compileProc = compileCmd.execute()
compileProc.consumeProcessOutput(System.out, System.err)
if (compileProc.waitFor() != 0) throw new RuntimeException("Compile failed")


// Step 2bis: Copy dependencies to target/dependency to run Playwright scripts
def copyDepsCmd = isWin ?
    ["cmd", "/c", "mvn", "dependency:copy-dependencies", "-DoutputDirectory=target/dependency", "-Pe2e"] :
    ["mvn", "dependency:copy-dependencies", "-DoutputDirectory=target/dependency", "-Pe2e"]
def copyDepsProc = copyDepsCmd.execute()
copyDepsProc.consumeProcessOutput(System.out, System.err)
if (copyDepsProc.waitFor() != 0) throw new RuntimeException("Failed to copy dependencies")

// Step 3: Run mvn payara-cloud:login with -Dintractive=false to prevent opening the browser before playwright
def loginCmd = isWin ? ["cmd", "/c", "mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:login", "-Dintractive=false"] : ["mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:login", "-Dintractive=false"]
def loginProc = loginCmd.execute()
def loginOutput = new StringBuffer()
def loginError = new StringBuffer()

// Start a thread to consume process output
Thread.start {
    loginProc.consumeProcessOutput(loginOutput, loginError)
}

// Wait for confirmation code and URL to appear in output
def confirmationCode = null
def urlConfirmationCode = null
def timeout = System.currentTimeMillis() + 60000 // 60 seconds timeout

while (System.currentTimeMillis() < timeout && (!confirmationCode || !urlConfirmationCode)) {
    if (!confirmationCode) {
        def matcher = (loginOutput.toString() =~ /\[INFO\] Your confirmation code is (\S+)/)
        if (matcher.find()) confirmationCode = matcher.group(1)
    }
    if (!urlConfirmationCode) {
        def urlMatcher = (loginOutput.toString() =~ /\[INFO\] Opening URL: (\S+)/)
        if (urlMatcher.find()) urlConfirmationCode = urlMatcher.group(1)
    }
    Thread.sleep(500)
}
println loginOutput.toString()
if (!confirmationCode) throw new RuntimeException("Confirmation code not found in Maven output")
if (!urlConfirmationCode) throw new RuntimeException("URL not found in Maven output")

// Step 4: run Playwright script to login
def cp = isWin ? "target/dependency/*;." : "target/dependency/*:."
def playwrightCmd = isWin ?
    ["cmd", "/c", "java", "-cp", cp, "groovy.ui.GroovyMain", "src/test/e2e/playwright_login.groovy", urlConfirmationCode, confirmationCode, testUsername, testPassword] :
    ["java", "-cp", cp, "groovy.ui.GroovyMain", "src/test/e2e/playwright_login.groovy", urlConfirmationCode, confirmationCode, testUsername, testPassword]
def playwrightProc = playwrightCmd.execute()
playwrightProc.consumeProcessOutput(System.out, System.err)
if (playwrightProc.waitFor() != 0) throw new RuntimeException("Playwright login failed")

// Wait for loginProc to finish
def loginExit = loginProc.waitFor()
if (loginExit != 0) throw new RuntimeException("Login failed")

// Step 5: Deploy clusterjsp.war
def deployCmd = isWin ? ["cmd", "/c", "mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:deploy", "-DapplicationPath=src/test/resources/clusterjsp.war", "-DapplicationName=ClusterJspTest"] :
    ["mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:deploy", "-DapplicationPath=src/test/resources/clusterjsp.war", "-DapplicationName=ClusterJspTest"]
def deployProc = deployCmd.execute()
def deployOutput = new StringBuffer()
def deployError = new StringBuffer()
deployProc.consumeProcessOutput(deployOutput, deployError)
println deployOutput.toString()
if (deployProc.waitFor() != 0) throw new RuntimeException("Deploy failed")

def urlDeployment = null
def urlDeploymentMatcher = (deployOutput.toString() =~ /"applicationEndpoint":\s*"([^"]+)"/)
if (urlDeploymentMatcher.find()) urlDeployment = urlDeploymentMatcher.group(1)

println "[INFO] Deployed URL : $urlDeployment"


// Step 6: Test the deployed app with Playwright script
def playwrightClusterCmd = isWin ?
    ["cmd", "/c", "java", "-cp", cp, "groovy.ui.GroovyMain", "src/test/e2e/playwright_clusterjsp.groovy", urlDeployment] :
    ["java", "-cp", cp, "groovy.ui.GroovyMain", "src/test/e2e/playwright_clusterjsp.groovy", urlDeployment]
def playwrightClusterProc = playwrightClusterCmd.execute()
playwrightClusterProc.consumeProcessOutput(System.out, System.err)
if (playwrightClusterProc.waitFor() != 0) throw new RuntimeException("Playwright ClusterJsp failed")

// Step 7: Stop the deployed application
def stopCmd = isWin ? ["cmd", "/c", "mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:stop", "-DapplicationName=ClusterJspTest"] : ["mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:stop", "-DapplicationName=ClusterJspTest"]
def stopProc = stopCmd.execute()
stopProc.consumeProcessOutput(System.out, System.err)
if (stopProc.waitFor() != 0) throw new RuntimeException("Stop failed")

// Step 8: Undeploy clusterjsp.war
def undeployCmd = isWin ? ["cmd", "/c", "mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:undeploy", "-DapplicationName=ClusterJspTest"] : ["mvn", "fish.payara.maven.plugins:payara-cloud-maven-plugin:${pluginVersion}:undeploy", "-DapplicationName=ClusterJspTest"]
def undeployProc = undeployCmd.execute()
undeployProc.consumeProcessOutput(System.out, System.err)
if (undeployProc.waitFor() != 0) throw new RuntimeException("Undeploy failed")

// Step 9: Restore Configuration.java
configFile.text = backupFile.text
backupFile.delete()