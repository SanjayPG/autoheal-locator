# User Guide

This guide covers the essential concepts and usage patterns for AutoHeal Locator, from basic setup to advanced configurations.

## Overview

AutoHeal Locator provides AI-powered element location with auto-healing capabilities for web test automation. When UI changes break your selectors, AutoHeal automatically finds and fixes them using AI analysis.

### Core Concepts

- **AI-Powered Healing**: When selectors fail, AI analyzes the page to find the correct elements
- **Multiple Strategies**: DOM analysis, visual analysis, and hybrid approaches
- **Intelligent Caching**: High-performance caching reduces costs and improves speed
- **Circuit Breaker**: Resilient design with fallback mechanisms for reliability

### Real-World Example

```java
// Before AutoHeal - Breaks when DOM changes
WebElement loginButton = driver.findElement(By.id("login-btn-v2"));

// With AutoHeal - Any locator type works and self-heals
WebElement loginButton = autoHeal.findElement("login-btn-v2", "login button on homepage");
// ✅ Works with CSS (#login-btn), XPath (//button[@id='login']), ID, Name, Link Text, etc.
// ✅ Automatically detects locator type - no need to specify By.id() or By.xpath()
// ✅ AI understands "login button" context and finds the right element
// ✅ Smart disambiguation when multiple elements match the same locator
```

### Universal Locator Support - Use Any Selector Type

AutoHeal automatically detects and works with all major locator types without requiring explicit `By` objects:

```java
// ✅ All these work the same way - AutoHeal detects the type automatically
autoHeal.findElement("#username", "username input field");           // CSS Selector
autoHeal.findElement("//button[@type='submit']", "submit button");   // XPath
autoHeal.findElement("email", "email input field");                  // ID or Name
autoHeal.findElement("Sign In", "login link");                       // Link Text
autoHeal.findElement("button", "any button element");                // Tag Name
autoHeal.findElement("btn-primary", "primary button");               // Class Name
```

### How AutoHeal Works - Intelligent Decision Flow

AutoHeal follows a smart, multi-layered approach to find elements reliably:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          🧠 AutoHeal Decision Flow                          │
└─────────────────────────────────────────────────────────────────────────────┘

    📝 User Request: autoHeal.findElement("#login-btn", "login button")
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Step 1: 🎯 Try Original Locator First                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ SUCCESS? ✅ Element Found → Return immediately (fastest path)       │    │
│  │ FAILURE? ❌ Element not found → Continue to Step 2                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Step 2: 🔄 Check Cache for Previous Success                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Cache Key: "login-btn-login button"                                │    │
│  │ CACHE HIT? ✅ Use cached selector → Return (very fast)            │    │
│  │ CACHE MISS? ❌ No previous success → Continue to Step 3           │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Step 3: 🤖 AI Strategy Selection                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Configuration: HYBRID Strategy                                      │    │
│  │ ┌─────────────────┐    ┌─────────────────────────────────────────┐ │    │
│  │ │   🔍 DOM Only   │    │         🖼️ Visual Analysis             │ │    │
│  │ │   • Fastest     │    │         • Most Accurate                │ │    │
│  │ │   • Cheapest    │    │         • Works with Complex UI        │ │    │
│  │ │   • HTML Analysis│    │         • Screenshot Analysis          │ │    │
│  │ └─────────────────┘    └─────────────────────────────────────────┘ │    │
│  │              │                               │                     │    │
│  │              └──────────┬────────────────────┘                     │    │
│  │                        ▼                                          │    │
│  │           🎯 Try DOM First → Success? Return                      │    │
│  │                        │                                          │    │
│  │                      Failure?                                     │    │
│  │                        │                                          │    │
│  │                        ▼                                          │    │
│  │           🖼️ Try Visual Analysis → Success? Return               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Step 4: 🧠 AI Processing                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ DOM Analysis:                                                       │    │
│  │ • Parse HTML structure                                              │    │
│  │ • Understand "login button" context                                │    │
│  │ • Find matching elements                                           │    │
│  │ • Multiple matches? → Use description for disambiguation           │    │
│  │                                                                     │    │
│  │ Visual Analysis (if needed):                                       │    │
│  │ • Take screenshot                                                  │    │
│  │ • AI vision identifies "login button"                             │    │
│  │ • Map visual element to DOM selector                               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Step 5: ✅ Success & Learning                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ • Return WebElement to user                                         │    │
│  │ • 💾 Cache successful selector for next time                       │    │
│  │ • 📊 Update metrics and confidence scores                          │    │
│  │ • 📝 Log healing activity for reporting                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
                    🎉 Element found and returned to user!
```

### Smart Element Disambiguation

When multiple elements match the same locator, AutoHeal's AI intelligently selects the correct one:

```java
// Example: Page has multiple "Submit" buttons
WebElement submitOrder = autoHeal.findElement("Submit", "submit order button in checkout form");
WebElement submitReview = autoHeal.findElement("Submit", "submit review button in feedback section");

