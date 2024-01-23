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

/**
 *
 * @author Gaurav Gupta
 */
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import org.apache.maven.plugin.logging.Log;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;

public class WebDriverFactory {

    public static WebDriver createWebDriver(String browser) {
        WebDriver driver = null;
        if (browser == null) {
            browser = getDefaultBrowser();
        }

        switch (browser.toLowerCase()) {
            case "chrome": {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--enable-notifications");
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(options);
                break;
            }
            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver(options);
                break;
            }
            case "edge": {
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;
            }
            case "ie": {
                InternetExplorerOptions options = new InternetExplorerOptions();
                WebDriverManager.iedriver().setup();
                driver = new InternetExplorerDriver(options);
                break;
            }
            case "safari": {
                driver = new SafariDriver();
                break;
            }
            default:
                throw new UnsupportedOperationException("Unsupported browser: " + browser);
        }

        return driver;
    }

    public static String getDefaultBrowser() {
        if (isChromeBrowserInstalled()) {
            return "chrome";
        } else if (isFirefoxBrowserInstalled()) {
            return "firefox";
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");
            boolean isMac = os.contains("mac");

            if (isWindows) {
                return "edge";
            } else if (isMac) {
                return "safari";
            } else {
                return "firefox";
            }
        }
    }

    public static boolean isFirefoxBrowserInstalled() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac");

        if (isWindows) {
            String programFiles = System.getenv("ProgramFiles");
            String firefoxPath = programFiles + "\\Mozilla Firefox\\firefox.exe";
            return new File(firefoxPath).exists();
        } else if (isMac) {
            String macFirefoxPath = "/Applications/Firefox.app/Contents/MacOS/firefox";
            return new File(macFirefoxPath).exists();
        } else {
            // Assuming Linux or Unix-based system where Firefox is typically installed in the path
            String[] searchPaths = {"/usr/bin/firefox", "/usr/bin/firefox-esr", "/usr/bin/firefox-bin"};
            for (String path : searchPaths) {
                if (new File(path).exists()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isChromeBrowserInstalled() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac");

        if (isWindows) {
            String programFiles = System.getenv("ProgramFiles");
            String chromePath = programFiles + "\\Google\\Chrome\\Application\\chrome.exe";
            return new File(chromePath).exists();
        } else if (isMac) {
            String macChromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            return new File(macChromePath).exists();
        } else {
            // Assuming Linux or Unix-based system where Chrome is typically installed in the path
            String[] searchPaths = {"/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser"};
            for (String path : searchPaths) {
                if (new File(path).exists()) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public static void executeScript(String script, WebDriver driver, Log log) {
        if (driver != null && driver instanceof JavascriptExecutor) {
            try {
                ((JavascriptExecutor) driver).executeScript(script);
            } catch(WebDriverException ex) {
                log.debug(ex);
            }
        }
    }
}
