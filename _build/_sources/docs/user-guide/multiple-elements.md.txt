# Handling Multiple Elements - Smart Disambiguation

## Overview

When multiple elements match the same locator, AutoHeal's AI uses contextual descriptions to intelligently select the correct element. This eliminates the need for complex, brittle selectors and makes tests more maintainable.

---

## How AI Disambiguation Works

### 1. Context-Based Selection

AutoHeal analyzes the element description to understand the intended context:

```java
// Page has multiple "Submit" buttons - AI selects based on description
WebElement orderSubmit = autoHeal.findElement("Submit", "submit order button in checkout form");
WebElement reviewSubmit = autoHeal.findElement("Submit", "submit review button in product page");
WebElement contactSubmit = autoHeal.findElement("Submit", "submit contact form button");

// ✅ "checkout form" → AI finds submit button within checkout context
// ✅ "product page" → AI finds submit button in review section
// ✅ "contact form" → AI finds submit button in contact section
```

### 2. Location-Based Disambiguation

AI considers the element's position and surrounding context:

```java
// Multiple "Edit" buttons on a user management page
WebElement editProfile = autoHeal.findElement("Edit", "edit button next to user profile");
WebElement editSettings = autoHeal.findElement("Edit", "edit button in settings section");
WebElement editPreferences = autoHeal.findElement("Edit", "edit button at bottom of preferences");

// AI-powered analysis finds the correct "Edit" button based on location
```

### 3. Semantic Understanding

AutoHeal understands business context and common UI patterns:

```java
// E-commerce site with multiple "Add to Cart" buttons
WebElement addMainProduct = autoHeal.findElement("Add to Cart", "add main product to cart");
WebElement addAccessory = autoHeal.findElement("Add to Cart", "add recommended accessory to cart");
WebElement addWarranty = autoHeal.findElement("Add to Cart", "add extended warranty to cart");

// AI understands "main product", "accessory", "warranty" contexts
```

---

## Common Disambiguation Scenarios

### 1. Form Buttons

```java
// Registration form with multiple buttons
WebElement createAccount = autoHeal.findElement("Create Account", "create new account button");
WebElement loginInstead = autoHeal.findElement("Sign In", "sign in instead button");
WebElement forgotPassword = autoHeal.findElement("Forgot Password", "forgot password link");

// Payment form with multiple submit options
WebElement payNow = autoHeal.findElement("Pay Now", "pay now button for immediate payment");
WebElement payLater = autoHeal.findElement("Pay Later", "pay later option button");
WebElement saveDraft = autoHeal.findElement("Save Draft", "save order as draft button");
```

### 2. Navigation Elements

```java
// Multiple "Home" links in different navigation areas
WebElement mainNavHome = autoHeal.findElement("Home", "home link in main navigation");
WebElement breadcrumbHome = autoHeal.findElement("Home", "home link in breadcrumb navigation");
WebElement footerHome = autoHeal.findElement("Home", "home link in footer");

// Tabs with similar names
WebElement personalInfo = autoHeal.findElement("Personal", "personal information tab");
WebElement personalPrefs = autoHeal.findElement("Personal", "personal preferences tab");
```

### 3. Data Tables

```java
// Table with multiple action buttons per row
WebElement editUser1 = autoHeal.findElement("Edit", "edit button for John Doe user row");
WebElement editUser2 = autoHeal.findElement("Edit", "edit button for Jane Smith user row");
WebElement deleteUser1 = autoHeal.findElement("Delete", "delete button for John Doe user row");

// AI uses surrounding table data to identify the correct row
```

### 4. Modal Dialogs

```java
// Multiple modals with similar buttons
WebElement confirmDelete = autoHeal.findElement("Confirm", "confirm button in delete confirmation dialog");
WebElement confirmSave = autoHeal.findElement("Confirm", "confirm button in save changes dialog");
WebElement confirmLogout = autoHeal.findElement("Confirm", "confirm button in logout confirmation");

// AI identifies the active modal and its context
```

---

## Best Practices for Descriptions

### 1. Include Context Information

```java
// ❌ Vague descriptions - Hard to disambiguate
autoHeal.findElement("Submit", "submit button");
autoHeal.findElement("Edit", "edit button");

// ✅ Contextual descriptions - Easy to disambiguate
autoHeal.findElement("Submit", "submit button in contact form");
autoHeal.findElement("Edit", "edit button in user profile section");
```

### 2. Specify Location When Helpful

```java
// ✅ Include location context
autoHeal.findElement("Search", "search button in header navigation");
autoHeal.findElement("Search", "search button in product filter sidebar");
autoHeal.findElement("Add", "add item button at top of shopping list");
autoHeal.findElement("Add", "add item button at bottom of shopping list");
```

### 3. Mention Surrounding Elements

```java
// ✅ Reference nearby elements for context
autoHeal.findElement("Save", "save button next to cancel button");
autoHeal.findElement("Next", "next button below form fields");
autoHeal.findElement("Upload", "upload button in file attachment area");
```

### 4. Use Business Terminology

```java
// ✅ Use domain-specific language
autoHeal.findElement("Checkout", "checkout button in shopping cart");
autoHeal.findElement("Apply", "apply coupon code button");
autoHeal.findElement("Subscribe", "subscribe to newsletter button in footer");
autoHeal.findElement("Book Now", "book appointment button for dentist");
```

---

## Advanced Disambiguation Techniques

### 1. Hierarchical Context

```java
// Multi-level context for complex pages
WebElement saveProfile = autoHeal.findElement("Save",
    "save button in personal information section of user profile page");
WebElement saveSettings = autoHeal.findElement("Save",
    "save button in notification preferences section of settings page");
```