// ✅ AI uses the description context to find the right "Submit" button
// ✅ "checkout form" context → finds the order submission button
// ✅ "feedback section" context → finds the review submission button
// ✅ No manual disambiguation needed - AI handles it automatically
```

### Why This Approach Works

1. **⚡ Fast Path**: Most requests succeed immediately with original locator
2. **🔄 Smart Caching**: Previously healed selectors work instantly
3. **🧠 AI Fallback**: Only use expensive AI when needed
4. **📈 Learning**: System gets smarter and faster over time
5. **💰 Cost Effective**: Minimize AI API calls through intelligent caching

## 📦 Installation & Setup

### Maven Dependency

**Option 1: Maven Central (Coming Soon)**
```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Option 2: Build from Source (Latest Features)**
```bash
# Clone and build locally
git clone https://github.com/your-org/autoheal-locator.git
cd autoheal-locator
mvn clean install
```

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>autoheal-locator</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Basic Configuration

**Option 1: Basic Setup**
```java
// Set your AI provider API key
System.setProperty("OPENAI_API_KEY", "your-api-key-here");

// Initialize AutoHeal with Selenium
WebDriver driver = new ChromeDriver();
AutoHealLocator autoHeal = AutoHealLocator.builder()
    .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
    .build();
```

**Option 2: Spring Boot Auto-Configuration**
```java
@SpringBootTest
public class MyTest {

    @Autowired
    private AutoHealLocator autoHeal;  // Automatically configured!

    @Autowired
    private WebDriver webDriver;       // Auto-configured WebDriver

    @Test
    void testWithAutoHeal() {
        WebElement element = autoHeal.findElement("#login", "login button");
        element.click();
    }
}
```

📚 **See complete Spring Boot setup** in [Spring Boot Integration Guide](./spring-boot-integration.md)

## 🛠️ Quick Start

### Universal Selector Support

AutoHeal automatically detects and supports all major selector types:

```java
// CSS Selectors - Single and Multiple Elements
autoHeal.findElement("#username", "username input field");
autoHeal.findElement(".btn-primary", "primary submit button");
autoHeal.findElement("input[name='email']", "email input field");

// Multiple elements with same selector
List<WebElement> allButtons = autoHeal.findElements(".btn-primary", "all primary buttons on page");
List<WebElement> allInputs = autoHeal.findElements("input", "all input fields");

// XPath - Single and Multiple
autoHeal.findElement("//button[text()='Login']", "login button");
autoHeal.findElement("//input[@placeholder='Enter email']", "email field");
List<WebElement> allRows = autoHeal.findElements("//table//tr", "all table rows");

// ID, Name, Class
autoHeal.findElement("user-email", "email input by ID");
autoHeal.findElement("username", "username field by name attribute");
List<WebElement> menuItems = autoHeal.findElements(".menu-item", "navigation menu items");

// Link Text
autoHeal.findElement("Sign Up Here", "registration link");
autoHeal.findElement("Forgot Password?", "password reset link");
List<WebElement> allLinks = autoHeal.findElements("a", "all links on page");

// Tag Names
autoHeal.findElement("button", "any button element");
List<WebElement> allButtons = autoHeal.findElements("button", "all button elements");
```

### Complete API Overview

```java
// Single Element Methods
WebElement element = autoHeal.findElement(selector, description);                    // Synchronous
CompletableFuture<WebElement> future = autoHeal.findElementAsync(selector, desc);   // Asynchronous

// Multiple Elements Methods
List<WebElement> elements = autoHeal.findElements(selector, description);           // Synchronous ✨ NEW
CompletableFuture<List<WebElement>> futureList = autoHeal.findElementsAsync(selector, desc); // Asynchronous

// Utility Methods
boolean exists = autoHeal.isElementPresentAsync(selector, description).join();      // Check existence
```

---

## 📋 Usage Patterns

### 1. Normal Test Script

```java
public class LoginTest {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                    .provider(AIProvider.OPENAI)
                    .apiKey(System.getProperty("OPENAI_API_KEY"))
                    .build())
                .build())
            .build();
    }

    @Test
    void testLogin() {
        driver.get("https://example.com/login");

        // AutoHeal finds elements even if selectors break
        WebElement usernameField = autoHeal.findElement("#username", "username input field");
        usernameField.sendKeys("testuser");

        WebElement passwordField = autoHeal.findElement("#password", "password input field");
        passwordField.sendKeys("password123");

        WebElement loginButton = autoHeal.findElement("//button[@type='submit']", "login submit button");
        loginButton.click();

        // Verify login success
        WebElement dashboard = autoHeal.findElement(".dashboard-header", "dashboard header after login");
        assertTrue(dashboard.isDisplayed());
    }

    @AfterEach
    void tearDown() {
        autoHeal.shutdown(); // Generate healing reports
        driver.quit();
    }
}
```

