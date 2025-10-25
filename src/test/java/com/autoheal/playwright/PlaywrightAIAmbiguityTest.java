package com.autoheal.playwright;

import com.autoheal.AutoHealLocator;
import com.autoheal.config.*;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.autoheal.impl.ai.ResilientAIService;
import com.autoheal.model.AIProvider;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Playwright AutoHeal with ambiguous elements - AI needs to disambiguate
 */
public class PlaywrightAIAmbiguityTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private AutoHealLocator autoHeal;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        System.out.println("🚀 Playwright browser launched for ambiguity testing");
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        System.out.println("🔒 Playwright browser closed");
    }

    @BeforeEach
    void setup() {
        page = browser.newPage();

        // Create Gemini AI config
        AIConfig aiConfig = AIConfig.builder()
                .provider(AIProvider.GOOGLE_GEMINI)
                .model("gemini-2.0-flash")
                .apiKey("AIzaSyDjFQ7M6pKRsYyr1SmZugaxs7mGiOh57w8")
                .timeout(java.time.Duration.ofSeconds(30))
                .maxRetries(2)
                .maxTokensDOM(1500)
                .temperatureDOM(0.1)
                .visualAnalysisEnabled(false)
                .build();

        ResilienceConfig resilienceConfig = ResilienceConfig.builder().build();
        ResilientAIService aiService = new ResilientAIService(aiConfig, resilienceConfig);

        AutoHealConfiguration config = AutoHealConfiguration.builder()
                .ai(aiConfig)
                .resilience(resilienceConfig)
                .build();

        com.autoheal.impl.cache.CaffeineBasedSelectorCache cache =
                new com.autoheal.impl.cache.CaffeineBasedSelectorCache(
                        com.autoheal.config.CacheConfig.defaultConfig());

        PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
        autoHeal = AutoHealLocator.builder()
                .withWebAdapter(adapter)
                .withConfiguration(config)
                .withCache(cache)
                .withAIService(aiService)
                .build();

        System.out.println("🤖 AutoHeal initialized with Gemini AI for ambiguity testing");
    }

    @AfterEach
    void tearDown() {
        if (page != null) {
            page.close();
        }
    }

    @Test
    @DisplayName("Test 1: Multiple buttons with same text - AI should use context")
    void testMultipleButtonsSameText() {
        System.out.println("\n🧪 Test 1: Multiple buttons with same text");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="product-card" data-product="laptop">
                    <h3>Laptop - $999</h3>
                    <button class="add-to-cart" data-testid="add-laptop">Add to Cart</button>
                </div>

                <div class="product-card" data-product="phone">
                    <h3>Phone - $699</h3>
                    <button class="add-to-cart" data-testid="add-phone">Add to Cart</button>
                </div>

                <div class="product-card" data-product="tablet">
                    <h3>Tablet - $499</h3>
                    <button class="add-to-cart" data-testid="add-tablet">Add to Cart</button>
                </div>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with 3 'Add to Cart' buttons");

        // Wrong selector - needs AI to find the PHONE's add to cart button
        System.out.println("\n🔴 Using WRONG selector: getByText('Add')");
        System.out.println("   (Too generic - matches all 3 buttons. Need the Phone button specifically)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator phoneCartButton = autoHeal.find(
                page,
                "getByText('Add')",  // WRONG - too ambiguous
                "Add to cart button for the Phone product"  // AI should use context
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(phoneCartButton, "AI should find the phone cart button");
        assertTrue(phoneCartButton.count() > 0, "Should find at least one button");

        // Verify it's the correct button (phone button)
        String testId = phoneCartButton.getAttribute("data-testid");
        System.out.println("✅ AI found button with data-testid: " + testId);
        System.out.println("   Time: " + duration + "ms");

        assertTrue(testId.equals("add-phone"),
            "AI should identify the phone button specifically, got: " + testId);

        System.out.println("🎉 AI correctly disambiguated and found the Phone button!");
    }

    @Test
    @DisplayName("Test 2: Multiple inputs with similar placeholders")
    void testMultipleSimilarInputs() {
        System.out.println("\n🧪 Test 2: Multiple inputs with similar placeholders");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <form>
                    <div class="field-group">
                        <label>Shipping Address</label>
                        <input type="text" placeholder="Street Address" id="shipping-street" />
                        <input type="text" placeholder="City" id="shipping-city" />
                        <input type="text" placeholder="ZIP Code" id="shipping-zip" />
                    </div>

                    <div class="field-group">
                        <label>Billing Address</label>
                        <input type="text" placeholder="Street Address" id="billing-street" />
                        <input type="text" placeholder="City" id="billing-city" />
                        <input type="text" placeholder="ZIP Code" id="billing-zip" />
                    </div>
                </form>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with duplicate placeholders in shipping/billing forms");

        System.out.println("\n🔴 Using WRONG selector: getByPlaceholder('City')");
        System.out.println("   (Ambiguous - matches both shipping and billing city fields)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator billingCity = autoHeal.find(
                page,
                "getByPlaceholder('City')",  // WRONG - ambiguous
                "City input field in the billing address section"  // Specific context
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(billingCity, "AI should find the billing city field");
        assertTrue(billingCity.count() > 0, "Should find the input");

        String inputId = billingCity.getAttribute("id");
        System.out.println("✅ AI found input with id: " + inputId);
        System.out.println("   Time: " + duration + "ms");

        assertEquals("billing-city", inputId,
            "AI should identify the billing city field specifically");

        System.out.println("🎉 AI correctly distinguished billing city from shipping city!");
    }

    @Test
    @DisplayName("Test 3: Table with multiple rows - find specific row")
    void testTableRowAmbiguity() {
        System.out.println("\n🧪 Test 3: Table rows with similar structure");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr data-user="john">
                            <td>John Doe</td>
                            <td>john@example.com</td>
                            <td><button class="edit-btn">Edit</button></td>
                        </tr>
                        <tr data-user="jane">
                            <td>Jane Smith</td>
                            <td>jane@example.com</td>
                            <td><button class="edit-btn">Edit</button></td>
                        </tr>
                        <tr data-user="bob">
                            <td>Bob Johnson</td>
                            <td>bob@example.com</td>
                            <td><button class="edit-btn">Edit</button></td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with user table (3 rows, each with 'Edit' button)");

        System.out.println("\n🔴 Using WRONG selector: getByRole(button, {name: 'Edit'})");
        System.out.println("   (Ambiguous - 3 identical Edit buttons. Need Jane's specifically)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator janeEditButton = autoHeal.find(
                page,
                "getByRole(button, {name: 'Edit'})",  // WRONG - matches all 3
                "Edit button for the user Jane Smith in the table"  // Specific user
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(janeEditButton, "AI should find Jane's edit button");
        assertTrue(janeEditButton.count() > 0, "Should find the button");

        // Get the parent row to verify it's Jane's row
        com.microsoft.playwright.Locator parentRow = janeEditButton.locator("xpath=ancestor::tr");
        String userData = parentRow.getAttribute("data-user");

        System.out.println("✅ AI found Edit button in row: " + userData);
        System.out.println("   Time: " + duration + "ms");

        assertEquals("jane", userData,
            "AI should identify Jane's edit button specifically");

        System.out.println("🎉 AI correctly found Jane's Edit button among 3 identical buttons!");
    }

    @Test
    @DisplayName("Test 4: Navigation menu with nested items")
    void testNestedNavigationMenu() {
        System.out.println("\n🧪 Test 4: Nested navigation with similar link text");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <nav>
                    <ul class="main-menu">
                        <li>
                            <a href="/home">Home</a>
                        </li>
                        <li class="dropdown">
                            <a href="/products">Products</a>
                            <ul class="submenu">
                                <li><a href="/products/laptops" id="nav-laptops">Laptops</a></li>
                                <li><a href="/products/phones" id="nav-phones">Phones</a></li>
                                <li><a href="/products/tablets" id="nav-tablets">Tablets</a></li>
                            </ul>
                        </li>
                        <li>
                            <a href="/about">About</a>
                        </li>
                    </ul>

                    <ul class="footer-menu">
                        <li><a href="/products-all" id="footer-products">Products</a></li>
                        <li><a href="/contact">Contact</a></li>
                    </ul>
                </nav>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with navigation (main menu and footer menu)");

        System.out.println("\n🔴 Using WRONG selector: getByText('Products')");
        System.out.println("   (Ambiguous - appears in both main nav and footer. Need submenu Phones link)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator phonesLink = autoHeal.find(
                page,
                "getByText('Products')",  // WRONG - too generic
                "Phones link in the Products submenu of main navigation"  // Very specific
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(phonesLink, "AI should find the Phones submenu link");
        assertTrue(phonesLink.count() > 0, "Should find the link");

        String linkId = phonesLink.getAttribute("id");
        System.out.println("✅ AI found link with id: " + linkId);
        System.out.println("   Time: " + duration + "ms");

        assertEquals("nav-phones", linkId,
            "AI should identify the Phones submenu link specifically");

        System.out.println("🎉 AI correctly navigated nested menu structure!");
    }

    @Test
    @DisplayName("Test 5: Modal dialogs with same buttons")
    void testOverlappingModals() {
        System.out.println("\n🧪 Test 5: Multiple modals with identical button text");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="modal" id="delete-modal" data-modal="delete">
                    <h2>Confirm Delete</h2>
                    <p>Are you sure you want to delete this item?</p>
                    <button class="btn-cancel" data-action="cancel-delete">Cancel</button>
                    <button class="btn-confirm" data-action="confirm-delete">Confirm</button>
                </div>

                <div class="modal" id="logout-modal" data-modal="logout">
                    <h2>Confirm Logout</h2>
                    <p>Are you sure you want to log out?</p>
                    <button class="btn-cancel" data-action="cancel-logout">Cancel</button>
                    <button class="btn-confirm" data-action="confirm-logout">Confirm</button>
                </div>

                <div class="modal" id="save-modal" data-modal="save">
                    <h2>Save Changes</h2>
                    <p>Do you want to save your changes?</p>
                    <button class="btn-cancel" data-action="cancel-save">Cancel</button>
                    <button class="btn-confirm" data-action="confirm-save">Confirm</button>
                </div>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with 3 modals, each with 'Cancel' and 'Confirm' buttons");

        System.out.println("\n🔴 Using WRONG selector: getByRole(button, {name: 'Confirm'})");
        System.out.println("   (Ambiguous - 3 Confirm buttons. Need logout modal specifically)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator logoutConfirm = autoHeal.find(
                page,
                "getByRole(button, {name: 'Confirm'})",  // WRONG - matches all 3
                "Confirm button in the logout confirmation modal"  // Specific modal context
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(logoutConfirm, "AI should find logout confirm button");
        assertTrue(logoutConfirm.count() > 0, "Should find the button");

        String action = logoutConfirm.getAttribute("data-action");
        System.out.println("✅ AI found Confirm button with action: " + action);
        System.out.println("   Time: " + duration + "ms");

        assertEquals("confirm-logout", action,
            "AI should identify the logout modal's confirm button");

        System.out.println("🎉 AI correctly distinguished logout modal from delete/save modals!");
    }

    @Test
    @DisplayName("Test 6: Dynamic list with similar items")
    void testDynamicListItems() {
        System.out.println("\n🧪 Test 6: Shopping cart with multiple product entries");

        String html = """
            <!DOCTYPE html>
            <html>
            <body>
                <div class="shopping-cart">
                    <div class="cart-item" data-product-id="101">
                        <span class="product-name">Wireless Mouse</span>
                        <span class="quantity">Qty: 2</span>
                        <span class="price">$29.99</span>
                        <button class="remove-btn" data-remove="101">Remove</button>
                    </div>

                    <div class="cart-item" data-product-id="102">
                        <span class="product-name">USB Keyboard</span>
                        <span class="quantity">Qty: 1</span>
                        <span class="price">$49.99</span>
                        <button class="remove-btn" data-remove="102">Remove</button>
                    </div>

                    <div class="cart-item" data-product-id="103">
                        <span class="product-name">HDMI Cable</span>
                        <span class="quantity">Qty: 3</span>
                        <span class="price">$12.99</span>
                        <button class="remove-btn" data-remove="103">Remove</button>
                    </div>

                    <div class="cart-item" data-product-id="104">
                        <span class="product-name">Wireless Mouse</span>
                        <span class="quantity">Qty: 1</span>
                        <span class="price">$29.99</span>
                        <button class="remove-btn" data-remove="104">Remove</button>
                    </div>
                </div>
            </body>
            </html>
            """;

        page.setContent(html);
        System.out.println("📄 Page loaded with shopping cart (4 items, 2 with duplicate product names)");

        System.out.println("\n🔴 Using WRONG selector: getByText('Remove')");
        System.out.println("   (Ambiguous - 4 Remove buttons. Need the one for USB Keyboard with quantity 1)");

        long startTime = System.currentTimeMillis();

        com.microsoft.playwright.Locator keyboardRemove = autoHeal.find(
                page,
                "getByText('Remove')",  // WRONG - matches all 4
                "Remove button for USB Keyboard product with quantity 1 in shopping cart"
        );

        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(keyboardRemove, "AI should find keyboard remove button");
        assertTrue(keyboardRemove.count() > 0, "Should find the button");

        String removeId = keyboardRemove.getAttribute("data-remove");
        System.out.println("✅ AI found Remove button with data-remove: " + removeId);
        System.out.println("   Time: " + duration + "ms");

        assertEquals("102", removeId,
            "AI should identify the USB Keyboard's remove button (product ID 102)");

        System.out.println("🎉 AI correctly identified USB Keyboard among duplicate items!");
    }
}
