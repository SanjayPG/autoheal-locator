# Selenium Integration Examples

This page provides comprehensive examples of using AutoHeal with Selenium WebDriver for various web automation scenarios.

## Table of Contents

- [Basic Setup](#basic-setup)
- [Form Interactions](#form-interactions)
- [Navigation & Links](#navigation--links)
- [Dynamic Content](#dynamic-content)
- [File Operations](#file-operations)
- [Advanced Interactions](#advanced-interactions)
- [Error Handling](#error-handling)

---

## Basic Setup

### Simple Test Class

```java
import com.autoheal.AutoHealLocator;
import com.autoheal.config.AutoHealConfiguration;
import com.autoheal.config.AIConfig;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import com.autoheal.model.AIProvider;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class BasicAutoHealTest {
    private WebDriver driver;
    private AutoHealLocator autoHeal;

    @BeforeEach
    void setUp() {
        // Initialize WebDriver
        driver = new ChromeDriver();

        // Configure AutoHeal
        AutoHealConfiguration config = AutoHealConfiguration.builder()
            .ai(AIConfig.builder()
                .provider(AIProvider.OPENAI)
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build())
            .build();

        // Initialize AutoHeal
        autoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(config)
            .build();
    }

    @Test
    void testBasicInteraction() {
        driver.get("https://example.com");

        // AutoHeal automatically detects selector types
        WebElement heading = autoHeal.findElement("h1", "main page heading");
        assertEquals("Welcome", heading.getText());

        WebElement button = autoHeal.findElement(".btn-primary", "primary action button");
        button.click();
    }

    @AfterEach
    void tearDown() {
        autoHeal.shutdown(); // Important: generates reports
        driver.quit();
    }
}
```

---

## Form Interactions

### Text Inputs

```java
@Test
void testTextInputs() {
    driver.get("https://forms.example.com/registration");

    // Different selector types automatically detected
    WebElement firstName = autoHeal.findElement("#firstName", "first name input field");
    firstName.sendKeys("John");

    WebElement lastName = autoHeal.findElement("//input[@name='lastName']", "last name input field");
    lastName.sendKeys("Doe");

    WebElement email = autoHeal.findElement("email", "email address input field");
    email.sendKeys("john.doe@example.com");

    // Clear and re-enter
    WebElement phone = autoHeal.findElement("input[type='tel']", "phone number input");
    phone.clear();
    phone.sendKeys("+1234567890");

    // Verify input values
    assertEquals("John", firstName.getAttribute("value"));
    assertEquals("john.doe@example.com", email.getAttribute("value"));
}
```

### Password Fields

```java
@Test
void testPasswordFields() {
    driver.get("https://account.example.com/signup");

    // Password creation form
    WebElement password = autoHeal.findElement("#new-password", "new password input field");
    password.sendKeys("SecurePassword123!");

    WebElement confirmPassword = autoHeal.findElement("#confirm-password", "confirm password input field");
    confirmPassword.sendKeys("SecurePassword123!");

    // Check password strength indicator
    WebElement strengthIndicator = autoHeal.findElement(".password-strength", "password strength indicator");
    assertTrue(strengthIndicator.getText().contains("Strong"));

    // Show/hide password toggle
    WebElement toggleButton = autoHeal.findElement("//button[@aria-label='Show password']", "show password toggle");
    toggleButton.click();

    assertEquals("text", password.getAttribute("type")); // Password now visible
}
```

### Checkboxes and Radio Buttons

```java
@Test
void testCheckboxesAndRadios() {
    driver.get("https://survey.example.com");

    // Single checkbox
    WebElement agreeTerms = autoHeal.findElement("#agree-terms", "terms and conditions checkbox");
    if (!agreeTerms.isSelected()) {
        agreeTerms.click();
    }
    assertTrue(agreeTerms.isSelected());

    // Multiple checkboxes - Using findElements for collections
    List<WebElement> allCheckboxes = autoHeal.findElements("input[type='checkbox']", "all interest checkboxes");
    System.out.println("Found " + allCheckboxes.size() + " checkboxes");

    // Click first 3 checkboxes
    for (int i = 0; i < Math.min(3, allCheckboxes.size()); i++) {
        WebElement checkbox = allCheckboxes.get(i);
        if (!checkbox.isSelected()) {
            checkbox.click();
            assertTrue(checkbox.isSelected());
        }
    }

    // Alternative: Individual checkbox selection
    List<String> interests = Arrays.asList("Technology", "Sports", "Music");
    for (String interest : interests) {
        WebElement checkbox = autoHeal.findElement(
            "//input[@type='checkbox' and @value='" + interest.toLowerCase() + "']",
            interest + " interest checkbox"
        );
        checkbox.click();
        assertTrue(checkbox.isSelected());
    }

    // Radio buttons
    WebElement genderMale = autoHeal.findElement("input[name='gender'][value='male']", "male gender radio button");
    genderMale.click();

    // Verify only one radio is selected
    WebElement genderFemale = autoHeal.findElement("input[name='gender'][value='female']", "female gender radio button");
    assertTrue(genderMale.isSelected());
    assertFalse(genderFemale.isSelected());
}
```

### Dropdowns and Select Elements

```java
@Test
void testDropdowns() {
    driver.get("https://checkout.example.com");

    // Standard HTML select
    WebElement countrySelect = autoHeal.findElement("select[name='country']", "country selection dropdown");
    Select selectCountry = new Select(countrySelect);

    // Different selection methods
    selectCountry.selectByVisibleText("United States");
    assertEquals("US", selectCountry.getFirstSelectedOption().getAttribute("value"));

    selectCountry.selectByValue("CA");
    assertEquals("Canada", selectCountry.getFirstSelectedOption().getText());

    selectCountry.selectByIndex(0);
    assertTrue(selectCountry.getFirstSelectedOption().isSelected());

    // Multi-select dropdown
    WebElement skillsSelect = autoHeal.findElement("#skills", "skills multi-select dropdown");
    Select multiSelect = new Select(skillsSelect);
    assertTrue(multiSelect.isMultiple());

    multiSelect.selectByVisibleText("Java");
    multiSelect.selectByVisibleText("Python");
    multiSelect.selectByVisibleText("JavaScript");

    assertEquals(3, multiSelect.getAllSelectedOptions().size());

    // Custom dropdown (div-based)
    WebElement customDropdown = autoHeal.findElement(".custom-dropdown-trigger", "custom dropdown menu");
    customDropdown.click();

    WebElement option = autoHeal.findElement("//div[@data-value='premium']", "premium plan option");
    option.click();

    // Verify selection
    WebElement selectedValue = autoHeal.findElement(".custom-dropdown-trigger .selected-text", "selected dropdown value");
    assertEquals("Premium Plan", selectedValue.getText());
}
```

### Working with Multiple Elements

```java
@Test
void testMultipleElements() {
    driver.get("https://catalog.example.com");

    // Find all product cards - Synchronous (simple and direct)
    List<WebElement> products = autoHeal.findElements(".product-card", "product cards on catalog page");
    System.out.println("Found " + products.size() + " products");

    // Process each product
    for (int i = 0; i < products.size(); i++) {
        WebElement product = products.get(i);
        WebElement title = product.findElement(By.className("product-title"));
        WebElement price = product.findElement(By.className("product-price"));

        System.out.println("Product " + (i + 1) + ": " + title.getText() + " - " + price.getText());
    }

    // Find all navigation menu items
    List<WebElement> menuItems = autoHeal.findElements(".nav-item", "navigation menu items");
    for (WebElement item : menuItems) {
        System.out.println("Menu: " + item.getText());
    }

    // Table rows processing
    List<WebElement> tableRows = autoHeal.findElements("//table[@id='results']//tr", "result table rows");
    System.out.println("Processing " + tableRows.size() + " table rows");

    for (WebElement row : tableRows) {
        List<WebElement> cells = row.findElements(By.tagName("td"));
        if (cells.size() >= 3) {
            String name = cells.get(0).getText();
            String email = cells.get(1).getText();
            String status = cells.get(2).getText();
            System.out.println(name + " | " + email + " | " + status);
        }
    }

    // Form inputs validation
    List<WebElement> formInputs = autoHeal.findElements("input", "all form input fields");
    for (WebElement input : formInputs) {
        if (input.getAttribute("required") != null) {
            System.out.println("Required field: " + input.getAttribute("name"));

            // Highlight required fields
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border='2px solid red'", input);
        }
    }

    // Button collections
    List<WebElement> actionButtons = autoHeal.findElements(".action-btn", "action buttons in toolbar");
    System.out.println("Found " + actionButtons.size() + " action buttons");

    // Click buttons with confirmation
    for (WebElement button : actionButtons) {
        if (button.isEnabled() && button.getText().equals("Preview")) {
            button.click();

            // Wait for preview to load
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("preview-panel")));
            break;
        }
    }
}

@Test
void testAsyncMultipleElements() {
    driver.get("https://dashboard.example.com");

    // Asynchronous approach for better performance
    CompletableFuture<List<WebElement>> cardsFuture =
        autoHeal.findElementsAsync(".dashboard-card", "dashboard summary cards");

    CompletableFuture<List<WebElement>> chartsFuture =
        autoHeal.findElementsAsync(".chart-container", "analytics charts");

    // Process both results when ready
    CompletableFuture.allOf(cardsFuture, chartsFuture).thenRun(() -> {
        try {
            List<WebElement> cards = cardsFuture.get();
            List<WebElement> charts = chartsFuture.get();

            System.out.println("Dashboard loaded with " + cards.size() +
                             " cards and " + charts.size() + " charts");

            // Process dashboard elements
            for (WebElement card : cards) {
                String title = card.findElement(By.className("card-title")).getText();
                String value = card.findElement(By.className("card-value")).getText();
                System.out.println(title + ": " + value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }).join();
}
```

---

## Navigation & Links

### Link Navigation

```java
@Test
void testLinkNavigation() {
    driver.get("https://news.example.com");

    // Navigate by link text
    WebElement homeLink = autoHeal.findElement("Home", "home navigation link");
    homeLink.click();
    assertTrue(driver.getCurrentUrl().contains("/home"));

    // Partial link text
    WebElement aboutLink = autoHeal.findElement("About Us", "about page link");
    aboutLink.click();
    assertTrue(driver.getTitle().contains("About"));

    // Link with specific attributes
    WebElement contactLink = autoHeal.findElement("//a[@href='/contact']", "contact page link");
    contactLink.click();

    // External link (opens in new tab)
    String originalWindow = driver.getWindowHandle();
    WebElement externalLink = autoHeal.findElement("//a[@target='_blank']", "external link opening in new tab");
    externalLink.click();

    // Switch to new tab
    Set<String> windows = driver.getWindowHandles();
    for (String window : windows) {
        if (!window.equals(originalWindow)) {
            driver.switchTo().window(window);
            break;
        }
    }

    // Verify new tab content
    WebElement newTabContent = autoHeal.findElement("h1", "main heading in new tab");
    assertNotNull(newTabContent);

    // Close new tab and return
    driver.close();
    driver.switchTo().window(originalWindow);
}
```

### Breadcrumb Navigation

```java
@Test
void testBreadcrumbNavigation() {
    driver.get("https://shop.example.com/electronics/phones/smartphones");

    // Navigate through breadcrumbs
    WebElement homecrumb = autoHeal.findElement("//nav[@aria-label='breadcrumb']//a[text()='Home']", "home breadcrumb");
    homecrumb.click();
    assertEquals("https://shop.example.com/", driver.getCurrentUrl());

    // Go back to category
    driver.navigate().back();
    WebElement categorycrumb = autoHeal.findElement(".breadcrumb-item a[href*='electronics']", "electronics category breadcrumb");
    categorycrumb.click();
    assertTrue(driver.getCurrentUrl().contains("/electronics"));
}
```

---

## Dynamic Content

### AJAX and Loading States

```java
@Test
void testDynamicContent() {
    driver.get("https://app.example.com/dashboard");

    // Trigger AJAX request
    WebElement loadDataButton = autoHeal.findElement("#load-data-btn", "load data button");
    loadDataButton.click();

    // Wait for loading indicator to appear
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement loadingSpinner = autoHeal.findElement(".loading-spinner", "data loading indicator");
    assertTrue(loadingSpinner.isDisplayed());

    // Wait for loading to complete
    wait.until(ExpectedConditions.invisibilityOf(loadingSpinner));

    // Verify loaded content
    WebElement dataTable = autoHeal.findElement("#data-table", "dynamically loaded data table");
    List<WebElement> rows = dataTable.findElements(By.tagName("tr"));
    assertTrue(rows.size() > 1); // Header + data rows

    // Test infinite scroll
    WebElement lastItem = autoHeal.findElement(".data-item:last-child", "last item in scrollable list");
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", lastItem);

    // Wait for new items to load
    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        LocatorTypeDetector.autoCreateBy(".data-item"),
        rows.size()
    ));
}
```

### Real-time Updates

```java
@Test
void testRealTimeUpdates() {
    driver.get("https://live.example.com/stock-ticker");

    // Initial stock price
    WebElement stockPrice = autoHeal.findElement("#AAPL-price", "Apple stock price");
    String initialPrice = stockPrice.getText();

    // Wait for price update (real-time data)
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(driver -> {
        WebElement currentPrice = autoHeal.findElement("#AAPL-price", "Apple stock price");
        return !initialPrice.equals(currentPrice.getText());
    });

    // Verify price changed
    String updatedPrice = stockPrice.getText();
    assertNotEquals(initialPrice, updatedPrice);

    // Test live notifications
    WebElement notificationArea = autoHeal.findElement(".notifications", "live notification area");
    wait.until(ExpectedConditions.textToBePresentInElement(notificationArea, "Price Alert"));
}
```

### Modal Dialogs

```java
@Test
void testModalDialogs() {
    driver.get("https://admin.example.com/users");

    // Open modal dialog
    WebElement addUserButton = autoHeal.findElement("Add User", "add new user button");
    addUserButton.click();

    // Wait for modal to appear
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
        LocatorTypeDetector.autoCreateBy("#user-modal", "user creation modal dialog")
    ));

    // Fill modal form
    WebElement modalUsername = autoHeal.findElement("#modal-username", "username field in modal");
    modalUsername.sendKeys("newuser");

    WebElement modalEmail = autoHeal.findElement("#modal-email", "email field in modal");
    modalEmail.sendKeys("newuser@example.com");

    // Submit modal
    WebElement saveButton = autoHeal.findElement("//div[@id='user-modal']//button[text()='Save']", "save button in modal");
    saveButton.click();

    // Wait for modal to close
    wait.until(ExpectedConditions.invisibilityOf(modal));

    // Verify user was added
    WebElement userList = autoHeal.findElement("#users-table", "users table");
    assertTrue(userList.getText().contains("newuser"));
}
```

---

## File Operations

### File Upload

```java
@Test
void testFileUpload() {
    driver.get("https://upload.example.com");

    // Simple file input
    WebElement fileInput = autoHeal.findElement("input[type='file']", "file upload input");
    String filePath = System.getProperty("user.dir") + "/testfiles/sample.pdf";
    fileInput.sendKeys(filePath);

    // Verify file selected
    String fileName = fileInput.getAttribute("value");
    assertTrue(fileName.contains("sample.pdf"));

    // Upload button
    WebElement uploadButton = autoHeal.findElement("#upload-btn", "start upload button");
    uploadButton.click();

    // Wait for upload progress
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement progressBar = autoHeal.findElement(".upload-progress", "upload progress bar");

    // Wait for upload completion
    wait.until(ExpectedConditions.textToBePresentInElement(progressBar, "100%"));

    // Verify success message
    WebElement successMessage = autoHeal.findElement(".upload-success", "upload success notification");
    assertTrue(successMessage.getText().contains("Upload completed"));
}
```

### Multiple File Upload

```java
@Test
void testMultipleFileUpload() {
    driver.get("https://gallery.example.com/upload");

    // Multiple file input
    WebElement multiFileInput = autoHeal.findElement("input[type='file'][multiple]", "multiple files upload input");

    String file1 = System.getProperty("user.dir") + "/testfiles/image1.jpg";
    String file2 = System.getProperty("user.dir") + "/testfiles/image2.png";
    String files = file1 + "\n" + file2; // Newline-separated for multiple files

    multiFileInput.sendKeys(files);

    // Verify both files selected
    WebElement fileList = autoHeal.findElement(".selected-files", "list of selected files");
    assertTrue(fileList.getText().contains("image1.jpg"));
    assertTrue(fileList.getText().contains("image2.png"));

    // Upload all files
    WebElement uploadAllButton = autoHeal.findElement("Upload All", "upload all files button");
    uploadAllButton.click();

    // Monitor upload progress for each file
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    List<WebElement> progressBars = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
        LocatorTypeDetector.autoCreateBy(".file-progress"), 0
    ));

    assertEquals(2, progressBars.size());

    // Wait for all uploads to complete
    wait.until(driver -> {
        List<WebElement> completedUploads = driver.findElements(
            LocatorTypeDetector.autoCreateBy(".file-progress.completed")
        );
        return completedUploads.size() == 2;
    });
}
```

### Download Files

```java
@Test
void testFileDownload() {
    // Set download directory
    String downloadDir = System.getProperty("user.dir") + "/downloads";
    new File(downloadDir).mkdirs();

    ChromeOptions options = new ChromeOptions();
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("download.default_directory", downloadDir);
    options.setExperimentalOption("prefs", prefs);

    WebDriver downloadDriver = new ChromeDriver(options);
    AutoHealLocator downloadAutoHeal = AutoHealLocator.builder()
        .withWebAdapter(new SeleniumWebAutomationAdapter(downloadDriver))
        .build();

    try {
        downloadDriver.get("https://files.example.com");

        // Click download link
        WebElement downloadLink = downloadAutoHeal.findElement(
            "//a[contains(@href, '.pdf')]",
            "PDF file download link"
        );
        downloadLink.click();

        // Wait for download to complete
        File downloadedFile = new File(downloadDir, "sample.pdf");
        WebDriverWait wait = new WebDriverWait(downloadDriver, Duration.ofSeconds(30));
        wait.until(driver -> downloadedFile.exists() && downloadedFile.length() > 0);

        assertTrue(downloadedFile.exists());
        assertTrue(downloadedFile.length() > 1000); // File has content

    } finally {
        downloadAutoHeal.shutdown();
        downloadDriver.quit();
    }
}
```

---

## Advanced Interactions

### Drag and Drop

```java
@Test
void testDragAndDrop() {
    driver.get("https://dragdrop.example.com");

    Actions actions = new Actions(driver);

    // Simple drag and drop
    WebElement sourceElement = autoHeal.findElement("#draggable-1", "first draggable element");
    WebElement targetElement = autoHeal.findElement("#drop-zone", "drop target area");

    actions.dragAndDrop(sourceElement, targetElement).perform();

    // Verify drop success
    WebElement dropZoneContent = autoHeal.findElement("#drop-zone .dropped-item", "dropped item in target zone");
    assertTrue(dropZoneContent.isDisplayed());

    // Complex drag and drop with offset
    WebElement complexSource = autoHeal.findElement(".complex-draggable", "complex draggable element");
    WebElement complexTarget = autoHeal.findElement(".complex-target", "complex target with specific position");

    actions.clickAndHold(complexSource)
           .moveToElement(complexTarget, 50, 25) // Offset within target
           .release()
           .perform();

    // File drag and drop simulation
    WebElement fileDropZone = autoHeal.findElement(".file-drop-zone", "file drop zone area");

    // JavaScript-based file drop (for testing file drag-drop interfaces)
    String jsDropFile = """
        var dt = new DataTransfer();
        dt.items.add(new File(['file content'], 'test.txt', {type: 'text/plain'}));
        var event = new DragEvent('drop', {dataTransfer: dt});
        arguments[0].dispatchEvent(event);
        """;

    ((JavascriptExecutor) driver).executeScript(jsDropFile, fileDropZone);

    // Verify file drop success
    WebElement droppedFile = autoHeal.findElement(".dropped-file-info", "information about dropped file");
    assertTrue(droppedFile.getText().contains("test.txt"));
}
```

### Mouse Actions

```java
@Test
void testMouseActions() {
    driver.get("https://interactive.example.com");

    Actions actions = new Actions(driver);

    // Hover to reveal submenu
    WebElement mainMenu = autoHeal.findElement(".main-menu-item", "main navigation menu item");
    actions.moveToElement(mainMenu).perform();

    WebElement submenu = autoHeal.findElement(".submenu", "submenu appearing on hover");
    assertTrue(submenu.isDisplayed());

    WebElement submenuItem = autoHeal.findElement(".submenu-item", "item in submenu");
    submenuItem.click();

    // Right-click context menu
    WebElement contextArea = autoHeal.findElement("#context-area", "area with right-click context menu");
    actions.contextClick(contextArea).perform();

    WebElement contextMenu = autoHeal.findElement(".context-menu", "right-click context menu");
    assertTrue(contextMenu.isDisplayed());

    WebElement contextOption = autoHeal.findElement(".context-menu-option", "option in context menu");
    contextOption.click();

    // Double-click action
    WebElement doubleClickTarget = autoHeal.findElement(".double-click-target", "element requiring double-click");
    actions.doubleClick(doubleClickTarget).perform();

    WebElement doubleClickResult = autoHeal.findElement(".double-click-result", "result of double-click action");
    assertTrue(doubleClickResult.getText().contains("Double-clicked"));

    // Click and hold
    WebElement holdTarget = autoHeal.findElement(".hold-target", "element for click and hold");
    actions.clickAndHold(holdTarget).perform();

    // Wait for hold effect
    Thread.sleep(2000);

    WebElement holdEffect = autoHeal.findElement(".hold-effect", "effect triggered by holding");
    assertTrue(holdEffect.isDisplayed());

    actions.release().perform();
}
```

### Keyboard Actions

```java
@Test
void testKeyboardActions() {
    driver.get("https://editor.example.com");

    Actions actions = new Actions(driver);

    // Text editor interactions
    WebElement textEditor = autoHeal.findElement("#editor", "text editor area");
    textEditor.click();

    // Type text
    textEditor.sendKeys("Hello World!");

    // Select all and copy
    actions.keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL).perform();
    actions.keyDown(Keys.CONTROL).sendKeys("c").keyUp(Keys.CONTROL).perform();

    // Move to new area and paste
    WebElement pasteArea = autoHeal.findElement("#paste-area", "area for pasting text");
    pasteArea.click();
    actions.keyDown(Keys.CONTROL).sendKeys("v").keyUp(Keys.CONTROL).perform();

    assertEquals("Hello World!", pasteArea.getAttribute("value"));

    // Undo action
    actions.keyDown(Keys.CONTROL).sendKeys("z").keyUp(Keys.CONTROL).perform();
    assertEquals("", pasteArea.getAttribute("value"));

    // Redo action
    actions.keyDown(Keys.CONTROL).keyDown(Keys.SHIFT).sendKeys("z").keyUp(Keys.SHIFT).keyUp(Keys.CONTROL).perform();
    assertEquals("Hello World!", pasteArea.getAttribute("value"));

    // Navigation keys
    textEditor.click();
    textEditor.clear();
    textEditor.sendKeys("Line 1\nLine 2\nLine 3");

    // Go to beginning
    actions.keyDown(Keys.CONTROL).sendKeys(Keys.HOME).keyUp(Keys.CONTROL).perform();

    // Select to end of line
    actions.keyDown(Keys.SHIFT).sendKeys(Keys.END).keyUp(Keys.SHIFT).perform();

    // Type over selection
    actions.sendKeys("First Line").perform();

    String editorContent = textEditor.getAttribute("value");
    assertTrue(editorContent.startsWith("First Line"));
}
```

---

## Error Handling

### Exception Handling

```java
@Test
void testErrorHandling() {
    driver.get("https://forms.example.com");

    // Handle element not found
    try {
        WebElement nonExistentElement = autoHeal.findElement("#does-not-exist", "non-existent element");
        fail("Should have thrown exception");
    } catch (AutoHealException e) {
        assertTrue(e.getMessage().contains("All healing strategies failed"));
        assertTrue(e.getErrorCode() == ErrorCode.ELEMENT_NOT_FOUND);
    }

    // Handle timeout
    try {
        // Set very short timeout
        AutoHealConfiguration quickTimeoutConfig = AutoHealConfiguration.builder()
            .performance(PerformanceConfig.builder()
                .elementTimeout(Duration.ofMillis(100))
                .build())
            .build();

        AutoHealLocator quickAutoHeal = AutoHealLocator.builder()
            .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
            .withConfiguration(quickTimeoutConfig)
            .build();

        WebElement timeoutElement = quickAutoHeal.findElement("#slow-loading-element", "very slow loading element");
        fail("Should have timed out");

    } catch (AutoHealException e) {
        assertTrue(e.getErrorCode() == ErrorCode.TIMEOUT_EXCEEDED);
    }

    // Graceful degradation
    WebElement fallbackElement = null;
    try {
        fallbackElement = autoHeal.findElement("#preferred-selector", "preferred element");
    } catch (AutoHealException e) {
        // Fallback to manual selector
        fallbackElement = driver.findElement(By.id("backup-selector"));
    }

    assertNotNull(fallbackElement);
}
```

### Retry Logic

```java
@Test
void testRetryLogic() {
    driver.get("https://unstable.example.com");

    // Custom retry for flaky elements
    WebElement flakyElement = retryFindElement(() ->
        autoHeal.findElement(".sometimes-present", "element that sometimes loads")
    );

    assertNotNull(flakyElement);
}

private WebElement retryFindElement(Supplier<WebElement> elementSupplier) {
    int maxRetries = 3;
    int attempt = 0;

    while (attempt < maxRetries) {
        try {
            return elementSupplier.get();
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) {
                throw e;
            }

            try {
                Thread.sleep(1000); // Wait before retry
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
        }
    }

    throw new RuntimeException("Element not found after " + maxRetries + " attempts");
}
```

### Validation and Assertions

```java
@Test
void testValidationAndAssertions() {
    driver.get("https://form.example.com");

    // Validate form before submission
    WebElement emailField = autoHeal.findElement("#email", "email input field");
    emailField.sendKeys("invalid-email");

    WebElement submitButton = autoHeal.findElement("Submit", "form submit button");
    submitButton.click();

    // Check for validation errors
    WebElement emailError = autoHeal.findElement(".email-error", "email validation error message");
    assertTrue(emailError.isDisplayed());
    assertTrue(emailError.getText().contains("valid email"));

    // Fix the email and resubmit
    emailField.clear();
    emailField.sendKeys("valid@example.com");
    submitButton.click();

    // Verify error is gone
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    wait.until(ExpectedConditions.invisibilityOf(emailError));

    // Check success state
    WebElement successMessage = autoHeal.findElement(".success-message", "form submission success message");
    assertTrue(successMessage.isDisplayed());

    // Validate form data was processed
    assertTrue(driver.getCurrentUrl().contains("success"));
}
```

---

This comprehensive guide covers the major Selenium integration patterns with AutoHeal. The self-healing capabilities ensure your tests remain stable even when the underlying web application changes, while the universal selector support makes it easy to write maintainable test code.

For more advanced topics, see:
- [Page Object Model Examples](./page-object-examples.md)
- [Cucumber Integration](./cucumber-examples.md)
- [Performance Optimization](../performance.md)