### 2. Page Object Model - Recommended Pattern

AutoHeal works best with the **Pure AutoHeal Pattern** - combining Page Factory organization with full healing capabilities:

```java
// Base Page with AutoHeal integration
public abstract class BasePage {
    protected WebDriver driver;
    protected AutoHealLocator autoHeal;

    public BasePage(WebDriver driver, AutoHealLocator autoHeal) {
        this.driver = driver;
        this.autoHeal = autoHeal;
    }

    protected WebElement findElement(String selector, String description) {
        return autoHeal.findElement(selector, description);
    }

    protected List<WebElement> findElements(String selector, String description) {
        return autoHeal.findElements(selector, description);
    }
}

// Login Page - Pure AutoHeal Pattern (Recommended)
public class LoginPage extends BasePage {

    // Locator constants (Page Factory-inspired organization)
    private static final String USERNAME_FIELD = "#username";
    private static final String PASSWORD_FIELD = "#password";
    private static final String LOGIN_BUTTON = "Sign In";
    private static final String ERROR_MESSAGE = ".error-message";

    // Description constants for better healing
    private static final String USERNAME_DESC = "username input field on login page";
    private static final String PASSWORD_DESC = "password input field on login page";
    private static final String LOGIN_BTN_DESC = "login submit button on login page";
    private static final String ERROR_DESC = "error message display on login page";

    public LoginPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    public void enterUsername(String username) {
        WebElement field = findElement(USERNAME_FIELD, USERNAME_DESC);
        field.clear();
        field.sendKeys(username);
    }

    public void enterPassword(String password) {
        WebElement field = findElement(PASSWORD_FIELD, PASSWORD_DESC);
        field.clear();
        field.sendKeys(password);
    }

    public HomePage clickLogin() {
        WebElement button = findElement(LOGIN_BUTTON, LOGIN_BTN_DESC);
        button.click();
        return new HomePage(driver, autoHeal);
    }

    public String getErrorMessage() {
        try {
            return findElement(ERROR_MESSAGE, ERROR_DESC).getText();
        } catch (Exception e) {
            return null;
        }
    }
}
```

**✅ Why This Pattern Works Best:**
- **Page Factory familiarity** with locator constants
- **Full AutoHeal healing** capabilities
- **Clean organization** and maintainability
- **No @FindBy limitations** - dynamic runtime healing

