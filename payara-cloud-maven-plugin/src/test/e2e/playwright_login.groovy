import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole

def urlConfirmationCode = args ? args[0] : null
if (!urlConfirmationCode) {
    throw new IllegalArgumentException("URL for confirmation code must be provided as the first argument")
}
def confirmationCode = args ? args[1] : null
if (!confirmationCode) {
    throw new IllegalArgumentException("Confirmation code must be provided as the second argument")
}
def testUsername = args ? args[2] : null
if (!testUsername) {
    throw new IllegalArgumentException("Test username must be provided as the third argument")
}
def testPassword = args ? args[3] : null
if (!testPassword) {
    throw new IllegalArgumentException("Test password must be provided as the fourth argument")
}

Playwright playwright = Playwright.create()
Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
Page page = browser.newPage()
page.navigate(urlConfirmationCode)

page.waitForSelector("img[alt='Payara Qube Managed']")

println "[INFO] Playwright opens : $urlConfirmationCode"

def codeValue = page.locator("input.input").inputValue()
assert codeValue == confirmationCode : "Confirmation code on page does not match expected value"

page.getByRole(AriaRole.BUTTON,
               new Page.GetByRoleOptions().setName("Confirm")).click()



// Wait for the page to load where the user enters their credentials
page.waitForSelector("input[name='username']")

page.locator("input[name='username']").fill(testUsername)
page.locator("input[name='password']").fill(testPassword)

page.getByRole(AriaRole.BUTTON,
               new Page.GetByRoleOptions().setName("Continue").setExact(true)).click()

// Wait for the page to load where the user authorizes the application
page.waitForSelector("span.success-lock")

browser.close()
playwright.close()