### 2. Temporal Context

```java
// Time-based disambiguation
WebElement morningSlot = autoHeal.findElement("Book", "book 9 AM appointment slot");
WebElement afternoonSlot = autoHeal.findElement("Book", "book 2 PM appointment slot");
WebElement emergencySlot = autoHeal.findElement("Book", "book emergency appointment slot");
```

### 3. State-Based Selection

```java
// Select based on element state or content
WebElement activeTab = autoHeal.findElement("Dashboard", "active dashboard tab");
WebElement inactiveTab = autoHeal.findElement("Reports", "inactive reports tab");
WebElement enabledBtn = autoHeal.findElement("Process", "enabled process button");
WebElement disabledBtn = autoHeal.findElement("Process", "disabled process button");
```

---

## Working with Lists and Collections

### 1. Specific Item Selection

```java
// Select specific items from lists
WebElement firstProduct = autoHeal.findElement("Add to Cart",
    "add to cart button for first product in search results");
WebElement lastProduct = autoHeal.findElement("Add to Cart",
    "add to cart button for last product in search results");
WebElement featuredProduct = autoHeal.findElement("Add to Cart",
    "add to cart button for featured product");
```

### 2. Category-Based Selection

```java
// Select based on category or type
WebElement electronicsFilter = autoHeal.findElement("Electronics",
    "electronics category filter in product search");
WebElement clothingFilter = autoHeal.findElement("Clothing",
    "clothing category filter in product search");
```

### 3. Dynamic List Handling

```java
// Handle dynamically generated lists
WebElement newMessage = autoHeal.findElement("Reply",
    "reply button for newest message in inbox");
WebElement importantMessage = autoHeal.findElement("Reply",
    "reply button for message marked as important");
```

---

## Debugging Disambiguation

### 1. Enable Debug Logging

```java
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .logging(LoggingConfig.builder()
        .logAIRequests(true)
        .logDisambiguationDetails(true)
        .verboseLogging(true)
        .build())
    .build();

// Logs will show:
// - Number of matching elements found
// - AI reasoning for element selection
// - Confidence scores for each candidate
```

### 2. Review Healing Reports

```java
// Generate detailed reports
.reporting(ReportingConfig.builder()
    .enabled(true)
    .generateHTML(true)
    .includeDisambiguationDetails(true)
    .build())

// Reports include:
// - Screenshots showing all matching elements
// - AI decision reasoning
// - Alternative elements considered
// - Confidence scores and selection criteria
```

### 3. Test Disambiguation Logic

```java
@Test
void testButtonDisambiguation() {
    // Test page with multiple "Save" buttons
    driver.get("https://example.com/user-profile");

    // Should find profile save button
    WebElement profileSave = autoHeal.findElement("Save",
        "save button in profile information section");

    // Should find settings save button
    WebElement settingsSave = autoHeal.findElement("Save",
        "save button in account settings section");

    // Verify different elements were selected
    assertNotEquals(profileSave, settingsSave);

    // Verify correct sections
    assertTrue(profileSave.getText().contains("Save Profile") ||
               profileSave.findElement(By.xpath("./ancestor::*[@class*='profile']")) != null);
}
```

---

## Performance Considerations

### 1. Disambiguation Caching

```java
// AI disambiguation results are cached to improve performance
AutoHealConfiguration config = AutoHealConfiguration.builder()
    .cache(CacheConfig.builder()
        .enabled(true)
        .cacheDisambiguationResults(true)  // Cache AI decisions
        .maxSize(10000)
        .build())
    .build();

// Subsequent calls with same description will use cached results
```

### 2. Optimize Descriptions

```java
// ✅ Concise but descriptive
autoHeal.findElement("Submit", "submit order form");

// ❌ Unnecessarily verbose - slower AI processing
autoHeal.findElement("Submit",
    "the submit button that is used to submit the order form when the user wants to complete their purchase after filling in all the required fields");
```

### 3. Batch Disambiguation

```java
// When finding multiple related elements, batch the requests
List<LocatorRequest> requests = Arrays.asList(
    new LocatorRequest("Edit", "edit user profile button"),
    new LocatorRequest("Edit", "edit user settings button"),
    new LocatorRequest("Delete", "delete user account button")
);

List<WebElement> elements = autoHeal.findElementsBatch(requests);
```

---

## Troubleshooting Common Issues

### 1. Incorrect Element Selected

**Problem**: AI selects wrong element despite good description

**Solution**:
```java
// Add more specific context
// Instead of:
autoHeal.findElement("Save", "save button");

// Use:
autoHeal.findElement("Save", "save button in the blue form at the top of the page");
```

### 2. Disambiguation Too Slow

**Problem**: AI takes too long to disambiguate

**Solution**:
```java
// Use more specific selectors when possible
// Instead of:
autoHeal.findElement("button", "submit button");  // Many button elements

// Use:
autoHeal.findElement("input[type='submit']", "submit button");  // Fewer matches
```

### 3. Inconsistent Selection

**Problem**: Same description returns different elements on different runs

**Solution**:
```java
// Make descriptions more deterministic
// Instead of:
autoHeal.findElement("Save", "save button");  // Ambiguous

// Use:
autoHeal.findElement("Save", "save button in user profile form");  // Specific context
```

---

## Next Steps

1. [Performance Optimization](./performance.md) - Optimize disambiguation performance
2. [Usage Examples](./examples/) - See more real-world disambiguation scenarios
3. [Troubleshooting Guide](./troubleshooting.md) - Debug disambiguation issues