📚 **See detailed comparison** of Page Factory vs AutoHeal patterns in [Page Object Examples](./examples/page-object-examples.md#page-factory-integration)

// Home Page
public class HomePage extends BasePage {

    public HomePage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    public void navigateToProfile() {
        WebElement profileMenu = findElement(".profile-dropdown", "profile menu in header");
        profileMenu.click();

        WebElement profileLink = findElement("View Profile", "profile link in dropdown menu");
        profileLink.click();
    }

    public List<WebElement> getNavigationItems() {
        return autoHeal.findElementsAsync(".nav-item", "navigation menu items")
            .join(); // Convert CompletableFuture to List
    }
}

// Test using Page Objects
public class PageObjectTest {

    @Test
    void testLoginWithPageObjects() {
        WebDriver driver = new ChromeDriver();
        AutoHealLocator autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .build();

        driver.get("https://example.com/login");

        LoginPage loginPage = new LoginPage(driver, autoHeal);
        loginPage.enterUsername("user@example.com");
        loginPage.enterPassword("password123");

        HomePage homePage = loginPage.clickLogin();
        homePage.navigateToProfile();

        autoHeal.shutdown();
        driver.quit();
    }
}
```

### 3. Cucumber Integration

```java
// Step Definitions
public class LoginSteps {
    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private LoginPage loginPage;

    @Before
    public void setUp() {
        driver = new ChromeDriver();
        autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(AutoHealConfiguration.builder()
                .reporting(ReportingConfig.builder()
                    .enabled(true)
                    .generateHTML(true)
                    .reportNamePrefix("Cucumber_AutoHeal")
                    .build())
                .build())
            .build();
        loginPage = new LoginPage(driver, autoHeal);
    }

    @Given("I am on the login page")
    public void i_am_on_the_login_page() {
        driver.get("https://example.com/login");
    }

    @When("I enter username {string}")
    public void i_enter_username(String username) {
        loginPage.enterUsername(username);
    }

    @When("I enter password {string}")
    public void i_enter_password(String password) {
        loginPage.enterPassword(password);
    }

    @When("I click the login button")
    public void i_click_the_login_button() {
        loginPage.clickLogin();
    }

    @Then("I should be logged in successfully")
    public void i_should_be_logged_in_successfully() {
        WebElement dashboard = autoHeal.findElement(".dashboard", "main dashboard after login");
        assertTrue(dashboard.isDisplayed());
    }

    @Then("I should see an error message")
    public void i_should_see_an_error_message() {
        assertTrue(loginPage.isLoginErrorDisplayed());
    }

    @After
    public void tearDown() {
        if (autoHeal != null) {
            autoHeal.shutdown(); // Generates Cucumber healing reports
        }
        if (driver != null) {
            driver.quit();
        }
    }
}
```

```gherkin
# login.feature
Feature: User Login
  As a user
  I want to login to the application
  So that I can access my dashboard

  Scenario: Successful login
    Given I am on the login page
    When I enter username "user@example.com"
    And I enter password "validpassword"
    And I click the login button
    Then I should be logged in successfully

  Scenario: Failed login
    Given I am on the login page
    When I enter username "user@example.com"
    And I enter password "wrongpassword"
    And I click the login button
    Then I should see an error message
```

---

## ⚙️ AI Configuration

### OpenAI Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.OPENAI)
        .apiKey("sk-proj-your-openai-api-key")
        .model("gpt-4o-mini")  // Cost-effective model
        .timeout(Duration.ofSeconds(30))
        .maxRetries(3)
        .visualAnalysisEnabled(true)
        .build())
    .build();
```

**Get OpenAI API Key:**
1. Visit [OpenAI API Keys](https://platform.openai.com/api-keys)
2. Sign up or login to your account
3. Click "Create new secret key"
4. Copy the key and set it in your configuration

### Gemini Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.GEMINI)
        .apiKey("your-gemini-api-key")
        .model("gemini-1.5-pro")
        .timeout(Duration.ofSeconds(45))
        .maxRetries(2)
        .visualAnalysisEnabled(false)  // Gemini doesn't support vision yet
        .build())
    .build();
```

**Get Gemini API Key:**
1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API key"
4. Copy the key for your configuration

### Local LLM Configuration

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.LOCAL_LLM)
        .apiUrl("http://localhost:11434/v1/chat/completions")  // Ollama endpoint
        .model("llama3.1:8b")
        .timeout(Duration.ofSeconds(60))
        .maxRetries(1)
        .visualAnalysisEnabled(false)
        .build())
    .build();
```

**Setup Local LLM (Ollama):**
1. Install [Ollama](https://ollama.ai/)
2. Pull a model: `ollama pull llama3.1:8b`
3. Start server: `ollama serve`
4. Configure AutoHeal to use local endpoint

### Mock AI (Testing)

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .ai(AIConfig.builder()
        .provider(AIProvider.MOCK)
        .build())
    .build();

// Add mock responses for testing
MockAIService mockAI = (MockAIService) autoHeal.getAIService();
mockAI.addMockResponse("login button", "#signin-btn", 0.95);
```

---

## 🎯 Common Usage Examples

### Form Interactions

```java
// Text inputs
WebElement nameField = autoHeal.findElement("firstName", "first name input field");
nameField.sendKeys("John Doe");

// Email input with validation
WebElement emailField = autoHeal.findElement("//input[@type='email']", "email address input");
emailField.sendKeys("john@example.com");

// Password fields
WebElement passwordField = autoHeal.findElement("#new-password", "new password creation field");
passwordField.sendKeys("SecurePass123!");

// Textarea
WebElement commentBox = autoHeal.findElement("textarea[name='comments']", "comments textarea");
commentBox.sendKeys("This is my feedback...");
```

### Checkbox and Radio Buttons

```java
// Checkbox
WebElement agreeCheckbox = autoHeal.findElement("//input[@name='terms']", "terms and conditions checkbox");
if (!agreeCheckbox.isSelected()) {
    agreeCheckbox.click();
}

// Radio buttons
WebElement maleRadio = autoHeal.findElement("input[value='male']", "male gender radio button");
maleRadio.click();

WebElement femaleRadio = autoHeal.findElement("input[value='female']", "female gender radio button");
femaleRadio.click();

// Verify radio selection
WebElement selectedGender = autoHeal.findElement("input[name='gender']:checked", "selected gender option");
String genderValue = selectedGender.getAttribute("value");
```

### Dropdowns and Select Elements

```java
// Standard HTML Select
WebElement countrySelect = autoHeal.findElement("select[name='country']", "country selection dropdown");
Select selectCountry = new Select(countrySelect);
selectCountry.selectByVisibleText("United States");
selectCountry.selectByValue("US");
selectCountry.selectByIndex(1);

// Custom dropdown (div-based)
WebElement customDropdown = autoHeal.findElement(".custom-dropdown", "custom dropdown trigger");
customDropdown.click();
WebElement option = autoHeal.findElement("//div[@data-value='option1']", "first option in custom dropdown");
option.click();

// Multi-select
WebElement multiSelect = autoHeal.findElement("select[multiple]", "multiple selection dropdown");
Select multi = new Select(multiSelect);
multi.selectByVisibleText("Option 1");
multi.selectByVisibleText("Option 2");
multi.selectByVisibleText("Option 3");
```

### File Upload

```java
// File input
WebElement fileInput = autoHeal.findElement("input[type='file']", "file upload input");
fileInput.sendKeys("/path/to/your/file.pdf");

// Drag and drop file upload
WebElement dropZone = autoHeal.findElement(".drop-zone", "drag and drop file area");
// Use Actions class for drag and drop
Actions actions = new Actions(driver);
// Implementation depends on your specific drag-drop component
```

### Buttons and Links

```java
// Submit buttons
WebElement submitBtn = autoHeal.findElement("//button[@type='submit']", "form submit button");
submitBtn.click();

// Link navigation
WebElement homeLink = autoHeal.findElement("Home", "homepage navigation link");
homeLink.click();

// Button with icon
WebElement saveBtn = autoHeal.findElement(".save-button", "save document button with icon");
saveBtn.click();

// Disabled button check
WebElement processBtn = autoHeal.findElement("#process-btn", "process data button");
boolean isEnabled = processBtn.isEnabled();
if (isEnabled) {
    processBtn.click();
}
```

### Tables and Lists

```java
// Table rows - Synchronous (simple and direct)
List<WebElement> tableRows = autoHeal.findElements("//table//tr", "all table rows");
for (WebElement row : tableRows) {
    List<WebElement> cells = row.findElements(By.tagName("td"));
    // Process each cell
}

// Table rows - Asynchronous (for better performance)
List<WebElement> asyncRows = autoHeal.findElementsAsync("//table//tr", "all table rows").join();

// Specific table cell
WebElement priceCell = autoHeal.findElement("//tr[td='Product A']//td[@class='price']", "price cell for Product A");
String price = priceCell.getText();

// List items - Multiple ways to get them
List<WebElement> menuItems = autoHeal.findElements(".menu-item", "navigation menu items");
for (WebElement item : menuItems) {
    System.out.println("Menu: " + item.getText());
}

// Form inputs
List<WebElement> formInputs = autoHeal.findElements("input", "all input fields in registration form");
for (WebElement input : formInputs) {
    if (input.getAttribute("required") != null) {
        System.out.println("Required field: " + input.getAttribute("name"));
    }
}

// Product cards in search results
List<WebElement> products = autoHeal.findElements(".product-card", "product cards on search results page");
System.out.println("Found " + products.size() + " products");

// Navigation links
List<WebElement> navLinks = autoHeal.findElements(".nav-link", "navigation menu links in header");
navLinks.get(2).click(); // Click third navigation item

// Dynamic list filtering
WebElement searchBox = autoHeal.findElement("#search", "search input for filtering list");
searchBox.sendKeys("filter term");
List<WebElement> searchResults = autoHeal.findElements(".search-result", "filtered search result items");
if (!searchResults.isEmpty()) {
    searchResults.get(0).click(); // Click first result
}
```

### Alerts and Confirmations

```java
// Trigger alert
WebElement deleteBtn = autoHeal.findElement(".delete-button", "delete item button");
deleteBtn.click();

// Handle JavaScript alert
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
Alert alert = wait.until(ExpectedConditions.alertIsPresent());
String alertText = alert.getText();
alert.accept(); // or alert.dismiss() for cancel

// Confirmation dialog
WebElement confirmBtn = autoHeal.findElement("//button[text()='Confirm']", "confirmation dialog confirm button");
confirmBtn.click();

// Custom modal dialog
WebElement modalOkBtn = autoHeal.findElement(".modal .btn-primary", "OK button in modal dialog");
modalOkBtn.click();
```

### Window and Tab Handling

```java
// Open new tab/window
String originalWindow = driver.getWindowHandle();
WebElement newTabLink = autoHeal.findElement("//a[@target='_blank']", "open in new tab link");
newTabLink.click();

// Switch to new window
Set<String> allWindows = driver.getWindowHandles();
for (String windowHandle : allWindows) {
    if (!windowHandle.equals(originalWindow)) {
        driver.switchTo().window(windowHandle);
        break;
    }
}

// Work in new window
WebElement newWindowElement = autoHeal.findElement("#new-window-content", "content in new window");
newWindowElement.click();

// Close new window and switch back
driver.close();
driver.switchTo().window(originalWindow);

// Frame switching
WebElement frame = autoHeal.findElement("#myframe", "iframe containing form");
driver.switchTo().frame(frame);
WebElement frameContent = autoHeal.findElement(".frame-content", "content inside iframe");
frameContent.click();
driver.switchTo().defaultContent(); // Switch back to main page
```

### Dynamic Content and AJAX

```java
// Wait for dynamic content
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

// Wait for element to be present
WebElement dynamicElement = wait.until(ExpectedConditions.presenceOfElementLocated(
    LocatorTypeDetector.autoCreateBy(".dynamic-content", "dynamic content area")));

// Wait for element to be clickable
WebElement loadingButton = autoHeal.findElement("#load-data", "load data button");
loadingButton.click();
WebElement loadedContent = wait.until(ExpectedConditions.elementToBeClickable(
    LocatorTypeDetector.autoCreateBy(".loaded-data", "loaded data content")));

// Handle loading indicators
WebElement loadingSpinner = autoHeal.findElement(".loading-spinner", "page loading indicator");
wait.until(ExpectedConditions.invisibilityOf(loadingSpinner));

// AJAX form submission
WebElement ajaxForm = autoHeal.findElement("#ajax-form", "ajax form submission");
WebElement submitBtn = autoHeal.findElement("//form[@id='ajax-form']//button[@type='submit']", "ajax form submit button");
submitBtn.click();

// Wait for success message
WebElement successMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
    LocatorTypeDetector.autoCreateBy(".success-message", "form submission success message")));
