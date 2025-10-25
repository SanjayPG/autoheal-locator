package com.autoheal.exception;

/**
 * Exception thrown when AutoHeal cannot extract a locator string from a native Playwright Locator object.
 *
 * This typically occurs when:
 * <ul>
 *   <li>Complex chained locators are used (e.g., multiple filter() calls)</li>
 *   <li>Custom locator implementations that don't expose internal state</li>
 *   <li>Reflection fails due to security restrictions or API changes</li>
 * </ul>
 *
 * <p><strong>Resolution:</strong> Use JavaScript-style string format instead:</p>
 * <pre>
 * // Instead of:
 * autoHeal.find(page, page.locator("...").filter(...).nth(2), "desc");
 *
 * // Use:
 * autoHeal.find(page, "locator('...').filter(...).nth(2)", "desc");
 * </pre>
 *
 * @author AutoHeal Framework
 * @version 1.0.0
 * @since 1.0.0
 */
public class PlaywrightLocatorExtractionException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message explaining why extraction failed
     */
    public PlaywrightLocatorExtractionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message explaining why extraction failed
     * @param cause the underlying cause of the extraction failure
     */
    public PlaywrightLocatorExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
