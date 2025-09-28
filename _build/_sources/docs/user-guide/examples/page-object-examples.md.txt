# Page Object Model Examples

This guide demonstrates how to implement the Page Object Model (POM) pattern with AutoHeal for maintainable and robust test automation.

## Table of Contents

- [Basic Page Object Setup](#basic-page-object-setup)
- [Advanced Page Object Features](#advanced-page-object-features)
- [Page Factory Integration](#page-factory-integration)
- [Component-Based Architecture](#component-based-architecture)
- [Data-Driven Testing](#data-driven-testing)
- [Best Practices](#best-practices)

---

## Basic Page Object Setup

### Base Page Class

```java
import com.autoheal.AutoHealLocator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    protected WebDriver driver;
    protected AutoHealLocator autoHeal;
    protected WebDriverWait wait;

    public BasePage(WebDriver driver, AutoHealLocator autoHeal) {
        this.driver = driver;
        this.autoHeal = autoHeal;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    // Common utility methods
    protected WebElement findElement(String selector, String description) {
        return autoHeal.findElement(selector, description);
    }

    protected List<WebElement> findElements(String selector, String description) {
        return autoHeal.findElementsAsync(selector, description).join();
    }

    protected void waitForPageToLoad() {
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
            .executeScript("return document.readyState").equals("complete"));
    }

    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // Abstract method to verify page loaded correctly
    public abstract boolean isLoaded();

    // Abstract method to get page URL pattern
    public abstract String getExpectedUrlPattern();

    // Verify we're on the correct page
    public void verifyPageLoaded() {
        assertTrue("Page not loaded correctly", isLoaded());
        if (getExpectedUrlPattern() != null) {
            assertTrue("Wrong URL: " + driver.getCurrentUrl(),
                driver.getCurrentUrl().matches(getExpectedUrlPattern()));
        }
    }
}
```

### Login Page Example

```java
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Page elements with AutoHeal
    private WebElement getUsernameField() {
        return findElement("#username", "username input field on login page");
    }

    private WebElement getPasswordField() {
        return findElement("#password", "password input field on login page");
    }

    private WebElement getLoginButton() {
        return findElement("//button[@type='submit']", "login submit button");
    }

    private WebElement getRememberMeCheckbox() {
        return findElement("#remember-me", "remember me checkbox");
    }

    private WebElement getForgotPasswordLink() {
        return findElement("Forgot Password?", "forgot password link");
    }

    private WebElement getErrorMessage() {
        return findElement(".error-message", "login error message");
    }

    // Page actions
    public LoginPage enterUsername(String username) {
        WebElement usernameField = getUsernameField();
        usernameField.clear();
        usernameField.sendKeys(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        WebElement passwordField = getPasswordField();
        passwordField.clear();
        passwordField.sendKeys(password);
        return this;
    }

    public LoginPage toggleRememberMe() {
        getRememberMeCheckbox().click();
        return this;
    }

    public HomePage loginWithValidCredentials(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        getLoginButton().click();
        return new HomePage(driver, autoHeal);
    }

    public LoginPage loginWithInvalidCredentials(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        getLoginButton().click();
        // Wait for error message to appear
        wait.until(ExpectedConditions.visibilityOf(getErrorMessage()));
        return this;
    }

    public ForgotPasswordPage clickForgotPassword() {
        getForgotPasswordLink().click();
        return new ForgotPasswordPage(driver, autoHeal);
    }

    // Validations
    public boolean isLoginErrorDisplayed() {
        try {
            return getErrorMessage().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginError() {
        return isLoginErrorDisplayed() ? getErrorMessage().getText() : null;
    }

    public boolean isRememberMeChecked() {
        return getRememberMeCheckbox().isSelected();
    }

    // BasePage implementations
    @Override
    public boolean isLoaded() {
        try {
            return getUsernameField().isDisplayed() &&
                   getPasswordField().isDisplayed() &&
                   getLoginButton().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/(login|signin).*";
    }
}
```

### Home Page Example

```java
public class HomePage extends BasePage {

    public HomePage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Header elements
    private WebElement getUserProfileDropdown() {
        return findElement(".user-profile-dropdown", "user profile dropdown in header");
    }

    private WebElement getLogoutLink() {
        return findElement("//a[text()='Logout']", "logout link in profile dropdown");
    }

    private WebElement getWelcomeMessage() {
        return findElement(".welcome-message", "welcome message on homepage");
    }

    // Navigation elements
    private WebElement getDashboardLink() {
        return findElement("Dashboard", "dashboard navigation link");
    }

    private WebElement getProductsLink() {
        return findElement("Products", "products navigation link");
    }

    private WebElement getOrdersLink() {
        return findElement("Orders", "orders navigation link");
    }

    private WebElement getSettingsLink() {
        return findElement("Settings", "settings navigation link");
    }

    // Main content elements
    private List<WebElement> getDashboardWidgets() {
        return findElements(".dashboard-widget", "dashboard widgets on homepage");
    }

    private WebElement getQuickActionsPanel() {
        return findElement("#quick-actions", "quick actions panel");
    }

    // Actions
    public String getWelcomeText() {
        return getWelcomeMessage().getText();
    }

    public LoginPage logout() {
        getUserProfileDropdown().click();
        wait.until(ExpectedConditions.elementToBeClickable(getLogoutLink()));
        getLogoutLink().click();
        return new LoginPage(driver, autoHeal);
    }

    public DashboardPage navigateToDashboard() {
        getDashboardLink().click();
        return new DashboardPage(driver, autoHeal);
    }

    public ProductsPage navigateToProducts() {
        getProductsLink().click();
        return new ProductsPage(driver, autoHeal);
    }

    public OrdersPage navigateToOrders() {
        getOrdersLink().click();
        return new OrdersPage(driver, autoHeal);
    }

    public SettingsPage navigateToSettings() {
        getSettingsLink().click();
        return new SettingsPage(driver, autoHeal);
    }

    public int getWidgetCount() {
        return getDashboardWidgets().size();
    }

    public List<String> getWidgetTitles() {
        return getDashboardWidgets().stream()
            .map(widget -> {
                try {
                    return widget.findElement(By.className("widget-title")).getText();
                } catch (Exception e) {
                    return "Unknown";
                }
            })
            .collect(Collectors.toList());
    }

    // Quick actions
    public CreateOrderPage clickCreateOrder() {
        WebElement createOrderButton = findElement(
            "#quick-actions .create-order-btn",
            "create order button in quick actions"
        );
        createOrderButton.click();
        return new CreateOrderPage(driver, autoHeal);
    }

    public AddProductPage clickAddProduct() {
        WebElement addProductButton = findElement(
            "#quick-actions .add-product-btn",
            "add product button in quick actions"
        );
        addProductButton.click();
        return new AddProductPage(driver, autoHeal);
    }

    // BasePage implementations
    @Override
    public boolean isLoaded() {
        try {
            return getWelcomeMessage().isDisplayed() &&
                   getDashboardLink().isDisplayed() &&
                   getUserProfileDropdown().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/home.*|.*/dashboard.*";
    }
}
```

---

## Advanced Page Object Features

### Form Page with Validation

```java
public class RegistrationPage extends BasePage {

    public RegistrationPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Form field elements
    private WebElement getFirstNameField() {
        return findElement("#firstName", "first name input field");
    }

    private WebElement getLastNameField() {
        return findElement("#lastName", "last name input field");
    }

    private WebElement getEmailField() {
        return findElement("//input[@type='email']", "email address input field");
    }

    private WebElement getPhoneField() {
        return findElement("input[name='phone']", "phone number input field");
    }

    private WebElement getPasswordField() {
        return findElement("#password", "password input field");
    }

    private WebElement getConfirmPasswordField() {
        return findElement("#confirmPassword", "confirm password input field");
    }

    // Dropdown elements
    private WebElement getCountryDropdown() {
        return findElement("select[name='country']", "country selection dropdown");
    }

    private WebElement getGenderDropdown() {
        return findElement("#gender", "gender selection dropdown");
    }

    // Checkbox elements
    private WebElement getTermsCheckbox() {
        return findElement("#terms", "terms and conditions checkbox");
    }

    private WebElement getNewsletterCheckbox() {
        return findElement("#newsletter", "newsletter subscription checkbox");
    }

    // Button elements
    private WebElement getRegisterButton() {
        return findElement("//button[@type='submit']", "register button");
    }

    private WebElement getClearButton() {
        return findElement("Clear Form", "clear form button");
    }

    // Validation elements
    private WebElement getFieldError(String fieldName) {
        return findElement(
            String.format(".field-error[data-field='%s']", fieldName),
            String.format("%s field validation error", fieldName)
        );
    }

    private WebElement getPasswordStrengthIndicator() {
        return findElement(".password-strength", "password strength indicator");
    }

    // Form filling methods
    public RegistrationPage fillPersonalInfo(String firstName, String lastName, String email, String phone) {
        getFirstNameField().sendKeys(firstName);
        getLastNameField().sendKeys(lastName);
        getEmailField().sendKeys(email);
        getPhoneField().sendKeys(phone);
        return this;
    }

    public RegistrationPage fillPassword(String password, String confirmPassword) {
        getPasswordField().sendKeys(password);
        getConfirmPasswordField().sendKeys(confirmPassword);
        return this;
    }

    public RegistrationPage selectCountry(String country) {
        Select countrySelect = new Select(getCountryDropdown());
        countrySelect.selectByVisibleText(country);
        return this;
    }

    public RegistrationPage selectGender(String gender) {
        Select genderSelect = new Select(getGenderDropdown());
        genderSelect.selectByValue(gender.toLowerCase());
        return this;
    }

    public RegistrationPage acceptTerms() {
        WebElement termsCheckbox = getTermsCheckbox();
        if (!termsCheckbox.isSelected()) {
            termsCheckbox.click();
        }
        return this;
    }

    public RegistrationPage subscribeToNewsletter() {
        WebElement newsletterCheckbox = getNewsletterCheckbox();
        if (!newsletterCheckbox.isSelected()) {
            newsletterCheckbox.click();
        }
        return this;
    }

    // Complete registration flow
    public class RegistrationData {
        public String firstName, lastName, email, phone, password, confirmPassword, country, gender;
        public boolean acceptTerms, subscribeNewsletter;
    }

    public HomePage registerUser(RegistrationData data) {
        fillPersonalInfo(data.firstName, data.lastName, data.email, data.phone);
        fillPassword(data.password, data.confirmPassword);
        selectCountry(data.country);
        selectGender(data.gender);

        if (data.acceptTerms) acceptTerms();
        if (data.subscribeNewsletter) subscribeToNewsletter();

        getRegisterButton().click();

        // Wait for registration to complete and redirect
        wait.until(ExpectedConditions.urlContains("/home"));
        return new HomePage(driver, autoHeal);
    }

    // Validation methods
    public boolean hasFieldError(String fieldName) {
        try {
            return getFieldError(fieldName).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getFieldErrorMessage(String fieldName) {
        return hasFieldError(fieldName) ? getFieldError(fieldName).getText() : null;
    }

    public String getPasswordStrength() {
        try {
            return getPasswordStrengthIndicator().getText();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public boolean isFormValid() {
        // Check if register button is enabled (indicates form validation passed)
        return getRegisterButton().isEnabled();
    }

    public RegistrationPage clearForm() {
        getClearButton().click();
        // Wait for form to clear
        wait.until(driver -> getFirstNameField().getAttribute("value").isEmpty());
        return this;
    }

    @Override
    public boolean isLoaded() {
        try {
            return getFirstNameField().isDisplayed() &&
                   getRegisterButton().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/register.*|.*/signup.*";
    }
}
```

### Data Table Page

```java
public class UsersManagementPage extends BasePage {

    public UsersManagementPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Table elements
    private WebElement getUsersTable() {
        return findElement("#users-table", "users data table");
    }

    private WebElement getSearchBox() {
        return findElement("#users-search", "users search input");
    }

    private WebElement getAddUserButton() {
        return findElement("Add User", "add new user button");
    }

    private WebElement getPaginationNext() {
        return findElement(".pagination .next", "next page button");
    }

    private WebElement getPaginationPrevious() {
        return findElement(".pagination .previous", "previous page button");
    }

    // Row-specific elements
    private List<WebElement> getAllUserRows() {
        return getUsersTable().findElements(By.cssSelector("tbody tr"));
    }

    private WebElement getUserRow(String username) {
        return findElement(
            String.format("//tr[td[text()='%s']]", username),
            String.format("table row for user %s", username)
        );
    }

    private WebElement getEditButton(String username) {
        return findElement(
            String.format("//tr[td[text()='%s']]//button[contains(@class,'edit')]", username),
            String.format("edit button for user %s", username)
        );
    }

    private WebElement getDeleteButton(String username) {
        return findElement(
            String.format("//tr[td[text()='%s']]//button[contains(@class,'delete')]", username),
            String.format("delete button for user %s", username)
        );
    }

    // Table operations
    public List<String> getAllUsernames() {
        return getAllUserRows().stream()
            .map(row -> row.findElements(By.tagName("td")).get(1).getText()) // Assuming username is 2nd column
            .collect(Collectors.toList());
    }

    public boolean isUserPresent(String username) {
        try {
            getUserRow(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UsersManagementPage searchUsers(String searchTerm) {
        WebElement searchBox = getSearchBox();
        searchBox.clear();
        searchBox.sendKeys(searchTerm);

        // Wait for search results to update
        wait.until(driver -> {
            List<String> usernames = getAllUsernames();
            return usernames.stream().anyMatch(name ->
                name.toLowerCase().contains(searchTerm.toLowerCase()));
        });

        return this;
    }

    public CreateUserPage clickAddUser() {
        getAddUserButton().click();
        return new CreateUserPage(driver, autoHeal);
    }

    public EditUserPage editUser(String username) {
        getEditButton(username).click();
        return new EditUserPage(driver, autoHeal);
    }

    public UsersManagementPage deleteUser(String username) {
        getDeleteButton(username).click();

        // Handle confirmation dialog
        WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(
            findElement("//button[text()='Confirm']", "delete confirmation button")
        ));
        confirmButton.click();

        // Wait for user to be removed from table
        wait.until(driver -> !isUserPresent(username));

        return this;
    }

    public int getUserCount() {
        return getAllUserRows().size();
    }

    // Pagination
    public boolean hasNextPage() {
        try {
            return getPaginationNext().isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasPreviousPage() {
        try {
            return getPaginationPrevious().isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public UsersManagementPage goToNextPage() {
        if (hasNextPage()) {
            getPaginationNext().click();
            waitForPageToLoad();
        }
        return this;
    }

    public UsersManagementPage goToPreviousPage() {
        if (hasPreviousPage()) {
            getPaginationPrevious().click();
            waitForPageToLoad();
        }
        return this;
    }

    // Table sorting
    public UsersManagementPage sortByColumn(String columnName) {
        WebElement columnHeader = findElement(
            String.format("//th[text()='%s']", columnName),
            String.format("%s column header", columnName)
        );
        columnHeader.click();

        // Wait for sort to complete
        Thread.sleep(1000);
        return this;
    }

    @Override
    public boolean isLoaded() {
        try {
            return getUsersTable().isDisplayed() &&
                   getAddUserButton().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/users.*|.*/user-management.*";
    }
}
```

---

## Page Factory Integration

### Comparison: Page Factory vs AutoHeal Patterns

| Aspect | Traditional Page Factory | AutoHeal Recommended | Page Factory + AutoHeal Hybrid |
|--------|-------------------------|---------------------|--------------------------------|
| **Locator Definition** | `@FindBy` annotations | Constants or inline | Mixed approach |
| **Healing Support** | ❌ No healing | ✅ Full healing | ⚠️ Limited healing |
| **Runtime Flexibility** | ❌ Fixed at init | ✅ Dynamic | ⚠️ Partial |
| **Complexity** | ⭐ Simple | ⭐⭐ Medium | ⭐⭐⭐ Complex |
| **Best For** | Stable apps | Dynamic apps | Legacy migration |

### Option 1: Pure AutoHeal Pattern (Recommended)

```java
// Clean, healing-enabled approach
public class LoginPage extends BasePage {

    // Locator constants for maintainability
    private static final String USERNAME_FIELD = "#username";
    private static final String PASSWORD_FIELD = "#password";
    private static final String LOGIN_BUTTON = "Sign In";
    private static final String ERROR_MESSAGE = ".error-message";

    // Description constants
    private static final String USERNAME_DESC = "username input on login page";
    private static final String PASSWORD_DESC = "password input on login page";
    private static final String LOGIN_BTN_DESC = "login submit button";
    private static final String ERROR_DESC = "login error message";

    public LoginPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Actions using AutoHeal healing
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

### Option 2: Centralized Locator Registry

```java
// Separate locator management for larger teams
public class LoginPageLocators {

    public static class Selectors {
        public static final String USERNAME = "#username";
        public static final String PASSWORD = "#password";
        public static final String LOGIN_BTN = "Sign In";
        public static final String ERROR_MSG = ".error-message";
        public static final String FORGOT_PWD = "Forgot Password?";
    }

    public static class Descriptions {
        public static final String USERNAME = "username input field on login page";
        public static final String PASSWORD = "password input field on login page";
        public static final String LOGIN_BTN = "login submit button on login page";
        public static final String ERROR_MSG = "error message display on login page";
        public static final String FORGOT_PWD = "forgot password link on login page";
    }
}

// Page Object using centralized locators
public class LoginPageWithRegistry extends BasePage {

    public LoginPageWithRegistry(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    public void enterCredentials(String username, String password) {
        enterUsername(username);
        enterPassword(password);
    }

    public void enterUsername(String username) {
        WebElement field = findElement(
            LoginPageLocators.Selectors.USERNAME,
            LoginPageLocators.Descriptions.USERNAME
        );
        field.clear();
        field.sendKeys(username);
    }

    public void enterPassword(String password) {
        WebElement field = findElement(
            LoginPageLocators.Selectors.PASSWORD,
            LoginPageLocators.Descriptions.PASSWORD
        );
        field.clear();
        field.sendKeys(password);
    }

    public HomePage submitLogin() {
        WebElement button = findElement(
            LoginPageLocators.Selectors.LOGIN_BTN,
            LoginPageLocators.Descriptions.LOGIN_BTN
        );
        button.click();
        return new HomePage(driver, autoHeal);
    }

    public void clickForgotPassword() {
        WebElement link = findElement(
            LoginPageLocators.Selectors.FORGOT_PWD,
            LoginPageLocators.Descriptions.FORGOT_PWD
        );
        link.click();
    }
}
```

### Option 3: Hybrid Page Factory + AutoHeal

```java
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

// Use for legacy migration or when mixing stable/unstable elements
public class HybridLoginPage extends BasePage {

    // Page Factory for stable elements (no healing)
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    public HybridLoginPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
        PageFactory.initElements(driver, this);
    }

    // Use Page Factory elements when they work
    public void enterUsername(String username) {
        try {
            usernameField.clear();
            usernameField.sendKeys(username);
        } catch (Exception e) {
            // Fallback to AutoHeal when Page Factory fails
            WebElement field = findElement("#username", "username input field");
            field.clear();
            field.sendKeys(username);
        }
    }

    // Use AutoHeal for unstable/dynamic elements
    public HomePage clickLogin() {
        // Dynamic button text - use AutoHeal
        WebElement button = findElement("Sign In", "login submit button");
        button.click();
        return new HomePage(driver, autoHeal);
    }

    public String getErrorMessage() {
        // Dynamic error messages - use AutoHeal
        try {
            return findElement(".error-message", "login error message").getText();
        } catch (Exception e) {
            return null;
        }
    }
}
```

### Recommendation Summary

**✅ Recommended: Option 1 (Pure AutoHeal Pattern)**
- **Benefits**: Full healing capability, clean code, consistent pattern
- **Best for**: New projects, dynamic applications, teams wanting self-healing tests

**⚠️ Consider: Option 2 (Centralized Registry)**
- **Benefits**: Enterprise-scale locator management, team collaboration
- **Best for**: Large teams, multiple projects, strict governance requirements

**🔄 Migration: Option 3 (Hybrid)**
- **Benefits**: Gradual migration path, mixed stability requirements
- **Best for**: Legacy codebases, phased adoption, mixed element stability

### AutoHeal Page Factory (Legacy Support)
    }

    // Hybrid approach: Use @FindBy for stable elements, AutoHeal for dynamic ones
    public PageFactoryLoginPage enterCredentials(String username, String password) {
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        return this;
    }

    public HomePage login() {
        loginButton.click();

        // Use AutoHeal for dynamic success message that might change
        WebElement successMessage = findElement(
            ".success-notification",
            "login success notification message"
        );

        wait.until(ExpectedConditions.visibilityOf(successMessage));
        return new HomePage(driver, autoHeal);
    }

    // Use AutoHeal for error elements that might have unstable selectors
    public String getLoginError() {
        try {
            WebElement errorMessage = findElement(
                ".error-message",
                "login error message display"
            );
            return errorMessage.getText();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isLoaded() {
        return usernameField.isDisplayed() &&
               passwordField.isDisplayed() &&
               loginButton.isDisplayed();
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/(login|signin).*";
    }
}
```

---

## Component-Based Architecture

Create reusable components that can be shared across multiple pages, improving maintainability and reducing code duplication.

### Reusable Components

```java
// Header component that appears on multiple pages
public class HeaderComponent {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    public HeaderComponent(WebDriver driver, AutoHealLocator autoHeal) {
        this.driver = driver;
        this.autoHeal = autoHeal;
    }

    private WebElement getLogo() {
        return autoHeal.findElement(".company-logo", "company logo in header");
    }

    private WebElement getUserMenu() {
        return autoHeal.findElement(".user-menu", "user menu dropdown in header");
    }

    private WebElement getNotificationsBell() {
        return autoHeal.findElement(".notifications-bell", "notifications bell icon");
    }

    private WebElement getSearchBox() {
        return autoHeal.findElement("#global-search", "global search input in header");
    }

    // Component actions
    public HomePage clickLogo() {
        getLogo().click();
        return new HomePage(driver, autoHeal);
    }

    public HeaderComponent openUserMenu() {
        getUserMenu().click();
        return this;
    }

    public LoginPage logout() {
        openUserMenu();
        WebElement logoutLink = autoHeal.findElement(
            "//a[text()='Logout']",
            "logout link in user menu"
        );
        logoutLink.click();
        return new LoginPage(driver, autoHeal);
    }

    public NotificationsPage openNotifications() {
        getNotificationsBell().click();
        return new NotificationsPage(driver, autoHeal);
    }

    public SearchResultsPage search(String searchTerm) {
        WebElement searchBox = getSearchBox();
        searchBox.clear();
        searchBox.sendKeys(searchTerm);
        searchBox.sendKeys(Keys.ENTER);
        return new SearchResultsPage(driver, autoHeal);
    }

    public int getNotificationCount() {
        try {
            WebElement notificationBadge = autoHeal.findElement(
                ".notifications-bell .badge",
                "notification count badge"
            );
            return Integer.parseInt(notificationBadge.getText());
        } catch (Exception e) {
            return 0;
        }
    }
}

// Navigation component
public class NavigationComponent {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    public NavigationComponent(WebDriver driver, AutoHealLocator autoHeal) {
        this.driver = driver;
        this.autoHeal = autoHeal;
    }

    private WebElement getNavItem(String itemName) {
        return autoHeal.findElement(
            String.format("//nav//a[text()='%s']", itemName),
            String.format("%s navigation item", itemName)
        );
    }

    public DashboardPage navigateToDashboard() {
        getNavItem("Dashboard").click();
        return new DashboardPage(driver, autoHeal);
    }

    public ProductsPage navigateToProducts() {
        getNavItem("Products").click();
        return new ProductsPage(driver, autoHeal);
    }

    public OrdersPage navigateToOrders() {
        getNavItem("Orders").click();
        return new OrdersPage(driver, autoHeal);
    }

    public boolean isNavItemActive(String itemName) {
        try {
            WebElement navItem = getNavItem(itemName);
            return navItem.getAttribute("class").contains("active");
        } catch (Exception e) {
            return false;
        }
    }
}

// Enhanced page with components
public class DashboardPage extends BasePage {
    private HeaderComponent header;
    private NavigationComponent navigation;

    public DashboardPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
        this.header = new HeaderComponent(driver, autoHeal);
        this.navigation = new NavigationComponent(driver, autoHeal);
    }

    // Expose components
    public HeaderComponent getHeader() {
        return header;
    }

    public NavigationComponent getNavigation() {
        return navigation;
    }

    // Page-specific elements
    private WebElement getDashboardTitle() {
        return findElement("h1.dashboard-title", "dashboard page title");
    }

    private List<WebElement> getDashboardWidgets() {
        return autoHeal.findElementsAsync(".widget", "dashboard widgets").join();
    }

    // Page actions
    public String getTitle() {
        return getDashboardTitle().getText();
    }

    public int getWidgetCount() {
        return getDashboardWidgets().size();
    }

    @Override
    public boolean isLoaded() {
        try {
            return getDashboardTitle().isDisplayed() &&
                   navigation.isNavItemActive("Dashboard");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/dashboard.*";
    }
}
```

---

## Data-Driven Testing

### Test Data Models

```java
// User data model
public class UserTestData {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String country;
    private String role;

    // Constructors, getters, setters
    public UserTestData(String firstName, String lastName, String email,
                       String password, String country, String role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.country = country;
        this.role = role;
    }

    // Getters and setters...

    // Factory methods for common test scenarios
    public static UserTestData validUser() {
        return new UserTestData("John", "Doe", "john.doe@example.com",
                               "Password123!", "United States", "User");
    }

    public static UserTestData adminUser() {
        return new UserTestData("Admin", "User", "admin@example.com",
                               "AdminPass123!", "United States", "Admin");
    }

    public static UserTestData invalidEmailUser() {
        return new UserTestData("Invalid", "User", "invalid-email",
                               "Password123!", "United States", "User");
    }
}

// Enhanced registration page with data-driven methods
public class DataDrivenRegistrationPage extends BasePage {

    public DataDrivenRegistrationPage(WebDriver driver, AutoHealLocator autoHeal) {
        super(driver, autoHeal);
    }

    // Bulk fill form using test data object
    public DataDrivenRegistrationPage fillRegistrationForm(UserTestData userData) {
        findElement("#firstName", "first name field").sendKeys(userData.getFirstName());
        findElement("#lastName", "last name field").sendKeys(userData.getLastName());
        findElement("#email", "email field").sendKeys(userData.getEmail());
        findElement("#password", "password field").sendKeys(userData.getPassword());

        // Select country from dropdown
        Select countrySelect = new Select(findElement("select[name='country']", "country dropdown"));
        countrySelect.selectByVisibleText(userData.getCountry());

        return this;
    }

    // Validation with expected results
    public Map<String, String> validateForm(UserTestData userData) {
        fillRegistrationForm(userData);

        // Trigger validation by clicking register
        findElement("//button[@type='submit']", "register button").click();

        Map<String, String> validationErrors = new HashMap<>();

        // Check for field-specific errors
        String[] fields = {"firstName", "lastName", "email", "password"};
        for (String field : fields) {
            try {
                WebElement errorElement = findElement(
                    String.format(".error[data-field='%s']", field),
                    String.format("%s validation error", field)
                );
                if (errorElement.isDisplayed()) {
                    validationErrors.put(field, errorElement.getText());
                }
            } catch (Exception e) {
                // No error for this field
            }
        }

        return validationErrors;
    }

    @Override
    public boolean isLoaded() {
        return findElement("#firstName", "first name field").isDisplayed();
    }

    @Override
    public String getExpectedUrlPattern() {
        return ".*/register.*";
    }
}
```

### Parameterized Test Example

```java
@ParameterizedTest
@MethodSource("provideUserTestData")
void testUserRegistrationWithVariousData(UserTestData userData, boolean shouldSucceed,
                                         Map<String, String> expectedErrors) {

    DataDrivenRegistrationPage registrationPage = new DataDrivenRegistrationPage(driver, autoHeal);
    driver.get("https://example.com/register");

    if (shouldSucceed) {
        // Test successful registration
        HomePage homePage = registrationPage.fillRegistrationForm(userData)
            .clickRegister();

        assertTrue(homePage.isLoaded());
        assertTrue(homePage.getWelcomeText().contains(userData.getFirstName()));

    } else {
        // Test validation errors
        Map<String, String> actualErrors = registrationPage.validateForm(userData);

        for (Map.Entry<String, String> expectedError : expectedErrors.entrySet()) {
            assertTrue("Expected error for field: " + expectedError.getKey(),
                      actualErrors.containsKey(expectedError.getKey()));
            assertTrue("Error message mismatch for field: " + expectedError.getKey(),
                      actualErrors.get(expectedError.getKey()).contains(expectedError.getValue()));
        }
    }
}

static Stream<Arguments> provideUserTestData() {
    return Stream.of(
        // Valid user - should succeed
        Arguments.of(
            UserTestData.validUser(),
            true,
            Collections.emptyMap()
        ),

        // Invalid email - should fail with email error
        Arguments.of(
            UserTestData.invalidEmailUser(),
            false,
            Map.of("email", "Please enter a valid email")
        ),

        // Missing data - should fail with required field errors
        Arguments.of(
            new UserTestData("", "", "", "", "", ""),
            false,
            Map.of(
                "firstName", "First name is required",
                "lastName", "Last name is required",
                "email", "Email is required",
                "password", "Password is required"
            )
        )
    );
}
```

---

## Best Practices

### 1. Consistent Element Location Strategy

```java
// ❌ Inconsistent approaches
public class InconsistentPage extends BasePage {
    public WebElement getElement1() {
        return driver.findElement(By.id("element1")); // Direct Selenium
    }

    public WebElement getElement2() {
        return autoHeal.findElement("#element2", "second element"); // AutoHeal
    }
}

// ✅ Consistent AutoHeal usage
public class ConsistentPage extends BasePage {
    public WebElement getElement1() {
        return findElement("#element1", "first element with specific purpose");
    }

    public WebElement getElement2() {
        return findElement("#element2", "second element with clear description");
    }
}
```

### 2. Meaningful Element Descriptions

```java
// ❌ Generic descriptions
findElement("#btn1", "button");
findElement(".input", "input field");

// ✅ Specific, contextual descriptions
findElement("#btn1", "submit order button on checkout page");
findElement(".input", "customer email input in registration form");
```

### 3. Page State Validation

```java
public class WellValidatedPage extends BasePage {

    @Override
    public boolean isLoaded() {
        try {
            // Check multiple indicators of page readiness
            return isMainContentLoaded() &&
                   isNavigationReady() &&
                   areScriptsLoaded();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMainContentLoaded() {
        return findElement("h1.page-title", "page title").isDisplayed();
    }

    private boolean isNavigationReady() {
        return findElement(".main-nav", "main navigation").isDisplayed();
    }

    private boolean areScriptsLoaded() {
        return (Boolean) ((JavascriptExecutor) driver)
            .executeScript("return window.appReady === true");
    }

    // Always verify page loaded in constructor or navigation methods
    public SomePage navigateToOtherPage() {
        findElement("Other Page", "other page link").click();
        SomePage otherPage = new SomePage(driver, autoHeal);
        otherPage.verifyPageLoaded(); // Ensure page is ready
        return otherPage;
    }
}
```

### 4. Error Handling and Resilience

```java
public class ResilientPage extends BasePage {

    public String getOptionalText(String selector, String description, String defaultValue) {
        try {
            WebElement element = findElement(selector, description);
            return element.getText();
        } catch (Exception e) {
            logger.debug("Optional element not found: " + description);
            return defaultValue;
        }
    }

    public boolean isElementPresent(String selector, String description) {
        try {
            findElement(selector, description);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public WebElement waitForElement(String selector, String description, Duration timeout) {
        WebDriverWait customWait = new WebDriverWait(driver, timeout);
        return customWait.until(driver -> {
            try {
                return findElement(selector, description);
            } catch (Exception e) {
                return null;
            }
        });
    }
}
```

### 5. Fluent Interface Design

```java
public class FluentPage extends BasePage {

    // Return 'this' for method chaining on same page
    public FluentPage enterData(String data) {
        findElement("#input", "data input field").sendKeys(data);
        return this;
    }

    public FluentPage selectOption(String option) {
        findElement("//option[text()='" + option + "']", "dropdown option").click();
        return this;
    }

    public FluentPage checkBox() {
        findElement("#checkbox", "agreement checkbox").click();
        return this;
    }

    // Return different page object when navigation occurs
    public NextPage submit() {
        findElement("Submit", "submit button").click();
        return new NextPage(driver, autoHeal);
    }

    // Usage example:
    // NextPage result = fluentPage
    //     .enterData("test data")
    //     .selectOption("Option 1")
    //     .checkBox()
    //     .submit();
}
```

This comprehensive Page Object Model guide demonstrates how to leverage AutoHeal's self-healing capabilities within a well-structured, maintainable test automation framework. The combination of POM patterns with AutoHeal's intelligent element location creates robust, resilient test suites that adapt to application changes. 