```

### Advanced Interactions

```java
// Mouse actions
Actions actions = new Actions(driver);

// Hover
WebElement menuItem = autoHeal.findElement(".menu-item", "dropdown menu trigger");
actions.moveToElement(menuItem).perform();
WebElement subMenu = autoHeal.findElement(".submenu-item", "submenu item after hover");
subMenu.click();

// Right-click context menu
WebElement contextElement = autoHeal.findElement(".context-menu-target", "element with context menu");
actions.contextClick(contextElement).perform();
WebElement contextOption = autoHeal.findElement(".context-menu-option", "context menu option");
contextOption.click();

// Drag and drop
WebElement sourceElement = autoHeal.findElement(".draggable", "draggable element");
WebElement targetElement = autoHeal.findElement(".drop-target", "drop target area");
actions.dragAndDrop(sourceElement, targetElement).perform();

// Double-click
WebElement doubleClickElement = autoHeal.findElement(".double-click-target", "double click target element");
actions.doubleClick(doubleClickElement).perform();

// Keyboard shortcuts
actions.keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).perform(); // Ctrl+A
actions.keyDown(Keys.CONTROL).sendKeys("c").keyUp(Keys.CONTROL).perform(); // Ctrl+C

// Scroll to element
WebElement bottomElement = autoHeal.findElement("#footer", "page footer element");
actions.moveToElement(bottomElement).perform(); // Scrolls to element

