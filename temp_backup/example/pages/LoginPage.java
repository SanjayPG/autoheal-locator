package com.example.pages;

import com.example.base.BasePage;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced LoginPage with AutoHeal capabilities for SauceDemo
 */
public class LoginPage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(LoginPage.class);

    // Selectors for SauceDemo with semantic descriptions
    private static final String USERNAME_FIELD = "#user-name";
    private static final String PASSWORD_FIELD = "#password";
    private static final String LOGIN_BUTTON = "#login-button";
    private static final String ERROR_MESSAGE = "[data-test='error']";
    private static final String LOGO = ".login_logo";
    private static final String CREDENTIALS_INFO = "#login_credentials";

    // Element descriptions for AutoHeal
    private static final String USERNAME_DESCRIPTION = "Username input field for login";
    private static final String PASSWORD_DESCRIPTION = "Password input field for login";
    private static final String LOGIN_BUTTON_DESCRIPTION = "Login submit button";
    private static final String ERROR_MESSAGE_DESCRIPTION = "Error message container for login failures";
    private static final String LOGO_DESCRIPTION = "SauceDemo logo on login page";
    private static final String CREDENTIALS_DESCRIPTION = "Available test credentials information";

    public LoginPage(Page page) {
        super(page);
        logger.info("LoginPage initialized with AutoHeal capabilities");
    }

    /**
     * Navigate to login page with URL validation
     */
    public LoginPage goToLoginPage(String baseUrl) {
        navigateTo(baseUrl);
        waitForPageLoad();
        
        // Verify we're on the login page by checking for logo
        if (!isLoginFormDisplayed()) {
            logger.warn("Login form not displayed after navigation to: {}", baseUrl);
            throw new RuntimeException("Failed to load login page correctly");
        }
        
        logger.info("Successfully navigated to login page: {}", baseUrl);
        return this;
    }

    /**
     * Enter username with AutoHeal
     */
    public LoginPage enterUsername(String username) {
        logger.debug("Entering username: {}", username);
        waitForElement(USERNAME_FIELD, USERNAME_DESCRIPTION);
        type(USERNAME_FIELD, username, USERNAME_DESCRIPTION);
        return this;
    }

    /**
     * Enter password with AutoHeal
     */
    public LoginPage enterPassword(String password) {
        logger.debug("Entering password");
        type(PASSWORD_FIELD, password, PASSWORD_DESCRIPTION);
        return this;
    }

    /**
     * Click login button with AutoHeal
     */
    public void clickLoginButton() {
        logger.debug("Clicking login button");
        click(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION);
        
        // Wait a moment for page transition or error message
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform complete login with credentials
     */
    public void login(String username, String password) {
        logger.info("Performing login with username: {}", username);
        enterUsername(username)
                .enterPassword(password)
                .clickLoginButton();
        
        // Log AutoHeal metrics after login attempt
        logger.debug("Login attempt completed. {}", getAutoHealMetrics());
    }

    /**
     * Get error message with AutoHeal
     */
    public String getErrorMessage() {
        logger.debug("Retrieving error message");
        waitForElement(ERROR_MESSAGE, ERROR_MESSAGE_DESCRIPTION);
        String errorText = getText(ERROR_MESSAGE, ERROR_MESSAGE_DESCRIPTION);
        logger.info("Error message retrieved: {}", errorText);
        return errorText;
    }

    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        boolean isDisplayed = isVisible(ERROR_MESSAGE, ERROR_MESSAGE_DESCRIPTION);
        logger.debug("Error message displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Get logo text with AutoHeal
     */
    public String getLogoText() {
        logger.debug("Retrieving logo text");
        waitForElement(LOGO, LOGO_DESCRIPTION);
        String logoText = getText(LOGO, LOGO_DESCRIPTION);
        logger.debug("Logo text: {}", logoText);
        return logoText;
    }

    /**
     * Check if login form is completely displayed
     */
    public boolean isLoginFormDisplayed() {
        boolean formDisplayed = isVisible(USERNAME_FIELD, USERNAME_DESCRIPTION) &&
                isVisible(PASSWORD_FIELD, PASSWORD_DESCRIPTION) &&
                isVisible(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION);
        
        logger.debug("Login form displayed: {}", formDisplayed);
        return formDisplayed;
    }

    /**
     * Get available test credentials with AutoHeal
     */
    public String getCredentialsInfo() {
        logger.debug("Retrieving credentials information");
        waitForElement(CREDENTIALS_INFO, CREDENTIALS_DESCRIPTION);
        String credInfo = getText(CREDENTIALS_INFO, CREDENTIALS_DESCRIPTION);
        logger.debug("Credentials info retrieved");
        return credInfo;
    }

    /**
     * Clear input fields with AutoHeal
     */
    public LoginPage clearFields() {
        logger.debug("Clearing login form fields");
        
        if (isVisible(USERNAME_FIELD, USERNAME_DESCRIPTION)) {
            type(USERNAME_FIELD, "", USERNAME_DESCRIPTION);
        }
        
        if (isVisible(PASSWORD_FIELD, PASSWORD_DESCRIPTION)) {
            type(PASSWORD_FIELD, "", PASSWORD_DESCRIPTION);
        }
        
        logger.debug("Login form fields cleared");
        return this;
    }

    /**
     * Validate login form elements are interactive
     */
    public boolean validateLoginFormInteractivity() {
        logger.debug("Validating login form interactivity");
        
        boolean usernameEnabled = isEnabled(USERNAME_FIELD, USERNAME_DESCRIPTION);
        boolean passwordEnabled = isEnabled(PASSWORD_FIELD, PASSWORD_DESCRIPTION);
        boolean loginButtonEnabled = isEnabled(LOGIN_BUTTON, LOGIN_BUTTON_DESCRIPTION);
        
        boolean allInteractive = usernameEnabled && passwordEnabled && loginButtonEnabled;
        
        logger.info("Login form interactivity validation: username={}, password={}, button={}, overall={}", 
                usernameEnabled, passwordEnabled, loginButtonEnabled, allInteractive);
                
        return allInteractive;
    }

    /**
     * Get current page URL for validation
     */
    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }

    /**
     * Check if we're still on login page (useful after failed login)
     */
    public boolean isOnLoginPage() {
        String currentUrl = getCurrentUrl();
        boolean onLoginPage = currentUrl.contains("saucedemo.com") && 
                             !currentUrl.contains("inventory");
        
        logger.debug("Currently on login page: {} (URL: {})", onLoginPage, currentUrl);
        return onLoginPage;
    }

    /**
     * Perform login with validation and detailed logging
     */
    public LoginResult performValidatedLogin(String username, String password) {
        logger.info("Starting validated login process for user: {}", username);
        
        // Validate form before attempting login
        if (!isLoginFormDisplayed()) {
            return new LoginResult(false, "Login form not displayed", getAutoHealMetrics());
        }
        
        if (!validateLoginFormInteractivity()) {
            return new LoginResult(false, "Login form elements not interactive", getAutoHealMetrics());
        }
        
        // Perform login
        login(username, password);
        
        // Check for errors
        if (isErrorMessageDisplayed()) {
            String errorMessage = getErrorMessage();
            logger.warn("Login failed with error: {}", errorMessage);
            return new LoginResult(false, errorMessage, getAutoHealMetrics());
        }
        
        // Check if we've successfully navigated away from login page
        boolean loginSuccessful = !isOnLoginPage();
        String message = loginSuccessful ? "Login successful" : "Login failed - still on login page";
        
        logger.info("Login validation completed: {} for user: {}", message, username);
        return new LoginResult(loginSuccessful, message, getAutoHealMetrics());
    }

    /**
     * Result class for detailed login validation
     */
    public static class LoginResult {
        private final boolean successful;
        private final String message;
        private final String autoHealMetrics;

        public LoginResult(boolean successful, String message, String autoHealMetrics) {
            this.successful = successful;
            this.message = message;
            this.autoHealMetrics = autoHealMetrics;
        }

        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public String getAutoHealMetrics() { return autoHealMetrics; }

        @Override
        public String toString() {
            return String.format("LoginResult{successful=%s, message='%s', metrics='%s'}", 
                    successful, message, autoHealMetrics);
        }
    }
}