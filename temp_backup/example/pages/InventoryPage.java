package com.example.pages;

import com.example.base.BasePage;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced InventoryPage with AutoHeal capabilities for SauceDemo
 */
public class InventoryPage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(InventoryPage.class);

    // Selectors for SauceDemo inventory page
    private static final String PAGE_TITLE = ".title";
    private static final String INVENTORY_CONTAINER = "#inventory_container";
    private static final String INVENTORY_ITEMS = ".inventory_item";
    private static final String ITEM_NAME = ".inventory_item_name";
    private static final String ITEM_DESCRIPTION = ".inventory_item_desc";
    private static final String ITEM_PRICE = ".inventory_item_price";
    private static final String ADD_TO_CART_BUTTON = ".btn_primary";
    private static final String REMOVE_BUTTON = ".btn_secondary";
    private static final String SHOPPING_CART_BADGE = ".shopping_cart_badge";
    private static final String SHOPPING_CART_LINK = ".shopping_cart_link";
    private static final String SORT_DROPDOWN = ".product_sort_container";
    private static final String MENU_BUTTON = "#react-burger-menu-btn";

    // Element descriptions for AutoHeal
    private static final String PAGE_TITLE_DESCRIPTION = "Products page title";
    private static final String INVENTORY_CONTAINER_DESCRIPTION = "Main inventory container";
    private static final String INVENTORY_ITEMS_DESCRIPTION = "Product inventory items";
    private static final String ITEM_NAME_DESCRIPTION = "Product name";
    private static final String ITEM_DESCRIPTION_DESCRIPTION = "Product description";
    private static final String ITEM_PRICE_DESCRIPTION = "Product price";
    private static final String ADD_TO_CART_DESCRIPTION = "Add to cart button";
    private static final String REMOVE_BUTTON_DESCRIPTION = "Remove from cart button";
    private static final String CART_BADGE_DESCRIPTION = "Shopping cart item count badge";
    private static final String CART_LINK_DESCRIPTION = "Shopping cart link";
    private static final String SORT_DROPDOWN_DESCRIPTION = "Product sort dropdown";
    private static final String MENU_BUTTON_DESCRIPTION = "Hamburger menu button";

    public InventoryPage(Page page) {
        super(page);
        logger.info("InventoryPage initialized with AutoHeal capabilities");
    }

    /**
     * Check if inventory page is loaded
     */
    public boolean isInventoryPageLoaded() {
        boolean pageLoaded = isVisible(PAGE_TITLE, PAGE_TITLE_DESCRIPTION) &&
                            isVisible(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
        
        logger.debug("Inventory page loaded: {}", pageLoaded);
        return pageLoaded;
    }

    /**
     * Get page title with AutoHeal
     */
    public String getPageTitle() {
        logger.debug("Retrieving page title");
        String title = getText(PAGE_TITLE, PAGE_TITLE_DESCRIPTION);
        logger.debug("Page title: {}", title);
        return title;
    }

    /**
     * Get count of inventory items
     */
    public int getInventoryItemCount() {
        // This is a more complex operation that would benefit from AutoHeal
        try {
            waitForElement(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
            String script = "return document.querySelectorAll('" + INVENTORY_ITEMS + "').length";
            Object result = executeScript(script);
            int count = result instanceof Integer ? (Integer) result : 0;
            
            logger.debug("Inventory item count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Failed to get inventory item count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get all product names using AutoHeal
     */
    public List<String> getAllProductNames() {
        logger.debug("Retrieving all product names");
        
        waitForElement(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
        
        try {
            String script = """
                return Array.from(document.querySelectorAll('%s')).map(el => el.textContent.trim());
                """.formatted(ITEM_NAME);
                
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) executeScript(script);
            
            logger.info("Retrieved {} product names", names.size());
            return names != null ? names : List.of();
            
        } catch (Exception e) {
            logger.error("Failed to get product names: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Add item to cart by product name
     */
    public InventoryPage addItemToCart(String productName) {
        logger.info("Adding item to cart: {}", productName);
        
        try {
            waitForElement(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
            
            // Find the specific product and click its add to cart button
            String script = """
                const items = Array.from(document.querySelectorAll('%s'));
                const item = items.find(item => 
                    item.querySelector('%s')?.textContent.trim() === '%s'
                );
                
                if (item) {
                    const addButton = item.querySelector('%s');
                    if (addButton) {
                        addButton.click();
                        return true;
                    }
                }
                return false;
                """.formatted(INVENTORY_ITEMS, ITEM_NAME, productName, ADD_TO_CART_BUTTON);
            
            Boolean success = (Boolean) executeScript(script);
            
            if (Boolean.TRUE.equals(success)) {
                logger.info("Successfully added '{}' to cart", productName);
            } else {
                logger.warn("Failed to add '{}' to cart - item not found or button not clickable", productName);
            }
            
        } catch (Exception e) {
            logger.error("Error adding item '{}' to cart: {}", productName, e.getMessage());
        }
        
        return this;
    }

    /**
     * Remove item from cart by product name
     */
    public InventoryPage removeItemFromCart(String productName) {
        logger.info("Removing item from cart: {}", productName);
        
        try {
            waitForElement(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
            
            String script = """
                const items = Array.from(document.querySelectorAll('%s'));
                const item = items.find(item => 
                    item.querySelector('%s')?.textContent.trim() === '%s'
                );
                
                if (item) {
                    const removeButton = item.querySelector('%s');
                    if (removeButton) {
                        removeButton.click();
                        return true;
                    }
                }
                return false;
                """.formatted(INVENTORY_ITEMS, ITEM_NAME, productName, REMOVE_BUTTON);
            
            Boolean success = (Boolean) executeScript(script);
            
            if (Boolean.TRUE.equals(success)) {
                logger.info("Successfully removed '{}' from cart", productName);
            } else {
                logger.warn("Failed to remove '{}' from cart - item not found or button not clickable", productName);
            }
            
        } catch (Exception e) {
            logger.error("Error removing item '{}' from cart: {}", productName, e.getMessage());
        }
        
        return this;
    }

    /**
     * Get shopping cart item count
     */
    public int getCartItemCount() {
        try {
            if (isVisible(SHOPPING_CART_BADGE, CART_BADGE_DESCRIPTION)) {
                String countText = getText(SHOPPING_CART_BADGE, CART_BADGE_DESCRIPTION);
                int count = Integer.parseInt(countText.trim());
                logger.debug("Cart item count: {}", count);
                return count;
            } else {
                logger.debug("Cart badge not visible - cart is empty");
                return 0;
            }
        } catch (Exception e) {
            logger.error("Failed to get cart item count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Click shopping cart to go to cart page
     */
    public void goToCart() {
        logger.info("Navigating to shopping cart");
        click(SHOPPING_CART_LINK, CART_LINK_DESCRIPTION);
    }

    /**
     * Sort products by option
     */
    public InventoryPage sortProductsBy(String sortOption) {
        logger.info("Sorting products by: {}", sortOption);
        
        try {
            waitForElement(SORT_DROPDOWN, SORT_DROPDOWN_DESCRIPTION);
            
            String script = """
                const dropdown = document.querySelector('%s');
                if (dropdown) {
                    dropdown.value = '%s';
                    dropdown.dispatchEvent(new Event('change', { bubbles: true }));
                    return true;
                }
                return false;
                """.formatted(SORT_DROPDOWN, sortOption);
            
            Boolean success = (Boolean) executeScript(script);
            
            if (Boolean.TRUE.equals(success)) {
                // Wait for sorting to complete
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Successfully sorted products by: {}", sortOption);
            } else {
                logger.warn("Failed to sort products by: {}", sortOption);
            }
            
        } catch (Exception e) {
            logger.error("Error sorting products by '{}': {}", sortOption, e.getMessage());
        }
        
        return this;
    }

    /**
     * Open hamburger menu
     */
    public InventoryPage openMenu() {
        logger.debug("Opening hamburger menu");
        click(MENU_BUTTON, MENU_BUTTON_DESCRIPTION);
        return this;
    }

    /**
     * Get product information by name
     */
    public ProductInfo getProductInfo(String productName) {
        logger.debug("Getting product info for: {}", productName);
        
        try {
            waitForElement(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
            
            String script = """
                const items = Array.from(document.querySelectorAll('%s'));
                const item = items.find(item => 
                    item.querySelector('%s')?.textContent.trim() === '%s'
                );
                
                if (item) {
                    return {
                        name: item.querySelector('%s')?.textContent.trim() || '',
                        description: item.querySelector('%s')?.textContent.trim() || '',
                        price: item.querySelector('%s')?.textContent.trim() || '',
                        hasAddButton: !!item.querySelector('%s'),
                        hasRemoveButton: !!item.querySelector('%s')
                    };
                }
                return null;
                """.formatted(INVENTORY_ITEMS, ITEM_NAME, productName, 
                            ITEM_NAME, ITEM_DESCRIPTION, ITEM_PRICE, 
                            ADD_TO_CART_BUTTON, REMOVE_BUTTON);
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) executeScript(script);
            
            if (result != null) {
                ProductInfo info = new ProductInfo(
                    (String) result.get("name"),
                    (String) result.get("description"), 
                    (String) result.get("price"),
                    (Boolean) result.get("hasAddButton"),
                    (Boolean) result.get("hasRemoveButton")
                );
                
                logger.debug("Product info retrieved: {}", info);
                return info;
            }
            
        } catch (Exception e) {
            logger.error("Failed to get product info for '{}': {}", productName, e.getMessage());
        }
        
        return null;
    }

    /**
     * Verify inventory page is fully loaded and functional
     */
    public boolean verifyPageFunctionality() {
        logger.info("Verifying inventory page functionality");
        
        boolean titleVisible = isVisible(PAGE_TITLE, PAGE_TITLE_DESCRIPTION);
        boolean containerVisible = isVisible(INVENTORY_CONTAINER, INVENTORY_CONTAINER_DESCRIPTION);
        boolean sortDropdownVisible = isVisible(SORT_DROPDOWN, SORT_DROPDOWN_DESCRIPTION);
        boolean cartLinkVisible = isVisible(SHOPPING_CART_LINK, CART_LINK_DESCRIPTION);
        boolean menuButtonVisible = isVisible(MENU_BUTTON, MENU_BUTTON_DESCRIPTION);
        
        int itemCount = getInventoryItemCount();
        boolean hasItems = itemCount > 0;
        
        boolean fullyFunctional = titleVisible && containerVisible && sortDropdownVisible && 
                                 cartLinkVisible && menuButtonVisible && hasItems;
        
        logger.info("Page functionality verification: title={}, container={}, sort={}, cart={}, menu={}, items={} (count={}), overall={}", 
                titleVisible, containerVisible, sortDropdownVisible, cartLinkVisible, 
                menuButtonVisible, hasItems, itemCount, fullyFunctional);
                
        return fullyFunctional;
    }

    /**
     * Product information class
     */
    public static class ProductInfo {
        private final String name;
        private final String description;
        private final String price;
        private final boolean hasAddButton;
        private final boolean hasRemoveButton;

        public ProductInfo(String name, String description, String price, 
                          boolean hasAddButton, boolean hasRemoveButton) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.hasAddButton = hasAddButton;
            this.hasRemoveButton = hasRemoveButton;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPrice() { return price; }
        public boolean hasAddButton() { return hasAddButton; }
        public boolean hasRemoveButton() { return hasRemoveButton; }

        @Override
        public String toString() {
            return String.format("ProductInfo{name='%s', price='%s', addButton=%s, removeButton=%s}", 
                    name, price, hasAddButton, hasRemoveButton);
        }
    }
}