// JavaScript execution with AutoHeal elements
WebElement jsTarget = autoHeal.findElement(".js-target", "element for javascript interaction");
JavascriptExecutor js = (JavascriptExecutor) driver;
js.executeScript("arguments[0].style.border='3px solid red'", jsTarget);
js.executeScript("arguments[0].scrollIntoView(true);", jsTarget);
```

---

## 🔧 Advanced Configuration

### Performance Optimization

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .performance(PerformanceConfig.builder()
        .threadPoolSize(4)  // Parallel processing
        .elementTimeout(Duration.ofSeconds(30))
        .enableMetrics(true)
        .executionStrategy(ExecutionStrategy.SMART_SEQUENTIAL)  // Try fast methods first
        .build())
    .cache(CacheConfig.builder()
        .enabled(true)
        .maxSize(1000)
        .expireAfterWrite(Duration.ofHours(1))
        .build())
    .build();
```

### Execution Strategies

```java
// Different strategies for different scenarios
ExecutionStrategy.SEQUENTIAL          // Try DOM then Visual (cost-effective)
ExecutionStrategy.PARALLEL           // Try both simultaneously (fastest)
ExecutionStrategy.SMART_SEQUENTIAL   // Adaptive based on historical success
ExecutionStrategy.DOM_ONLY           // Only DOM analysis (cheapest)
ExecutionStrategy.VISUAL_FIRST       // Visual first, DOM fallback (accurate)
```

### Comprehensive Reporting

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .reporting(ReportingConfig.builder()
        .enabled(true)
        .generateHTML(true)      // Interactive HTML reports
        .generateJSON(true)      // Machine-readable data
        .generateText(true)      // Human-readable summary
        .consoleLogging(true)    // Real-time console output
        .outputDirectory("./reports")
        .reportNamePrefix("AutoHeal_Test")
        .build())
    .build();

// Reports include:
// - Selector success/failure rates
// - Healing strategies used
// - AI token usage and costs
// - Performance metrics
// - Element disambiguation details
```

---

## 🚫 Playwright Integration (Coming Soon)

```java
// Future Playwright support
PlaywrightWebAutomationAdapter playwrightAdapter = new PlaywrightWebAutomationAdapter(page);
AutoHealLocator autoHeal = AutoHealLocator.builder()
    .withWebAdapter(playwrightAdapter)
    .build();

