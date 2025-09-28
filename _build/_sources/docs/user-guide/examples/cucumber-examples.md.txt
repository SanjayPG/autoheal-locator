# Cucumber Integration Examples

This guide demonstrates how to integrate AutoHeal with Cucumber for behavior-driven testing, combining natural language test scenarios with intelligent element location.

## Table of Contents

- [Basic Setup](#basic-setup)
- [Feature Files](#feature-files)
- [Step Definitions](#step-definitions)
- [Hooks and Configuration](#hooks-and-configuration)
- [Data Tables](#data-tables)
- [Scenario Outlines](#scenario-outlines)
- [Best Practices](#best-practices)

---

## Basic Setup

### Dependencies

```xml
<dependencies>
    <!-- AutoHeal -->
    <dependency>
        <groupId>com.autoheal</groupId>
        <artifactId>autoheal-locator</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Cucumber -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>7.15.0</version>
    </dependency>
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>7.15.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Selenium -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>

    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-suite</artifactId>
        <version>1.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Test Runner

```java
import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty,html:target/cucumber-reports,json:target/cucumber-reports/Cucumber.json")
@ConfigurationParameter(key = "cucumber.glue", value = "com.yourcompany.stepdefinitions")
@ConfigurationParameter(key = "cucumber.features", value = "src/test/resources/features")
public class CucumberTestRunner {
}
```

---

## Feature Files

### Login Feature

```gherkin
@login
Feature: User Authentication
  As a user
  I want to log into the application
  So that I can access my account

  Background:
    Given I am on the login page

  @smoke @positive
  Scenario: Successful login with valid credentials
    When I enter username "john.doe@example.com"
    And I enter password "validpassword"
    And I click the login button
    Then I should be logged in successfully
    And I should see the welcome message "Welcome, John"

  @negative
  Scenario: Failed login with invalid credentials
    When I enter username "john.doe@example.com"
    And I enter password "wrongpassword"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should remain on the login page

  @negative
  Scenario: Login with empty credentials
    When I click the login button
    Then I should see validation errors
      | field    | message                |
      | username | Username is required   |
      | password | Password is required   |

  @security
  Scenario: Account lockout after multiple failed attempts
    Given I have made 2 failed login attempts
    When I enter username "john.doe@example.com"
    And I enter password "wrongpassword"
    And I click the login button
    Then I should see an error message "Account locked due to multiple failed attempts"
    And the login button should be disabled
```

### E-commerce Feature

```gherkin
@ecommerce
Feature: Product Purchase Flow
  As a customer
  I want to purchase products
  So that I can buy items I need

  Background:
    Given I am logged in as a customer
    And I am on the products page

  @smoke @positive
  Scenario: Complete purchase flow
    When I search for "laptop"
    And I select the first product from search results
    And I add the product to cart
    And I go to checkout
    And I fill in shipping information:
      | field     | value           |
      | firstName | John            |
      | lastName  | Doe             |
      | address   | 123 Main St     |
      | city      | New York        |
      | zipCode   | 10001           |
    And I select payment method "Credit Card"
    And I enter payment details:
      | cardNumber | 4532015112830366 |
      | expiryDate | 12/25            |
      | cvv        | 123              |
    And I place the order
    Then I should see order confirmation
    And I should receive an order number
    And the order should appear in my order history

  @negative
  Scenario: Checkout with invalid payment information
    Given I have items in my cart
    When I go to checkout
    And I fill in valid shipping information
    And I enter invalid payment details:
      | cardNumber | 1234567890123456 |
      | expiryDate | 01/20            |
      | cvv        | 999              |
    And I place the order
    Then I should see payment error "Invalid card details"
    And I should remain on the checkout page
```

### Admin Dashboard Feature

```gherkin
@admin
Feature: User Management
  As an administrator
  I want to manage user accounts
  So that I can maintain system security

  Background:
    Given I am logged in as an administrator
    And I am on the user management page

  @crud
  Scenario: Create a new user account
    When I click the "Add User" button
    And I fill in the user creation form:
      | firstName | Jane               |
      | lastName  | Smith              |
      | email     | jane.smith@example.com |
      | role      | User               |
    And I save the user
    Then I should see success message "User created successfully"
    And the user "jane.smith@example.com" should appear in the users list
    And the user should have role "User"

  @crud
  Scenario: Edit existing user details
    Given a user "john.doe@example.com" exists
    When I search for user "john.doe@example.com"
    And I click the edit button for that user
    And I update the user information:
      | firstName | Jonathan    |
      | role      | Admin       |
    And I save the changes
    Then I should see success message "User updated successfully"
    And the user details should be updated in the system

  @crud @confirmation
  Scenario: Delete user with confirmation
    Given a user "test.user@example.com" exists
    When I search for user "test.user@example.com"
    And I click the delete button for that user
    And I confirm the deletion
    Then I should see success message "User deleted successfully"
    And the user "test.user@example.com" should not appear in the users list
```

---

## Step Definitions

### Login Step Definitions

```java
package com.yourcompany.stepdefinitions;

import com.autoheal.AutoHealLocator;
import com.yourcompany.pages.LoginPage;
import com.yourcompany.pages.HomePage;
import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class LoginSteps {

    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private LoginPage loginPage;
    private HomePage homePage;

    // Constructor injection from hooks
    public LoginSteps(TestContext context) {
        this.driver = context.getDriver();
        this.autoHeal = context.getAutoHeal();
    }

    @Given("I am on the login page")
    public void i_am_on_the_login_page() {
        driver.get("https://example.com/login");
        loginPage = new LoginPage(driver, autoHeal);
        assertTrue(loginPage.isLoaded(), "Login page should be loaded");
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
        loginPage.clickLoginButton();
    }

    @Then("I should be logged in successfully")
    public void i_should_be_logged_in_successfully() {
        homePage = new HomePage(driver, autoHeal);
        assertTrue(homePage.isLoaded(), "Home page should be loaded after successful login");
    }

    @Then("I should see the welcome message {string}")
    public void i_should_see_the_welcome_message(String expectedMessage) {
        String actualMessage = homePage.getWelcomeMessage();
        assertTrue(actualMessage.contains(expectedMessage),
            "Expected welcome message: " + expectedMessage + ", but got: " + actualMessage);
    }

    @Then("I should see an error message {string}")
    public void i_should_see_an_error_message(String expectedError) {
        String actualError = loginPage.getErrorMessage();
        assertEquals(expectedError, actualError, "Error message mismatch");
    }

    @Then("I should remain on the login page")
    public void i_should_remain_on_the_login_page() {
        assertTrue(loginPage.isLoaded(), "Should remain on login page after failed login");
    }

    @Then("I should see validation errors")
    public void i_should_see_validation_errors(DataTable dataTable) {
        List<Map<String, String>> validationErrors = dataTable.asMaps();

        for (Map<String, String> error : validationErrors) {
            String field = error.get("field");
            String expectedMessage = error.get("message");

            String actualMessage = loginPage.getFieldValidationError(field);
            assertEquals(expectedMessage, actualMessage,
                "Validation error mismatch for field: " + field);
        }
    }

    @Given("I have made {int} failed login attempts")
    public void i_have_made_failed_login_attempts(int attempts) {
        for (int i = 0; i < attempts; i++) {
            loginPage.enterUsername("john.doe@example.com");
            loginPage.enterPassword("wrongpassword");
            loginPage.clickLoginButton();
            loginPage.waitForErrorMessage();
        }
    }

    @Then("the login button should be disabled")
    public void the_login_button_should_be_disabled() {
        assertFalse(loginPage.isLoginButtonEnabled(), "Login button should be disabled after account lockout");
    }
}
```

### E-commerce Step Definitions

```java
package com.yourcompany.stepdefinitions;

import com.autoheal.AutoHealLocator;
import com.yourcompany.pages.*;
import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class EcommerceSteps {

    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private ProductsPage productsPage;
    private ProductDetailPage productDetailPage;
    private CartPage cartPage;
    private CheckoutPage checkoutPage;
    private OrderConfirmationPage confirmationPage;

    public EcommerceSteps(TestContext context) {
        this.driver = context.getDriver();
        this.autoHeal = context.getAutoHeal();
    }

    @Given("I am logged in as a customer")
    public void i_am_logged_in_as_a_customer() {
        // Login with customer credentials
        driver.get("https://example.com/login");
        LoginPage loginPage = new LoginPage(driver, autoHeal);
        loginPage.loginWithCredentials("customer@example.com", "password123");
    }

    @Given("I am on the products page")
    public void i_am_on_the_products_page() {
        driver.get("https://example.com/products");
        productsPage = new ProductsPage(driver, autoHeal);
        assertTrue(productsPage.isLoaded(), "Products page should be loaded");
    }

    @When("I search for {string}")
    public void i_search_for(String searchTerm) {
        productsPage.searchForProduct(searchTerm);
    }

    @When("I select the first product from search results")
    public void i_select_the_first_product_from_search_results() {
        productDetailPage = productsPage.clickFirstProduct();
        assertTrue(productDetailPage.isLoaded(), "Product detail page should be loaded");
    }

    @When("I add the product to cart")
    public void i_add_the_product_to_cart() {
        productDetailPage.addToCart();
    }

    @When("I go to checkout")
    public void i_go_to_checkout() {
        cartPage = productDetailPage.goToCart();
        checkoutPage = cartPage.proceedToCheckout();
        assertTrue(checkoutPage.isLoaded(), "Checkout page should be loaded");
    }

    @When("I fill in shipping information:")
    public void i_fill_in_shipping_information(DataTable dataTable) {
        Map<String, String> shippingInfo = dataTable.asMap();
        checkoutPage.fillShippingInformation(shippingInfo);
    }

    @When("I select payment method {string}")
    public void i_select_payment_method(String paymentMethod) {
        checkoutPage.selectPaymentMethod(paymentMethod);
    }

    @When("I enter payment details:")
    public void i_enter_payment_details(DataTable dataTable) {
        Map<String, String> paymentDetails = dataTable.asMap();
        checkoutPage.fillPaymentDetails(paymentDetails);
    }

    @When("I place the order")
    public void i_place_the_order() {
        checkoutPage.placeOrder();
    }

    @Then("I should see order confirmation")
    public void i_should_see_order_confirmation() {
        confirmationPage = new OrderConfirmationPage(driver, autoHeal);
        assertTrue(confirmationPage.isLoaded(), "Order confirmation page should be loaded");
    }

    @Then("I should receive an order number")
    public void i_should_receive_an_order_number() {
        String orderNumber = confirmationPage.getOrderNumber();
        assertNotNull(orderNumber, "Order number should be present");
        assertFalse(orderNumber.isEmpty(), "Order number should not be empty");
    }

    @Then("the order should appear in my order history")
    public void the_order_should_appear_in_my_order_history() {
        String orderNumber = confirmationPage.getOrderNumber();
        OrderHistoryPage orderHistoryPage = confirmationPage.goToOrderHistory();

        assertTrue(orderHistoryPage.isOrderPresent(orderNumber),
            "Order " + orderNumber + " should appear in order history");
    }

    @Given("I have items in my cart")
    public void i_have_items_in_my_cart() {
        // Add a sample item to cart
        i_am_on_the_products_page();
        i_search_for("test product");
        i_select_the_first_product_from_search_results();
        i_add_the_product_to_cart();
    }

    @When("I fill in valid shipping information")
    public void i_fill_in_valid_shipping_information() {
        Map<String, String> validShippingInfo = Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "address", "123 Main St",
            "city", "New York",
            "zipCode", "10001"
        );
        checkoutPage.fillShippingInformation(validShippingInfo);
    }

    @When("I enter invalid payment details:")
    public void i_enter_invalid_payment_details(DataTable dataTable) {
        Map<String, String> invalidPaymentDetails = dataTable.asMap();
        checkoutPage.fillPaymentDetails(invalidPaymentDetails);
    }

    @Then("I should see payment error {string}")
    public void i_should_see_payment_error(String expectedError) {
        String actualError = checkoutPage.getPaymentError();
        assertEquals(expectedError, actualError, "Payment error message mismatch");
    }

    @Then("I should remain on the checkout page")
    public void i_should_remain_on_the_checkout_page() {
        assertTrue(checkoutPage.isLoaded(), "Should remain on checkout page after payment error");
    }
}
```

### Admin Step Definitions

```java
package com.yourcompany.stepdefinitions;

import com.autoheal.AutoHealLocator;
import com.yourcompany.pages.UserManagementPage;
import com.yourcompany.pages.CreateUserPage;
import com.yourcompany.pages.EditUserPage;
import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class AdminSteps {

    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private UserManagementPage userManagementPage;
    private CreateUserPage createUserPage;
    private EditUserPage editUserPage;

    public AdminSteps(TestContext context) {
        this.driver = context.getDriver();
        this.autoHeal = context.getAutoHeal();
    }

    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        driver.get("https://example.com/login");
        LoginPage loginPage = new LoginPage(driver, autoHeal);
        loginPage.loginWithCredentials("admin@example.com", "adminpassword");
    }

    @Given("I am on the user management page")
    public void i_am_on_the_user_management_page() {
        driver.get("https://example.com/admin/users");
        userManagementPage = new UserManagementPage(driver, autoHeal);
        assertTrue(userManagementPage.isLoaded(), "User management page should be loaded");
    }

    @When("I click the {string} button")
    public void i_click_the_button(String buttonText) {
        if ("Add User".equals(buttonText)) {
            createUserPage = userManagementPage.clickAddUser();
            assertTrue(createUserPage.isLoaded(), "Create user page should be loaded");
        }
    }

    @When("I fill in the user creation form:")
    public void i_fill_in_the_user_creation_form(DataTable dataTable) {
        Map<String, String> userDetails = dataTable.asMap();
        createUserPage.fillUserForm(userDetails);
    }

    @When("I save the user")
    public void i_save_the_user() {
        userManagementPage = createUserPage.saveUser();
    }

    @Then("I should see success message {string}")
    public void i_should_see_success_message(String expectedMessage) {
        String actualMessage = userManagementPage.getSuccessMessage();
        assertEquals(expectedMessage, actualMessage, "Success message mismatch");
    }

    @Then("the user {string} should appear in the users list")
    public void the_user_should_appear_in_the_users_list(String userEmail) {
        assertTrue(userManagementPage.isUserPresent(userEmail),
            "User " + userEmail + " should appear in the users list");
    }

    @Then("the user should have role {string}")
    public void the_user_should_have_role(String expectedRole) {
        // This would need to be implemented to check user role in the table
        String actualRole = userManagementPage.getUserRole("jane.smith@example.com");
        assertEquals(expectedRole, actualRole, "User role mismatch");
    }

    @Given("a user {string} exists")
    public void a_user_exists(String userEmail) {
        // Create user if not exists, or verify user exists
        if (!userManagementPage.isUserPresent(userEmail)) {
            createTestUser(userEmail);
        }
    }

    @When("I search for user {string}")
    public void i_search_for_user(String userEmail) {
        userManagementPage.searchUser(userEmail);
    }

    @When("I click the edit button for that user")
    public void i_click_the_edit_button_for_that_user() {
        editUserPage = userManagementPage.clickEditButton();
        assertTrue(editUserPage.isLoaded(), "Edit user page should be loaded");
    }

    @When("I update the user information:")
    public void i_update_the_user_information(DataTable dataTable) {
        Map<String, String> updatedInfo = dataTable.asMap();
        editUserPage.updateUserInfo(updatedInfo);
    }

    @When("I save the changes")
    public void i_save_the_changes() {
        userManagementPage = editUserPage.saveChanges();
    }

    @Then("the user details should be updated in the system")
    public void the_user_details_should_be_updated_in_the_system() {
        // Verify the changes are reflected in the user list
        String updatedName = userManagementPage.getUserName("john.doe@example.com");
        assertEquals("Jonathan", updatedName, "User name should be updated");
    }

    @When("I click the delete button for that user")
    public void i_click_the_delete_button_for_that_user() {
        userManagementPage.clickDeleteButton();
    }

    @When("I confirm the deletion")
    public void i_confirm_the_deletion() {
        userManagementPage.confirmDeletion();
    }

    @Then("the user {string} should not appear in the users list")
    public void the_user_should_not_appear_in_the_users_list(String userEmail) {
        assertFalse(userManagementPage.isUserPresent(userEmail),
            "User " + userEmail + " should not appear in the users list after deletion");
    }

    // Helper method to create test user
    private void createTestUser(String email) {
        CreateUserPage createPage = userManagementPage.clickAddUser();
        Map<String, String> testUser = Map.of(
            "firstName", "Test",
            "lastName", "User",
            "email", email,
            "role", "User"
        );
        createPage.fillUserForm(testUser);
        createPage.saveUser();
    }
}
```

---

## Hooks and Configuration

### Test Context

```java
package com.yourcompany.context;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.config.ReportingConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class TestContext {
    private WebDriver driver;
    private AutoHealLocator autoHeal;
    private String scenarioName;

    public void initialize(String scenarioName) {
        this.scenarioName = scenarioName;
        setupWebDriver();
        setupAutoHeal();
    }

    private void setupWebDriver() {
        ChromeOptions options = new ChromeOptions();

        // Configure based on environment
        if ("true".equals(System.getProperty("headless"))) {
            options.addArguments("--headless");
        }

        options.addArguments("--disable-web-security");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    private void setupAutoHeal() {
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(AIProvider.OPENAI)
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build())
            .reporting(ReportingConfig.builder()
                .enabled(true)
                .generateHTML(true)
                .generateJSON(true)
                .consoleLogging(true)
                .reportNamePrefix("Cucumber_" + scenarioName.replaceAll(" ", "_"))
                .build())
            .build();

        autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(config)
            .build();
    }

    public void cleanup() {
        if (autoHeal != null) {
            autoHeal.shutdown(); // Generate AutoHeal reports
        }
        if (driver != null) {
            driver.quit();
        }
    }

    // Getters
    public WebDriver getDriver() { return driver; }
    public AutoHealLocator getAutoHeal() { return autoHeal; }
    public String getScenarioName() { return scenarioName; }
}
```

### Cucumber Hooks

```java
package com.yourcompany.hooks;

import com.yourcompany.context.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

public class CucumberHooks {
    private TestContext testContext;

    public CucumberHooks(TestContext testContext) {
        this.testContext = testContext;
    }

    @Before
    public void setUp(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        testContext.initialize(scenario.getName());
    }

    @After
    public void tearDown(Scenario scenario) {
        // Take screenshot if scenario failed
        if (scenario.isFailed() && testContext.getDriver() != null) {
            try {
                byte[] screenshot = ((TakesScreenshot) testContext.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", "Screenshot");
            } catch (Exception e) {
                System.err.println("Failed to capture screenshot: " + e.getMessage());
            }
        }

        // Log scenario completion
        String status = scenario.isFailed() ? "FAILED" : "PASSED";
        System.out.println("Scenario " + scenario.getName() + " " + status);

        // Cleanup resources
        testContext.cleanup();
    }

    @Before("@smoke")
    public void beforeSmokeTest() {
        System.out.println("Running smoke test - ensuring critical functionality");
    }

    @Before("@admin")
    public void beforeAdminTest() {
        System.out.println("Running admin test - additional security checks may apply");
    }

    @After("@report")
    public void afterReportTest(Scenario scenario) {
        // Generate additional reports for scenarios tagged with @report
        System.out.println("Generating detailed report for: " + scenario.getName());
    }
}
```

### Configuration Properties

```java
package com.yourcompany.config;

public class TestConfig {
    public static final String BASE_URL = System.getProperty("base.url", "https://example.com");
    public static final String BROWSER = System.getProperty("browser", "chrome");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "false"));
    public static final int IMPLICIT_WAIT = Integer.parseInt(System.getProperty("implicit.wait", "10"));

    // AI Configuration
    public static final String AI_PROVIDER = System.getProperty("ai.provider", "OPENAI");
    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    // Environment-specific configurations
    public static String getLoginUrl() {
        return BASE_URL + "/login";
    }

    public static String getProductsUrl() {
        return BASE_URL + "/products";
    }

    public static String getAdminUrl() {
        return BASE_URL + "/admin";
    }
}
```

---

## Data Tables

### Using Data Tables Effectively

```gherkin
Feature: User Registration with Data Tables

  @registration
  Scenario: Register multiple users with different roles
    Given I am on the registration page
    When I create users with the following details:
      | firstName | lastName | email              | role  | department |
      | John      | Doe      | john@example.com   | Admin | IT         |
      | Jane      | Smith    | jane@example.com   | User  | Sales      |
      | Bob       | Johnson  | bob@example.com    | User  | Marketing  |
    Then all users should be created successfully
    And each user should have the correct role assigned

  @validation
  Scenario: Form validation with multiple field errors
    Given I am on the registration page
    When I submit the form with invalid data:
      | field     | value          | expectedError              |
      | email     | invalid-email  | Please enter a valid email |
      | password  | 123            | Password too short         |
      | phone     | abc123         | Invalid phone number       |
      | age       | 150            | Age must be realistic      |
    Then I should see all validation errors displayed
    And the form should not be submitted
```

```java
// Step definition for data tables
@When("I create users with the following details:")
public void i_create_users_with_the_following_details(DataTable dataTable) {
    List<Map<String, String>> users = dataTable.asMaps();

    for (Map<String, String> user : users) {
        // Navigate to add user page
        CreateUserPage createUserPage = userManagementPage.clickAddUser();

        // Fill form with user data
        createUserPage.fillUserForm(user);

        // Save user
        userManagementPage = createUserPage.saveUser();

        // Verify success message
        assertTrue(userManagementPage.getSuccessMessage().contains("created successfully"));
    }
}

@When("I submit the form with invalid data:")
public void i_submit_the_form_with_invalid_data(DataTable dataTable) {
    List<Map<String, String>> validationTests = dataTable.asMaps();

    for (Map<String, String> test : validationTests) {
        String field = test.get("field");
        String value = test.get("value");
        String expectedError = test.get("expectedError");

        // Clear form and enter invalid value
        registrationPage.clearForm();
        registrationPage.enterFieldValue(field, value);
        registrationPage.submitForm();

        // Verify error message
        String actualError = registrationPage.getFieldError(field);
        assertEquals(expectedError, actualError,
            "Validation error mismatch for field: " + field);
    }
}
```

---

## Scenario Outlines

### Parameterized Testing

```gherkin
Feature: Login with Multiple Credentials

  @parameterized
  Scenario Outline: Login attempts with different credentials
    Given I am on the login page
    When I enter username "<username>"
    And I enter password "<password>"
    And I click the login button
    Then I should see the result "<result>"
    And the page should be "<expectedPage>"

    Examples:
      | username           | password      | result                    | expectedPage |
      | admin@example.com  | validpass     | Welcome, Admin           | home         |
      | user@example.com   | validpass     | Welcome, User            | home         |
      | admin@example.com  | wrongpass     | Invalid credentials      | login        |
      | invalid@email      | validpass     | Invalid email format     | login        |
      |                    | validpass     | Username is required     | login        |
      | user@example.com   |               | Password is required     | login        |

  @boundary
  Scenario Outline: Password strength validation
    Given I am on the registration page
    When I enter password "<password>"
    Then the password strength should be "<strength>"
    And the registration button should be "<buttonState>"

    Examples:
      | password           | strength | buttonState |
      | 123                | Weak     | disabled    |
      | password123        | Fair     | disabled    |
      | Password123        | Good     | enabled     |
      | Password123!       | Strong   | enabled     |
      | P@ssw0rd123!       | Strong   | enabled     |

  @ecommerce
  Scenario Outline: Product search with different terms
    Given I am on the products page
    When I search for "<searchTerm>"
    Then I should see "<resultCount>" products
    And the first product should contain "<expectedText>"

    Examples:
      | searchTerm | resultCount | expectedText |
      | laptop     | 5           | Laptop       |
      | phone      | 8           | Phone        |
      | tablet     | 3           | Tablet       |
      | mouse      | 12          | Mouse        |
      | keyboard   | 6           | Keyboard     |
```

```java
// Step definitions for scenario outlines
@Then("I should see the result {string}")
public void i_should_see_the_result(String expectedResult) {
    if (expectedResult.startsWith("Welcome")) {
        // Success case
        HomePage homePage = new HomePage(driver, autoHeal);
        String welcomeMessage = homePage.getWelcomeMessage();
        assertTrue(welcomeMessage.contains(expectedResult.substring(0, 7)),
            "Welcome message should contain: " + expectedResult);
    } else {
        // Error case
        String actualError = loginPage.getErrorMessage();
        assertEquals(expectedResult, actualError, "Error message mismatch");
    }
}

@Then("the page should be {string}")
public void the_page_should_be(String expectedPage) {
    if ("home".equals(expectedPage)) {
        HomePage homePage = new HomePage(driver, autoHeal);
        assertTrue(homePage.isLoaded(), "Should be on home page");
    } else if ("login".equals(expectedPage)) {
        assertTrue(loginPage.isLoaded(), "Should remain on login page");
    }
}

@Then("the password strength should be {string}")
public void the_password_strength_should_be(String expectedStrength) {
    String actualStrength = registrationPage.getPasswordStrength();
    assertEquals(expectedStrength, actualStrength, "Password strength mismatch");
}

@Then("the registration button should be {string}")
public void the_registration_button_should_be(String expectedState) {
    boolean isEnabled = registrationPage.isRegistrationButtonEnabled();

    if ("enabled".equals(expectedState)) {
        assertTrue(isEnabled, "Registration button should be enabled");
    } else if ("disabled".equals(expectedState)) {
        assertFalse(isEnabled, "Registration button should be disabled");
    }
}
```

---

## Best Practices

### 1. Feature Organization

```
src/test/resources/features/
├── authentication/
│   ├── login.feature
│   ├── logout.feature
│   └── password-reset.feature
├── user-management/
│   ├── create-user.feature
│   ├── edit-user.feature
│   └── delete-user.feature
├── ecommerce/
│   ├── product-search.feature
│   ├── shopping-cart.feature
│   └── checkout.feature
└── admin/
    ├── dashboard.feature
    └── reports.feature
```

### 2. Meaningful Tags

```gherkin
@smoke @login @positive
Scenario: Successful login

@regression @user-management @negative
Scenario: Create user with duplicate email

@integration @ecommerce @payment
Scenario: Complete purchase flow

@security @admin @authorization
Scenario: Access control validation
```

### 3. Step Definition Organization

```java
// Organize by domain, not by page
package com.yourcompany.stepdefinitions;

// AuthenticationSteps.java - handles all auth-related steps
// UserManagementSteps.java - handles user CRUD operations
// EcommerceSteps.java - handles shopping and purchasing
// CommonSteps.java - handles shared/generic steps

public class CommonSteps {
    @Given("I wait for {int} seconds")
    public void i_wait_for_seconds(int seconds) {
        try { Thread.sleep(seconds * 1000); }
        catch (InterruptedException e) { /* handle */ }
    }

    @Then("the page title should contain {string}")
    public void the_page_title_should_contain(String expectedTitle) {
        assertTrue(driver.getTitle().contains(expectedTitle));
    }
}
```

### 4. Error Handling in Steps

```java
@When("I perform an action that might fail")
public void i_perform_an_action_that_might_fail() {
    try {
        // Primary action with AutoHeal
        WebElement element = autoHeal.findElement(".dynamic-selector", "target element");
        element.click();
    } catch (Exception e) {
        // Log the failure for debugging
        System.err.println("Primary action failed: " + e.getMessage());

        // Try fallback approach
        WebElement fallbackElement = autoHeal.findElement("#fallback-selector", "fallback element");
        fallbackElement.click();
    }
}
```

### 5. Reporting Integration

```java
// Enhanced hooks with AutoHeal reporting
@After
public void tearDown(Scenario scenario) {
    // Capture AutoHeal metrics
    if (testContext.getAutoHeal() != null) {
        AutoHealMetrics metrics = testContext.getAutoHeal().getMetrics();

        // Attach metrics to Cucumber report
        scenario.attach(
            metrics.toJson().getBytes(),
            "application/json",
            "AutoHeal Metrics"
        );

        // Log healing activities
        if (metrics.getHealingCount() > 0) {
            scenario.log("AutoHeal performed " + metrics.getHealingCount() + " healing operations");
        }
    }

    // Generate reports
    testContext.cleanup();
}
```

### 6. Environment Configuration

```gherkin
# Run specific environments
@dev
Scenario: Development environment test

@staging
Scenario: Staging environment test

@prod
Scenario: Production environment test
```

```java
// Environment-specific configuration
@Before
public void configureEnvironment(Scenario scenario) {
    String environment = System.getProperty("test.env", "dev");

    switch (environment) {
        case "dev":
            TestConfig.setBaseUrl("https://dev.example.com");
            break;
        case "staging":
            TestConfig.setBaseUrl("https://staging.example.com");
            break;
        case "prod":
            TestConfig.setBaseUrl("https://example.com");
            break;
    }
}
```

### 7. Parallel Execution

```xml
<!-- Maven Surefire Plugin Configuration -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.0.0-M9</version>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>3</threadCount>
        <properties>
            <configurationParameters>
                cucumber.execution.parallel.enabled=true
                cucumber.execution.parallel.config.strategy=dynamic
            </configurationParameters>
        </properties>
    </configuration>
</plugin>
```

This comprehensive Cucumber integration guide demonstrates how AutoHeal enhances BDD testing by providing reliable element location even when application selectors change, ensuring your behavior-driven tests remain stable and maintainable.