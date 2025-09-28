package com.example.tests;

import com.example.base.BaseTest;
import com.example.config.AutoHealConfigurationManager;
import com.example.pages.InventoryPage;
import com.example.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced Login Tests with AutoHeal capabilities and comprehensive validation
 */
@DisplayName("Enhanced Login Tests with AutoHeal")
public class EnhancedLoginTest extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLoginTest.class);

    private static final String BASE_URL = AutoHealConfigurationManager.getBaseUrl();
    private static final String VALID_USERNAME = "standard_user";
    private static final String VALID_PASSWORD = "secret_sauce";
    private static final String LOCKED_USERNAME = "locked_out_user";
    private static final String INVALID_USERNAME = "invalid_user";
    private static final String INVALID_PASSWORD = "invalid_pass";
    private static final String PROBLEM_USERNAME = "problem_user";
    private static final String PERFORMANCE_USERNAME = "performance_glitch_user";

    private LoginPage loginPage;
    private InventoryPage inventoryPage;

    @BeforeEach
    void setupPages(TestInfo testInfo) {
        logger.info("Setting up test: {}", testInfo.getDisplayName());
        
        loginPage = new LoginPage(page);
        inventoryPage = new InventoryPage(page);
        
        // Print AutoHeal configuration for the first test
        if (testInfo.getDisplayName().contains("Successful Login")) {
            AutoHealConfigurationManager.printConfiguration();
        }
    }

    @Test
    @DisplayName("Test Successful Login with Standard User")
    void testSuccessfulLoginWithStandardUser() {
        logger.info("=== Testing Successful Login with Standard User ===");
        
        // Navigate to login page
        loginPage.goToLoginPage(BASE_URL);

        // Verify login form is displayed and interactive
        assertTrue(loginPage.isLoginFormDisplayed(), "Login form should be displayed");
        assertTrue(loginPage.validateLoginFormInteractivity(), "Login form should be interactive");

        // Verify logo text
        assertEquals("Swag Labs", loginPage.getLogoText(), "Logo should display 'Swag Labs'");

        // Perform validated login
        LoginPage.LoginResult result = loginPage.performValidatedLogin(VALID_USERNAME, VALID_PASSWORD);
        
        // Verify login was successful
        assertTrue(result.isSuccessful(), "Login should be successful: " + result.getMessage());
        logger.info("Login result: {}", result);

        // Verify successful login - should be on inventory page
        assertTrue(inventoryPage.isInventoryPageLoaded(), "Should be redirected to inventory page");
        assertEquals("Products", inventoryPage.getPageTitle(), "Page title should be 'Products'");

        // Verify URL contains inventory
        assertTrue(page.url().contains("inventory.html"), "URL should contain inventory.html");
        
        // Verify inventory page functionality
        assertTrue(inventoryPage.verifyPageFunctionality(), "Inventory page should be fully functional");
        
        // Check that we have products loaded
        int itemCount = inventoryPage.getInventoryItemCount();
        assertTrue(itemCount > 0, "Should have inventory items loaded, found: " + itemCount);

        logger.info("=== Standard User Login Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Login with Invalid Username")
    void testLoginWithInvalidUsername() {
        logger.info("=== Testing Login with Invalid Username ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Attempt login with invalid username
        LoginPage.LoginResult result = loginPage.performValidatedLogin(INVALID_USERNAME, VALID_PASSWORD);

        // Verify login failed
        assertFalse(result.isSuccessful(), "Login should fail with invalid username");
        logger.info("Login result: {}", result);

        // Verify error message is displayed
        assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage.contains("Username and password do not match"),
                "Should show username/password mismatch error, got: " + errorMessage);

        // Verify still on login page
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page");
        assertTrue(loginPage.isLoginFormDisplayed(), "Login form should still be displayed");

        logger.info("=== Invalid Username Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Login with Invalid Password")
    void testLoginWithInvalidPassword() {
        logger.info("=== Testing Login with Invalid Password ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Attempt login with invalid password
        LoginPage.LoginResult result = loginPage.performValidatedLogin(VALID_USERNAME, INVALID_PASSWORD);

        // Verify login failed
        assertFalse(result.isSuccessful(), "Login should fail with invalid password");
        logger.info("Login result: {}", result);

        // Verify error message is displayed
        assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage.contains("Username and password do not match"),
                "Should show username/password mismatch error, got: " + errorMessage);

        logger.info("=== Invalid Password Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Login with Locked Out User")
    void testLoginWithLockedOutUser() {
        logger.info("=== Testing Login with Locked Out User ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Attempt login with locked out user
        LoginPage.LoginResult result = loginPage.performValidatedLogin(LOCKED_USERNAME, VALID_PASSWORD);

        // Verify login failed
        assertFalse(result.isSuccessful(), "Login should fail for locked out user");
        logger.info("Login result: {}", result);

        // Verify locked out error message
        assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage.contains("locked out"),
                "Should show locked out error message, got: " + errorMessage);

        // Verify still on login page
        assertTrue(loginPage.isOnLoginPage(), "Should remain on login page");

        logger.info("=== Locked Out User Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Problem User Login and Inventory")
    void testProblemUserLoginAndInventory() {
        logger.info("=== Testing Problem User Login and Inventory ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Login with problem user (this user has visual issues but login works)
        LoginPage.LoginResult result = loginPage.performValidatedLogin(PROBLEM_USERNAME, VALID_PASSWORD);
        
        // Login should succeed despite visual problems
        assertTrue(result.isSuccessful(), "Problem user login should succeed: " + result.getMessage());

        // Verify we're on inventory page
        assertTrue(inventoryPage.isInventoryPageLoaded(), "Should be on inventory page");
        
        // Test that AutoHeal handles the visual problems
        int itemCount = inventoryPage.getInventoryItemCount();
        logger.info("Problem user sees {} inventory items", itemCount);
        
        // Try to get product names (problem user might see broken images)
        var productNames = inventoryPage.getAllProductNames();
        assertFalse(productNames.isEmpty(), "Should be able to get product names even with visual problems");
        
        logger.info("Problem user can see products: {}", productNames);

        logger.info("=== Problem User Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Performance User Login")
    void testPerformanceUserLogin() {
        logger.info("=== Testing Performance User Login (slower response) ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Login with performance user (this user has slower responses)
        long startTime = System.currentTimeMillis();
        LoginPage.LoginResult result = loginPage.performValidatedLogin(PERFORMANCE_USERNAME, VALID_PASSWORD);
        long endTime = System.currentTimeMillis();
        
        long loginDuration = endTime - startTime;
        logger.info("Performance user login took {} ms", loginDuration);

        // Login should succeed despite performance issues
        assertTrue(result.isSuccessful(), "Performance user login should succeed: " + result.getMessage());

        // Verify we're on inventory page
        assertTrue(inventoryPage.isInventoryPageLoaded(), "Should be on inventory page");
        
        // Test inventory operations with performance user
        int itemCount = inventoryPage.getInventoryItemCount();
        assertTrue(itemCount > 0, "Should have inventory items loaded");
        
        logger.info("Performance user login completed in {} ms with {} items", loginDuration, itemCount);

        logger.info("=== Performance User Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Login Form Field Clearing")
    void testLoginFormFieldClearing() {
        logger.info("=== Testing Login Form Field Clearing ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Enter some text in fields
        loginPage.enterUsername(INVALID_USERNAME);
        loginPage.enterPassword(INVALID_PASSWORD);

        // Clear the fields
        loginPage.clearFields();

        // Try to login (should fail because fields are empty)
        loginPage.clickLoginButton();

        // Verify error message about required fields
        assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed for empty fields");
        
        String errorMessage = loginPage.getErrorMessage();
        assertTrue(errorMessage.toLowerCase().contains("username") || errorMessage.toLowerCase().contains("required"),
                "Should show username required error, got: " + errorMessage);

        logger.info("=== Field Clearing Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test Multiple Login Attempts with AutoHeal")
    void testMultipleLoginAttemptsWithAutoHeal() {
        logger.info("=== Testing Multiple Login Attempts (AutoHeal resilience) ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // First attempt with invalid credentials
        logger.info("Attempt 1: Invalid credentials");
        LoginPage.LoginResult result1 = loginPage.performValidatedLogin(INVALID_USERNAME, INVALID_PASSWORD);
        assertFalse(result1.isSuccessful(), "First attempt should fail");
        logger.info("Attempt 1 result: {}", result1);

        // Clear any error message by clearing fields
        loginPage.clearFields();

        // Second attempt with valid credentials
        logger.info("Attempt 2: Valid credentials");
        LoginPage.LoginResult result2 = loginPage.performValidatedLogin(VALID_USERNAME, VALID_PASSWORD);
        assertTrue(result2.isSuccessful(), "Second attempt should succeed: " + result2.getMessage());
        logger.info("Attempt 2 result: {}", result2);

        // Verify successful navigation to inventory
        assertTrue(inventoryPage.isInventoryPageLoaded(), "Should be on inventory page after successful login");

        logger.info("=== Multiple Login Attempts Test Completed Successfully ===");
    }

    @Test
    @DisplayName("Test AutoHeal Health Monitoring")
    void testAutoHealHealthMonitoring() {
        logger.info("=== Testing AutoHeal Health Monitoring ===");
        
        loginPage.goToLoginPage(BASE_URL);

        // Check initial AutoHeal health
        boolean initialHealth = loginPage.isAutoHealHealthy();
        logger.info("Initial AutoHeal health: {}", initialHealth);

        // Perform some operations to generate AutoHeal activity
        loginPage.performValidatedLogin(VALID_USERNAME, VALID_PASSWORD);
        
        // Check AutoHeal metrics
        String metrics = loginPage.getAutoHealMetrics();
        logger.info("AutoHeal metrics after login: {}", metrics);
        
        assertNotNull(metrics, "AutoHeal metrics should be available");
        assertTrue(metrics.contains("Success Rate") || metrics.contains("Cache Hit Rate"), 
                "Metrics should contain performance information");

        // Check final AutoHeal health
        boolean finalHealth = loginPage.isAutoHealHealthy();
        logger.info("Final AutoHeal health: {}", finalHealth);

        logger.info("=== AutoHeal Health Monitoring Test Completed Successfully ===");
    }
}