// Same API, different framework
WebElement element = autoHeal.findElement(".selector", "element description");
```

---

## 📊 Best Practices

### 1. Meaningful Descriptions

```java
// ❌ Poor descriptions
autoHeal.findElement("#btn", "button");
autoHeal.findElement(".input", "input field");

// ✅ Good descriptions
autoHeal.findElement("#btn", "submit order button on checkout page");
autoHeal.findElement(".input", "customer email input in registration form");
```

### 2. Selector Strategy

```java
// ✅ Prefer stable attributes
autoHeal.findElement("[data-testid='login-button']", "login button");  // Best
autoHeal.findElement("#login-btn", "login button");                    // Good
autoHeal.findElement(".btn-primary:first-child", "login button");      // Fragile

// ✅ Use semantic descriptions
autoHeal.findElement("Sign In", "main login button");  // Link text - very stable
```

### 3. Error Handling

```java
try {
    WebElement element = autoHeal.findElement(".selector", "target element");
    element.click();
} catch (AutoHealException e) {
    // Log healing failure details
    logger.error("AutoHeal failed to find element: {}", e.getMessage());
    // Implement fallback strategy
}
```

### 4. Resource Management & Reporting

```java
@AfterEach
void tearDown() {
    // Always shutdown AutoHeal to:
    // 1. Generate comprehensive reports in YOUR project directory
    // 2. Clean up resources
    // 3. Save cache data
    autoHeal.shutdown();
    driver.quit();
}
```

**📊 AutoHeal automatically generates detailed reports** in your test project:
- **HTML Report**: Interactive dashboard with metrics and recommendations
- **JSON Report**: Machine-readable data for CI/CD integration
- **Text Report**: Human-readable summary for quick analysis
- **Screenshots**: Visual evidence of healing activities

```
your-test-project/
├── src/test/java/
├── autoheal-reports/           ← Reports generated HERE (your project)
│   ├── MyApp_2024-01-15.html  ← Interactive dashboard
│   ├── MyApp_2024-01-15.json  ← CI/CD integration data
│   └── screenshots/           ← Visual evidence
└── target/
```

📚 **See complete report examples and configuration** in [AutoHeal Reporting Guide](./reporting.md)

---

## 🚀 Parallel Testing Support

AutoHeal Locator is designed for parallel test execution with thread-safe architecture and shared caching.

### Thread-Safe Design

```java
// AutoHeal instances are thread-safe - safe for parallel execution
public class ParallelTestSuite {

    // Shared AutoHeal configuration for all tests
    private static final AutoHealConfiguration SHARED_CONFIG = AutoHealConfiguration.builder()
        .ai(AIConfig.builder()
            .provider(AIProvider.OPENAI)
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .model("gpt-4o-mini")
            .build())
        .cache(CacheConfig.builder()
            .enabled(true)
            .maxSize(10000)                // Shared cache across all threads
            .expireAfterWrite(Duration.ofHours(2))
            .build())
        .performance(PerformanceConfig.builder()
            .threadPoolSize(8)             // Handle concurrent AI requests
            .maxConcurrentRequests(20)     // Rate limiting for API calls
            .build())
        .build();

    @Test
    @Execution(ExecutionMode.CONCURRENT)  // JUnit 5 parallel execution
    void testUserRegistration() throws InterruptedException {
        WebDriver driver = new ThreadLocalWebDriver().get(); // Thread-local driver
        AutoHealLocator autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(SHARED_CONFIG)  // Shared config + cache
            .build();

        try {
            driver.get("https://app.example.com/register");

            // Each thread operates independently
            WebElement emailField = autoHeal.findElement("#email", "email input field");
            emailField.sendKeys("test" + Thread.currentThread().getId() + "@example.com");

            WebElement submitButton = autoHeal.findElement("Register", "registration submit button");
            submitButton.click();

            // Verify success
            WebElement successMsg = autoHeal.findElement(".success-message", "registration success message");
            assertTrue(successMsg.isDisplayed());

        } finally {
            autoHeal.shutdown();  // Thread-safe shutdown
            driver.quit();
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void testUserLogin() throws InterruptedException {
        WebDriver driver = new ThreadLocalWebDriver().get();
        AutoHealLocator autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(SHARED_CONFIG)  // Benefits from shared cache
            .build();

        try {
            driver.get("https://app.example.com/login");

            WebElement usernameField = autoHeal.findElement("#username", "username input field");
            usernameField.sendKeys("testuser" + Thread.currentThread().getId());

            WebElement passwordField = autoHeal.findElement("#password", "password input field");
            passwordField.sendKeys("password123");

            WebElement loginButton = autoHeal.findElement("Sign In", "login button");
            loginButton.click();

            // Verify login
            WebElement dashboard = autoHeal.findElement(".dashboard", "user dashboard");
            assertTrue(dashboard.isDisplayed());

        } finally {
            autoHeal.shutdown();
            driver.quit();
        }
    }
}

// Thread-local WebDriver setup for parallel execution
class ThreadLocalWebDriver {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public WebDriver get() {
        if (driver.get() == null) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");  // Better for parallel execution
            driver.set(new ChromeDriver(options));
        }
        return driver.get();
    }

    public void remove() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
}
```

### TestNG Parallel Execution

```java
// testng.xml configuration for parallel execution
/*
<suite name="ParallelSuite" parallel="methods" thread-count="4">
    <test name="ParallelTests">
        <classes>
            <class name="com.example.ParallelAutoHealTests"/>
        </classes>
    </test>
</suite>
*/

public class ParallelAutoHealTests {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeMethod
    public void setUp() {
        // Each thread gets its own WebDriver instance
        driver = new ChromeDriver();

        // Shared AutoHeal configuration with thread-safe operations
        autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(AutoHealConfiguration.builder()
                .ai(AIConfig.builder()
                    .provider(AIProvider.OPENAI)
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .build())
                .cache(CacheConfig.builder()
                    .enabled(true)    // Shared cache benefits all threads
                    .build())
                .build())
            .build();
    }

