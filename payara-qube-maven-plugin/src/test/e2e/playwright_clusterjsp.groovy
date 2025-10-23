import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole

// open clusterjsp url
// check for presence of elements
// enter a value in a field and submit it
// check the result







def urlCluster = args ? args[0] : null
if (!urlCluster) {
    throw new IllegalArgumentException("URL for ClusterJsp must be provided as the first argument")
}

Playwright playwright = Playwright.create()
Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
Page page = browser.newPage()
page.navigate(urlCluster)

page.getByText("Cluster - HA JSP Sample").waitFor()

println "[OPEN] Playwright opens: $urlCluster"

page.locator("input[name='dataName']").fill("nameTest")
page.locator("input[name='dataValue']").fill("valueTest")

page.getByRole(AriaRole.BUTTON,
               new Page.GetByRoleOptions().setName("ADD SESSION DATA")).click()


// Wait for the page to load where the user authorizes the application
page.getByText("nameTest = valueTest").waitFor()

browser.close()
playwright.close()