    @Test(threadPoolSize = 3, invocationCount = 10)  // Run 10 times with 3 threads
    public void testProductSearch() {
        String threadName = Thread.currentThread().getName();
        System.out.println("Running on thread: " + threadName);

        driver.get("https://shop.example.com");

        WebElement searchBox = autoHeal.findElement("#search", "product search input");
        searchBox.sendKeys("laptop " + threadName);

        WebElement searchButton = autoHeal.findElement("Search", "search submit button");
        searchButton.click();

        List<WebElement> results = autoHeal.findElements(".product-item", "search result products");
        assertTrue(results.size() > 0, "No products found on thread " + threadName);

        System.out.println("Thread " + threadName + " found " + results.size() + " products");
    }

    @AfterMethod
    public void tearDown() {
        if (autoHeal != null) {
            autoHeal.shutdown();
        }
        if (driver != null) {
            driver.quit();
        }
    }
}
```

### Key Benefits for Parallel Testing

1. **Shared Caching**: All parallel threads benefit from the same selector cache
2. **Rate Limiting**: Built-in AI request throttling prevents API limits
3. **Thread Safety**: No race conditions or shared state issues
4. **Resource Efficiency**: Smart connection pooling and request batching
5. **Independent Failures**: One thread's failure doesn't affect others

### Best Practices for Parallel Execution

```java
// 1. Use thread-local WebDriver instances
private static ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

// 2. Configure appropriate thread pool sizes
.performance(PerformanceConfig.builder()
    .threadPoolSize(Runtime.getRuntime().availableProcessors())
    .maxConcurrentRequests(50)  // Balance between speed and API limits
    .build())

// 3. Enable shared caching for better performance
.cache(CacheConfig.builder()
    .enabled(true)
    .maxSize(20000)  // Larger cache for parallel execution
    .build())

// 4. Use headless browsers for faster parallel execution
ChromeOptions options = new ChromeOptions();
options.addArguments("--headless", "--disable-gpu", "--no-sandbox");

// 5. Proper cleanup in parallel tests
@AfterEach
void cleanup() {
    autoHeal.shutdown();  // Thread-safe shutdown
    driver.quit();
}
```

---

## 🔍 Troubleshooting

### Common Issues

**Issue: DOM analysis taking too long**
```java
// Solution: Reduce timeout or use DOM_ONLY strategy
.ai(AIConfig.builder()
    .timeout(Duration.ofSeconds(15))  // Reduce timeout
    .build())
.performance(PerformanceConfig.builder()
    .executionStrategy(ExecutionStrategy.DOM_ONLY)  // Skip visual analysis
    .build())
```

**Issue: High AI costs**
```java
// Solution: Use cheaper models and caching
.ai(AIConfig.builder()
    .model("gpt-4o-mini")  // Use cost-effective model
    .build())
.cache(CacheConfig.builder()
    .enabled(true)  // Enable aggressive caching
    .maxSize(5000)
    .build())
```

**Issue: Visual analysis not working**
```java
// Check: Visual analysis requirements
.ai(AIConfig.builder()
    .provider(AIProvider.OPENAI)  // Only OpenAI supports vision currently
    .visualAnalysisEnabled(true)
    .model("gpt-4o")  // Use vision-capable model
    .build())
```

---

## 📚 Additional Resources

- [GitHub Repository](https://github.com/your-org/autoheal-locator)
- [API Documentation](./api-reference/)
- [Examples Repository](https://github.com/your-org/autoheal-examples)
- [Community Support](https://github.com/your-org/autoheal-locator/discussions)

---

*AutoHeal Locator - Making web automation resilient and intelligent* 🤖✨