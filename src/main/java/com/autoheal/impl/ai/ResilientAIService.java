package com.autoheal.impl.ai;

import com.autoheal.config.AIConfig;
import com.autoheal.config.ResilienceConfig;
import com.autoheal.core.AIService;
import com.autoheal.exception.CircuitBreakerOpenException;
import com.autoheal.metrics.AIServiceMetrics;
import com.autoheal.model.AIAnalysisResult;
import com.autoheal.model.AIProvider;
import com.autoheal.model.ElementCandidate;
import com.autoheal.resilience.CircuitBreaker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resilient AI Service with circuit breaker and fallback
 */
public class ResilientAIService implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(ResilientAIService.class);

    private final List<AIProvider> providers;
    private final CircuitBreaker circuitBreaker;
    private final ExecutorService executorService;
    private final AIServiceMetrics metrics;
    private final AIConfig config;
    private final OkHttpClient httpClient;
    private final com.autoheal.metrics.CostMetrics costMetrics;

    public ResilientAIService(AIConfig config, ResilienceConfig resilienceConfig) {
        this.config = config;
        this.providers = initializeProviders(config);
        this.circuitBreaker = new CircuitBreaker(
                resilienceConfig.getCircuitBreakerFailureThreshold(),
                resilienceConfig.getCircuitBreakerTimeout()
        );
        this.executorService = Executors.newFixedThreadPool(4);
        this.metrics = new AIServiceMetrics();
        this.costMetrics = new com.autoheal.metrics.CostMetrics();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout())
                .readTimeout(config.getTimeout())
                .writeTimeout(config.getTimeout())
                .build();

        logger.info("ResilientAIService initialized with provider: {}", config.getProvider());
    }

    @Override
    public CompletableFuture<AIAnalysisResult> analyzeDOM(String html, String description, String previousSelector) {
        // Default to Selenium for backward compatibility
        return analyzeDOM(html, description, previousSelector, com.autoheal.model.AutomationFramework.SELENIUM);
    }

    @Override
    public CompletableFuture<AIAnalysisResult> analyzeDOM(String html, String description, String previousSelector,
                                                           com.autoheal.model.AutomationFramework framework) {
        if (!circuitBreaker.canExecute()) {
            metrics.recordCircuitBreakerOpen();
            return CompletableFuture.failedFuture(
                    new CircuitBreakerOpenException("AI Service circuit breaker is open")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Build prompt based on framework
                String prompt = framework == com.autoheal.model.AutomationFramework.PLAYWRIGHT
                        ? buildPlaywrightDOMAnalysisPrompt(html, description, previousSelector)
                        : buildDOMAnalysisPrompt(html, description, previousSelector);

                AIAnalysisResult result = callAIWithRetry(prompt, framework);

                circuitBreaker.recordSuccess();
                metrics.recordRequest(true, System.currentTimeMillis() - startTime);
                costMetrics.recordDomRequest(); // Track DOM cost
                logger.debug("{} DOM analysis completed successfully for: {} (Cost: ${})",
                        framework, description, 0.02);
                return result;

            } catch (Exception e) {
                circuitBreaker.recordFailure();
                metrics.recordRequest(false, System.currentTimeMillis() - startTime);
                logger.error("{} DOM analysis failed for: {}", framework, description, e);
                throw new RuntimeException("AI analysis failed", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<AIAnalysisResult> analyzeVisual(byte[] screenshot, String description) {
        if (!config.isVisualAnalysisEnabled()) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("Visual analysis is disabled in configuration")
            );
        }

        // Smart capability check: Does the configured AI provider support visual analysis?
        if (!config.getProvider().supportsVisualAnalysis()) {
            String message = String.format(
                "Visual analysis is not supported by the configured AI provider: %s. " +
                "Supported providers for visual analysis: %s. " +
                "Consider switching to OpenAI or enabling DOM-only analysis.",
                config.getProvider(),
                com.autoheal.model.AIProvider.getVisualAnalysisCapableProviders()
            );
            logger.warn(message);
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException(message)
            );
        }

        if (!circuitBreaker.canExecute()) {
            metrics.recordCircuitBreakerOpen();
            return CompletableFuture.failedFuture(
                    new CircuitBreakerOpenException("AI Service circuit breaker is open")
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Provider-specific visual analysis implementation
                AIAnalysisResult result = performProviderSpecificVisualAnalysis(screenshot, description, startTime);

                circuitBreaker.recordSuccess();
                metrics.recordRequest(true, System.currentTimeMillis() - startTime);
                logger.debug("Visual analysis completed for: {} -> {}", description, result.getRecommendedSelector());
                return result;

            } catch (Exception e) {
                circuitBreaker.recordFailure();
                metrics.recordRequest(false, System.currentTimeMillis() - startTime);
                logger.error("Visual analysis failed for: {}", description, e);
                throw new RuntimeException("Visual analysis failed", e);
            }
        }, executorService);
    }

    @Override
    public boolean isHealthy() {
        return !circuitBreaker.isOpen() && metrics.getSuccessRate() > 0.7;
    }

    @Override
    public AIServiceMetrics getMetrics() {
        return metrics;
    }

    public com.autoheal.metrics.CostMetrics getCostMetrics() {
        return costMetrics;
    }

    @Override
    public CompletableFuture<WebElement> selectBestMatchingElement(List<WebElement> elements, String description) {
        if (elements.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Elements list cannot be empty"));
        }

        if (elements.size() == 1) {
            return CompletableFuture.completedFuture(elements.get(0));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("AI disambiguation for {} elements with description: {}", elements.size(), description);

                // Build context information about each element
                StringBuilder elementsContext = new StringBuilder();
                for (int i = 0; i < elements.size(); i++) {
                    WebElement element = elements.get(i);
                    elementsContext.append(String.format("Element %d:\n", i + 1));
                    elementsContext.append(String.format("  Tag: %s\n", element.getTagName()));
                    elementsContext.append(String.format("  Text: %s\n", getElementText(element)));
                    elementsContext.append(String.format("  ID: %s\n", element.getAttribute("id")));
                    elementsContext.append(String.format("  Class: %s\n", element.getAttribute("class")));
                    elementsContext.append(String.format("  Name: %s\n", element.getAttribute("name")));
                    elementsContext.append(String.format("  Value: %s\n", element.getAttribute("value")));
                    elementsContext.append(String.format("  Aria-label: %s\n", element.getAttribute("aria-label")));
                    elementsContext.append(String.format("  Data-testid: %s\n", element.getAttribute("data-testid")));
                    elementsContext.append("\n");
                }

                String prompt = buildDisambiguationPrompt(description, elementsContext.toString());
                int selectedIndex = callAIForDisambiguation(prompt);

                // Validate the returned index
                if (selectedIndex < 1 || selectedIndex > elements.size()) {
                    logger.warn("AI returned invalid element index {}, falling back to first element", selectedIndex);
                    return elements.get(0);
                }

                WebElement selectedElement = elements.get(selectedIndex - 1); // Convert to 0-based index
                logger.debug("AI selected element {} out of {}", selectedIndex, elements.size());
                return selectedElement;

            } catch (Exception e) {
                logger.warn("AI disambiguation failed, returning first element: {}", e.getMessage());
                return elements.get(0);
            }
        }, executorService);
    }

    private List<AIProvider> initializeProviders(AIConfig config) {
        return Arrays.asList(
                config.getProvider(),
                AIProvider.MOCK  // Fallback to mock
        );
    }

    private String buildDOMAnalysisPrompt(String html, String description, String previousSelector) {
        return String.format("""
            You are a web automation expert. Find the best CSS selector for: "%s"

            The selector "%s" is broken. Analyze the HTML and find the correct element.

            HTML:
            %s

            REQUIREMENTS:
            - Look for elements with matching id, name, class, or text content
            - Prefer ID selectors (#id) when available
            - Ensure the selector matches exactly one element
            - The selector must be valid CSS syntax
            - Do NOT include any prefixes like "selector:" or "css:"

            Respond with valid JSON only:
            {
                "selector": "css-selector-here",
                "confidence": 0.95,
                "reasoning": "brief explanation",
                "alternatives": ["alt1", "alt2"]
            }
            """, description, previousSelector, html);
    }

    private String buildPlaywrightDOMAnalysisPrompt(String html, String description, String previousLocator) {
        return String.format("""
            You are a Playwright automation expert. Find the best user-facing locator for: "%s"

            The previous locator "%s" is broken. Analyze the HTML and find the correct element.

            HTML:
            %s

            PRIORITY ORDER (try in this sequence):
            1. getByRole() - ARIA role with accessible name (e.g., button, textbox, heading, link)
            2. getByLabel() - Form label text associated with input
            3. getByPlaceholder() - Input placeholder text
            4. getByText() - Visible text content
            5. getByTestId() - Test ID attribute (data-testid, data-test)
            6. CSS Selector - FALLBACK ONLY if no user-facing option exists

            RULES:
            - Prefer accessibility-first locators (1-5) over CSS selectors
            - Use CSS selector ONLY as last resort when no good user-facing option exists
            - Ensure locator is unique and stable
            - Avoid index-based or complex brittle selectors
            - For getByRole, include the accessible name in options if available

            Respond with valid JSON only:
            {
                "locatorType": "getByRole|getByLabel|getByPlaceholder|getByText|getByTestId|css",
                "value": "button|Username|.class-name",
                "options": {"name": "Submit"},
                "confidence": 0.95,
                "reasoning": "brief explanation why this locator was chosen",
                "alternatives": [
                    {"type": "css", "value": "#username"},
                    {"type": "getByTestId", "value": "user-input"}
                ]
            }

            Examples:
            - Button: {"locatorType": "getByRole", "value": "button", "options": {"name": "Submit"}}
            - Input with label: {"locatorType": "getByLabel", "value": "Username"}
            - Input with placeholder: {"locatorType": "getByPlaceholder", "value": "Enter email"}
            - Text element: {"locatorType": "getByText", "value": "Welcome"}
            - Fallback CSS: {"locatorType": "css", "value": ".user-input"}
            """, description, previousLocator, html);
    }

    private AIAnalysisResult callAIWithRetry(String prompt, com.autoheal.model.AutomationFramework framework) {
        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Route to framework-specific AI call
                return simulateAICall(prompt, framework);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("AI call failed after " + maxRetries + " attempts", lastException);
    }

    private AIAnalysisResult simulateAICall(String prompt, com.autoheal.model.AutomationFramework framework) {
        try {
            // Route to provider-specific implementation with framework awareness
            return performProviderSpecificDOMAnalysis(prompt, framework);
        } catch (Exception e) {
            logger.error("{} API call failed for framework {}", config.getProvider(), framework, e);
            throw new RuntimeException(config.getProvider() + " API call failed", e);
        }
    }

    /**
     * Shutdown the service and cleanup resources
     */
    public void shutdown() {
        executorService.shutdown();
        logger.info("ResilientAIService shutdown completed");
    }

    // ==================== DOM ANALYSIS HELPER METHODS ====================

    private AIAnalysisResult performProviderSpecificDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        switch (config.getProvider()) {
            case OPENAI:
                return performOpenAIDOMAnalysis(prompt, framework);
            case GOOGLE_GEMINI:
                return performGeminiDOMAnalysis(prompt, framework);
            case DEEPSEEK:
                return performDeepSeekDOMAnalysis(prompt, framework);
            case ANTHROPIC_CLAUDE:
                return performAnthropicDOMAnalysis(prompt, framework);
            case GROK:
                return performGrokDOMAnalysis(prompt, framework);
            case LOCAL_MODEL:
                return performLocalModelDOMAnalysis(prompt, framework);
            case MOCK:
                return performMockDOMAnalysis(prompt, framework);
            default:
                throw new UnsupportedOperationException(
                    String.format("DOM analysis not implemented for provider: %s", config.getProvider())
                );
        }
    }

    private AIAnalysisResult performOpenAIDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        // For now, delegate to Selenium-only version
        // TODO: Parse Playwright response format from OpenAI
        AIAnalysisResult result = performOpenAIDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performOpenAIDOMAnalysis(String prompt) {
        try {
            String requestBody = createOpenAIDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("OpenAI API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("OpenAI API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("OpenAI API response received (length: {})", responseBody.length());

                return parseDOMAnalysisResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("OpenAI DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performGeminiDOMAnalysis(String prompt) {
        try {
            String requestBody = createGeminiDOMRequestBody(prompt);
            String url = config.getApiUrl() + "/" + config.getModel() + ":generateContent";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Gemini API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("Gemini API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("Gemini API response received (length: {})", responseBody.length());

                return parseGeminiDOMResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gemini DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performGeminiDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        AIAnalysisResult result = performGeminiDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performDeepSeekDOMAnalysis(String prompt) {
        try {
            String requestBody = createDeepSeekDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("DeepSeek API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("DeepSeek API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("DeepSeek API response received (length: {})", responseBody.length());

                return parseDOMAnalysisResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performDeepSeekDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        AIAnalysisResult result = performDeepSeekDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performAnthropicDOMAnalysis(String prompt) {
        try {
            String requestBody = createAnthropicDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Anthropic API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("Anthropic API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("Anthropic API response received (length: {})", responseBody.length());

                return parseAnthropicDOMResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Anthropic DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performAnthropicDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        AIAnalysisResult result = performAnthropicDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performGrokDOMAnalysis(String prompt) {
        try {
            String requestBody = createGrokDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Grok API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("Grok API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("Grok API response received (length: {})", responseBody.length());

                return parseDOMAnalysisResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("Grok DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performGrokDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        AIAnalysisResult result = performGrokDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performLocalModelDOMAnalysis(String prompt) {
        // Detect if this is Ollama based on URL pattern
        boolean isOllama = config.getApiUrl() != null && config.getApiUrl().contains("11434");

        if (isOllama) {
            return performOllamaDOMAnalysis(prompt);
        } else {
            // Existing LOCAL_MODEL implementation for other local models
            return performStandardLocalModelDOMAnalysis(prompt);
        }
    }

    private AIAnalysisResult performLocalModelDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        AIAnalysisResult result = performLocalModelDOMAnalysis(prompt);
        return AIAnalysisResult.builder()
                .recommendedSelector(result.getRecommendedSelector())
                .playwrightLocator(result.getPlaywrightLocator())
                .targetFramework(framework)
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .alternatives(result.getAlternatives())
                .build();
    }

    private AIAnalysisResult performStandardLocalModelDOMAnalysis(String prompt) {
        try {
            String requestBody = createLocalModelDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Local model API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("Local model API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("Local model API response received (length: {})", responseBody.length());

                return parseDOMAnalysisResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("Local model DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performOllamaDOMAnalysis(String prompt) {
        try {
            String requestBody = createOllamaDOMRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Ollama API call failed with status: {}", response.code());
                    if (response.body() != null) {
                        logger.error("Error response: {}", response.body().string());
                    }
                    throw new RuntimeException("Ollama API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                logger.debug("Ollama API response received (length: {})", responseBody.length());

                return parseOllamaResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ollama DOM analysis failed", e);
        }
    }

    private AIAnalysisResult performMockDOMAnalysis(String prompt, com.autoheal.model.AutomationFramework framework) {
        logger.debug("Performing mock DOM analysis for framework: {}", framework);

        if (framework == com.autoheal.model.AutomationFramework.PLAYWRIGHT) {
            // Mock Playwright locator response
            com.autoheal.model.PlaywrightLocator mockLocator = com.autoheal.model.PlaywrightLocator.builder()
                    .byTestId("password1")
                    .build();

            return AIAnalysisResult.builder()
                    .playwrightLocator(mockLocator)
                    .targetFramework(com.autoheal.model.AutomationFramework.PLAYWRIGHT)
                    .confidence(0.9)
                    .reasoning("Mock Playwright analysis: Generated getByTestId locator for testing")
                    .alternatives(Arrays.asList(
                        new ElementCandidate("getByLabel('Password')", 0.8, "Mock label alternative", null, new HashMap<>()),
                        new ElementCandidate("getByPlaceholder('Enter password')", 0.7, "Mock placeholder", null, new HashMap<>())
                    ))
                    .build();
        } else {
            // Mock Selenium selector response
            return AIAnalysisResult.builder()
                    .recommendedSelector("input[data-test='password1']")
                    .targetFramework(com.autoheal.model.AutomationFramework.SELENIUM)
                    .confidence(0.9)
                    .reasoning("Mock Selenium analysis: Generated CSS selector for testing")
                    .alternatives(Arrays.asList(
                        new ElementCandidate("#password", 0.8, "Mock alternative", null, new HashMap<>()),
                        new ElementCandidate("input[name='password']", 0.7, "Mock name-based", null, new HashMap<>())
                    ))
                    .build();
        }
    }

    private String createOpenAIDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokensDOM());
        request.put("temperature", config.getTemperatureDOM());

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpenAI request body", e);
        }
    }

    private String createGeminiDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        ArrayNode contents = mapper.createArrayNode();
        ObjectNode content = mapper.createObjectNode();
        ArrayNode parts = mapper.createArrayNode();
        ObjectNode part = mapper.createObjectNode();

        String systemPrompt = "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.\n\n";

        part.put("text", systemPrompt + prompt);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        request.set("contents", contents);

        ObjectNode generationConfig = mapper.createObjectNode();
        generationConfig.put("temperature", config.getTemperatureDOM());
        generationConfig.put("maxOutputTokens", config.getMaxTokensDOM());
        request.set("generationConfig", generationConfig);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Gemini request body", e);
        }
    }

    private AIAnalysisResult parseGeminiDOMResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            logger.debug("Raw Gemini response content: {}", content);

            // Clean the content if needed (remove markdown formatting)
            String cleanContent = cleanMarkdown(content);
            logger.debug("Cleaned Gemini response content: {}", cleanContent);

            // Parse the JSON response from AI
            JsonNode aiResponse = mapper.readTree(cleanContent);

            // Detect if this is Playwright format or Selenium format
            if (aiResponse.has("locatorType")) {
                return parsePlaywrightDOMContent(aiResponse);
            } else {
                return parseSeleniumDOMContent(aiResponse);
            }

        } catch (Exception e) {
            logger.error("Failed to parse Gemini response. ResponseBody: {}", responseBody, e);
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private AIAnalysisResult parseDOMAnalysisResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("choices").get(0).path("message").path("content").asText();
            logger.debug("Raw AI response content: {}", content);

            // Clean the content if needed (remove markdown formatting)
            String cleanContent = cleanMarkdown(content);
            logger.debug("Cleaned AI response content: {}", cleanContent);

            // Parse the JSON response from AI
            JsonNode aiResponse = mapper.readTree(cleanContent);

            // Detect if this is Playwright format or Selenium format
            if (aiResponse.has("locatorType")) {
                return parsePlaywrightDOMContent(aiResponse);
            } else {
                return parseSeleniumDOMContent(aiResponse);
            }

        } catch (Exception e) {
            logger.error("Failed to parse OpenAI response. ResponseBody: {}", responseBody, e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    /**
     * Clean markdown formatting from AI response content
     */
    private String cleanMarkdown(String content) {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```json")) {
            cleanContent = cleanContent.substring(7);
        }
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.substring(3);
        }
        if (cleanContent.endsWith("```")) {
            cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
        }
        return cleanContent.trim();
    }

    /**
     * Parse Playwright-specific DOM analysis response
     */
    private AIAnalysisResult parsePlaywrightDOMContent(JsonNode aiResponse) {
        try {
            String locatorType = aiResponse.path("locatorType").asText();
            String value = aiResponse.path("value").asText();
            double confidence = aiResponse.path("confidence").asDouble(0.8);
            String reasoning = aiResponse.path("reasoning").asText("AI-generated Playwright locator");

            logger.debug("Parsed Playwright locator: type='{}', value='{}', confidence={}",
                        locatorType, value, confidence);

            // Build PlaywrightLocator
            com.autoheal.model.PlaywrightLocator.Builder locatorBuilder = com.autoheal.model.PlaywrightLocator.builder();

            // Parse options if present
            JsonNode optionsNode = aiResponse.path("options");
            java.util.Map<String, Object> options = new HashMap<>();
            if (optionsNode.isObject()) {
                optionsNode.fields().forEachRemaining(entry -> {
                    options.put(entry.getKey(), entry.getValue().asText());
                });
            }

            // Build locator based on type
            com.autoheal.model.PlaywrightLocator playwrightLocator;
            switch (locatorType.toLowerCase()) {
                case "getbyrole":
                    String roleName = options.containsKey("name") ? (String) options.get("name") : null;
                    if (roleName != null) {
                        playwrightLocator = locatorBuilder.byRole(value, roleName).build();
                    } else {
                        playwrightLocator = locatorBuilder.byRole(value).build();
                    }
                    break;
                case "getbylabel":
                    playwrightLocator = locatorBuilder.byLabel(value).build();
                    break;
                case "getbyplaceholder":
                    playwrightLocator = locatorBuilder.byPlaceholder(value).build();
                    break;
                case "getbytext":
                    playwrightLocator = locatorBuilder.byText(value).build();
                    break;
                case "getbyalttext":
                    playwrightLocator = locatorBuilder.byAltText(value).build();
                    break;
                case "getbytitle":
                    playwrightLocator = locatorBuilder.byTitle(value).build();
                    break;
                case "getbytestid":
                    playwrightLocator = locatorBuilder.byTestId(value).build();
                    break;
                case "css":
                    playwrightLocator = locatorBuilder.cssSelector(value).build();
                    break;
                case "xpath":
                    playwrightLocator = locatorBuilder.xpath(value).build();
                    break;
                default:
                    logger.warn("Unknown Playwright locator type: {}, falling back to CSS", locatorType);
                    playwrightLocator = locatorBuilder.cssSelector(value).build();
            }

            // Parse alternatives
            List<ElementCandidate> alternatives = new ArrayList<>();
            JsonNode alternativesNode = aiResponse.path("alternatives");
            if (alternativesNode.isArray()) {
                for (JsonNode alt : alternativesNode) {
                    if (alt.isObject()) {
                        String altType = alt.path("type").asText();
                        String altValue = alt.path("value").asText();
                        String altSelector = altType + "('" + altValue + "')";
                        alternatives.add(new ElementCandidate(altSelector, confidence * 0.8,
                            "Alternative Playwright locator", null, new HashMap<>()));
                    } else if (alt.isTextual()) {
                        alternatives.add(new ElementCandidate(alt.asText(), confidence * 0.8,
                            "Alternative selector", null, new HashMap<>()));
                    }
                }
            }

            return AIAnalysisResult.builder()
                    .playwrightLocator(playwrightLocator)
                    .targetFramework(com.autoheal.model.AutomationFramework.PLAYWRIGHT)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .alternatives(alternatives)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to parse Playwright DOM content", e);
            throw new RuntimeException("Failed to parse Playwright DOM content", e);
        }
    }

    /**
     * Parse Selenium-specific DOM analysis response (CSS selectors)
     */
    private AIAnalysisResult parseSeleniumDOMContent(JsonNode aiResponse) {
        try {
            String selector = aiResponse.path("selector").asText();
            double confidence = aiResponse.path("confidence").asDouble(0.8);
            String reasoning = aiResponse.path("reasoning").asText("AI-generated selector");

            logger.debug("Parsed Selenium selector: '{}', confidence: {}", selector, confidence);

            List<ElementCandidate> alternatives = new ArrayList<>();
            JsonNode alternativesNode = aiResponse.path("alternatives");
            if (alternativesNode.isArray()) {
                for (JsonNode alt : alternativesNode) {
                    if (alt.isTextual()) {
                        alternatives.add(new ElementCandidate(alt.asText(), confidence * 0.8,
                            "Alternative selector", null, new HashMap<>()));
                    }
                }
            }

            return AIAnalysisResult.builder()
                    .recommendedSelector(selector)
                    .targetFramework(com.autoheal.model.AutomationFramework.SELENIUM)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .alternatives(alternatives)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to parse Selenium DOM content", e);
            throw new RuntimeException("Failed to parse Selenium DOM content", e);
        }
    }

    private String createDeepSeekDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokensDOM());
        request.put("temperature", config.getTemperatureDOM());

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DeepSeek request body", e);
        }
    }

    private String createAnthropicDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokensDOM());

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.\n\n" + prompt);

        messages.add(message);
        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Anthropic request body", e);
        }
    }

    private String createGrokDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokensDOM());
        request.put("temperature", config.getTemperatureDOM());

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Grok request body", e);
        }
    }

    private String createLocalModelDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", config.getMaxTokensDOM());
        request.put("temperature", config.getTemperatureDOM());

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Local Model request body", e);
        }
    }

    private AIAnalysisResult parseAnthropicDOMResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("content")
                    .path(0)
                    .path("text")
                    .asText();

            logger.debug("Raw Anthropic response content: {}", content);

            // Clean the content if needed (remove markdown formatting)
            String cleanContent = cleanMarkdown(content);
            logger.debug("Cleaned Anthropic response content: {}", cleanContent);

            // Parse the JSON response from AI
            JsonNode aiResponse = mapper.readTree(cleanContent);

            // Detect if this is Playwright format or Selenium format
            if (aiResponse.has("locatorType")) {
                return parsePlaywrightDOMContent(aiResponse);
            } else {
                return parseSeleniumDOMContent(aiResponse);
            }

        } catch (Exception e) {
            logger.error("Failed to parse Anthropic response. ResponseBody: {}", responseBody, e);
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    /**
     * Convert AI selector to proper format based on selector type
     */
    private String convertAISelector(String selector, String selectorType) {
        if (selector == null || selector.isEmpty()) {
            return selector;
        }

        try {
            switch (selectorType.toUpperCase()) {
                case "ID":
                    // For ID type, ensure it doesn't have # prefix (as it will be added by By.id())
                    return selector.startsWith("#") ? selector.substring(1) : selector;

                case "CSS_SELECTOR":
                    // CSS selector should be returned as-is
                    return selector;

                case "XPATH":
                    // XPath should be returned as-is
                    return selector;

                case "NAME":
                case "CLASS_NAME":
                case "TAG_NAME":
                case "LINK_TEXT":
                case "PARTIAL_LINK_TEXT":
                    // These should be returned as-is
                    return selector;

                default:
                    logger.warn("Unknown selector type '{}', treating as CSS selector", selectorType);
                    return selector;
            }
        } catch (Exception e) {
            logger.warn("Error converting AI selector '{}' of type '{}', returning as-is: {}",
                       selector, selectorType, e.getMessage());
            return selector;
        }
    }

    // ==================== VISUAL ANALYSIS HELPER METHODS ====================

    private String createVisualAnalysisPrompt(String description) {
        return createEnhancedVisualAnalysisPrompt(description);
    }

    private String createEnhancedVisualAnalysisPrompt(String description) {
        return String.format("""
            You are an expert web automation engineer. Analyze the provided screenshot to locate an element described as: "%s"

            Generate ROBUST selectors that will survive DOM changes by identifying:
            1. Multiple identification strategies (ID, class, attributes, text content)
            2. Stable visual landmarks nearby for relative positioning
            3. Hierarchical relationships with parent containers
            4. Text-based selectors using visible content
            5. Semantic attributes like data-testid, aria-label, role

            Respond in JSON format:
            {
                "primary_selector": "most reliable CSS selector",
                "alternative_selectors": ["fallback option 1", "fallback option 2", "fallback option 3"],
                "visual_landmarks": ["stable nearby elements for context"],
                "text_based_selector": "selector using visible text if available",
                "attribute_hints": ["data-testid suggestions", "aria-label suggestions", "role suggestions"],
                "relative_position": "position relative to stable UI elements",
                "element_description": "what you see that matches the description",
                "confidence": 0.85,
                "stability_reasoning": "why these selectors should survive DOM changes"
            }

            Focus on creating selectors that are:
            - Resilient to DOM structure changes
            - Multiple fallback options available
            - Based on semantic meaning and accessibility attributes
            - Contextually aware of surrounding stable elements
            """, description);
    }

    private String createSimpleVisualAnalysisPrompt(String description) {
        return String.format("""
            Analyze this screenshot to locate an element described as: "%s"

            Find the element and provide a CSS selector for it.

            Respond in JSON format:
            {
                "selector": "css-selector-here",
                "confidence": 0.85,
                "reasoning": "brief explanation"
            }
            """, description);
    }

    private String createVisualAnalysisRequestBody(String prompt, String base64Image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("model", "gpt-4o-mini"); // GPT-4o-mini supports vision and is cost-effective
            root.put("max_tokens", 1000);
            
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode message = mapper.createObjectNode();
            message.put("role", "user");
            
            ArrayNode content = mapper.createArrayNode();
            
            // Add text prompt
            ObjectNode textContent = mapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            content.add(textContent);
            
            // Add image
            ObjectNode imageContent = mapper.createObjectNode();
            imageContent.put("type", "image_url");
            ObjectNode imageUrl = mapper.createObjectNode();
            imageUrl.put("url", "data:image/png;base64," + base64Image);
            imageContent.set("image_url", imageUrl);
            content.add(imageContent);
            
            message.set("content", content);
            messages.add(message);
            root.set("messages", messages);
            
            return mapper.writeValueAsString(root);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create visual analysis request body", e);
        }
    }

    private String createGeminiVisualAnalysisRequestBody(String prompt, String base64Image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();

            ArrayNode contents = mapper.createArrayNode();
            ObjectNode content = mapper.createObjectNode();
            ArrayNode parts = mapper.createArrayNode();

            // Add text prompt
            ObjectNode textPart = mapper.createObjectNode();
            textPart.put("text", prompt);
            parts.add(textPart);

            // Add image
            ObjectNode imagePart = mapper.createObjectNode();
            ObjectNode inlineData = mapper.createObjectNode();
            inlineData.put("mimeType", "image/png");
            inlineData.put("data", base64Image);
            imagePart.set("inlineData", inlineData);
            parts.add(imagePart);

            content.set("parts", parts);
            contents.add(content);
            root.set("contents", contents);

            ObjectNode generationConfig = mapper.createObjectNode();
            generationConfig.put("temperature", config.getTemperatureVisual());
            generationConfig.put("maxOutputTokens", config.getMaxTokensVisual());
            root.set("generationConfig", generationConfig);

            return mapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Gemini visual analysis request body", e);
        }
    }

    private AIAnalysisResult parseGeminiVisualAnalysisResponse(String responseBody, String description) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            logger.debug("Raw Gemini visual response content: {}", content);

            // Parse the JSON response from the AI (clean markdown formatting if present)
            String cleanContent = content.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7); // Remove ```json
            }
            if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3); // Remove ```
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3); // Remove closing ```
            }
            cleanContent = cleanContent.trim();

            logger.debug("Cleaned Gemini visual response content: {}", cleanContent);

            // Try enhanced parsing first
            try {
                return parseEnhancedVisualResponse(mapper.readTree(cleanContent), description);
            } catch (Exception enhancedError) {
                logger.warn("Enhanced visual parsing failed, trying legacy format: {}", enhancedError.getMessage());
                // Fallback to legacy parsing
                return parseLegacyVisualResponse(mapper.readTree(cleanContent), description);
            }

        } catch (Exception e) {
            logger.warn("All Gemini visual parsing methods failed, using final fallback", e);
            // Final fallback result
            return createFallbackVisualResult(description);
        }
    }

    private AIAnalysisResult parseVisualAnalysisResponse(String responseBody, String description) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            logger.debug("Raw AI response content: {}", content);

            // Parse the JSON response from the AI (clean markdown formatting if present)
            String cleanContent = content.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7); // Remove ```json
            }
            if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3); // Remove ```
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3); // Remove closing ```
            }
            cleanContent = cleanContent.trim();

            logger.debug("Cleaned AI response content: {}", cleanContent);

            // Try enhanced parsing first
            try {
                return parseEnhancedVisualResponse(mapper.readTree(cleanContent), description);
            } catch (Exception enhancedError) {
                logger.warn("Enhanced visual parsing failed, trying legacy format: {}", enhancedError.getMessage());
                // Fallback to legacy parsing
                return parseLegacyVisualResponse(mapper.readTree(cleanContent), description);
            }

        } catch (Exception e) {
            logger.warn("All visual parsing methods failed, using final fallback", e);

            // Final fallback result
            return createFallbackVisualResult(description);
        }
    }

    /**
     * Parse enhanced visual response with robust selectors
     */
    private AIAnalysisResult parseEnhancedVisualResponse(JsonNode aiResponse, String description) {
        // Extract primary selector (new field name)
        String primarySelector = aiResponse.path("primary_selector").asText();
        if (primarySelector.isEmpty()) {
            throw new IllegalArgumentException("No primary_selector found in enhanced response");
        }

        double confidence = aiResponse.path("confidence").asDouble(0.5);

        // Combine reasoning with stability reasoning
        String reasoning = aiResponse.path("stability_reasoning").asText();
        if (reasoning.isEmpty()) {
            reasoning = aiResponse.path("reasoning").asText();
        }

        List<ElementCandidate> alternatives = new ArrayList<>();

        // Process alternative_selectors array
        JsonNode alternativesNode = aiResponse.path("alternative_selectors");
        if (alternativesNode.isArray()) {
            for (JsonNode alt : alternativesNode) {
                String altSelector = alt.asText();
                if (!altSelector.isEmpty()) {
                    ElementCandidate candidate = new ElementCandidate(
                            altSelector,
                            confidence * 0.9, // High confidence for planned alternatives
                            description + " (alternative)",
                            null,
                            new HashMap<>()
                    );
                    alternatives.add(candidate);
                }
            }
        }

        // Add text-based selector as alternative if available
        String textBasedSelector = aiResponse.path("text_based_selector").asText();
        if (!textBasedSelector.isEmpty()) {
            ElementCandidate textCandidate = new ElementCandidate(
                    textBasedSelector,
                    confidence * 0.8, // Good confidence for text-based selectors
                    description + " (text-based)",
                    null,
                    new HashMap<>()
            );
            alternatives.add(textCandidate);
        }

        // Add attribute-based suggestions as alternatives
        JsonNode attributeHints = aiResponse.path("attribute_hints");
        if (attributeHints.isArray()) {
            for (JsonNode hint : attributeHints) {
                String attrSelector = hint.asText();
                if (!attrSelector.isEmpty()) {
                    ElementCandidate attrCandidate = new ElementCandidate(
                            attrSelector,
                            confidence * 0.7, // Moderate confidence for attribute hints
                            description + " (attribute-based)",
                            null,
                            new HashMap<>()
                    );
                    alternatives.add(attrCandidate);
                }
            }
        }

        // Enhance reasoning with visual context
        String elementDescription = aiResponse.path("element_description").asText();
        String relativePosition = aiResponse.path("relative_position").asText();
        if (!elementDescription.isEmpty() || !relativePosition.isEmpty()) {
            reasoning += " | Visual context: " + elementDescription;
            if (!relativePosition.isEmpty()) {
                reasoning += " | Position: " + relativePosition;
            }
        }

        // Log visual landmarks for debugging
        JsonNode landmarks = aiResponse.path("visual_landmarks");
        if (landmarks.isArray() && landmarks.size() > 0) {
            logger.debug("Enhanced visual landmarks identified: {}", landmarks.toString());
        }

        logger.debug("Enhanced visual parsing succeeded - Primary: {}, Alternatives: {}",
                    primarySelector, alternatives.size());

        return AIAnalysisResult.builder()
                .recommendedSelector(primarySelector)
                .confidence(confidence)
                .reasoning("Enhanced: " + reasoning)
                .alternatives(alternatives)
                .build();
    }

    /**
     * Parse legacy visual response format as fallback
     */
    private AIAnalysisResult parseLegacyVisualResponse(JsonNode aiResponse, String description) {
        String selector = aiResponse.path("selector").asText();
        if (selector.isEmpty()) {
            throw new IllegalArgumentException("No selector found in legacy response format");
        }

        double confidence = aiResponse.path("confidence").asDouble(0.5);
        String reasoning = aiResponse.path("reasoning").asText();

        List<ElementCandidate> alternatives = new ArrayList<>();
        JsonNode alternativesNode = aiResponse.path("alternatives");
        if (alternativesNode.isArray()) {
            for (JsonNode alt : alternativesNode) {
                String altSelector = alt.asText();
                if (!altSelector.isEmpty()) {
                    ElementCandidate candidate = new ElementCandidate(
                            altSelector,
                            confidence * 0.8, // Slightly lower confidence for legacy alternatives
                            description + " (legacy alternative)",
                            null,
                            new HashMap<>()
                    );
                    alternatives.add(candidate);
                }
            }
        }

        logger.debug("Legacy visual parsing succeeded - Primary: {}, Alternatives: {}",
                    selector, alternatives.size());

        return AIAnalysisResult.builder()
                .recommendedSelector(selector)
                .confidence(confidence)
                .reasoning("Legacy: " + reasoning)
                .alternatives(alternatives)
                .build();
    }

    /**
     * Create final fallback result when all parsing fails
     */
    private AIAnalysisResult createFallbackVisualResult(String description) {
        logger.warn("Creating emergency fallback visual result for: {}", description);

        List<ElementCandidate> emergencyAlternatives = new ArrayList<>();

        // Generate common selector patterns as emergency alternatives
        if (description.toLowerCase().contains("button")) {
            emergencyAlternatives.add(new ElementCandidate(
                "button", 0.4, description + " (button fallback)", null, new HashMap<>()
            ));
            emergencyAlternatives.add(new ElementCandidate(
                "input[type='submit']", 0.3, description + " (submit fallback)", null, new HashMap<>()
            ));
        }

        if (description.toLowerCase().contains("input") || description.toLowerCase().contains("field")) {
            emergencyAlternatives.add(new ElementCandidate(
                "input", 0.4, description + " (input fallback)", null, new HashMap<>()
            ));
        }

        return AIAnalysisResult.builder()
                .recommendedSelector("*[contains(text(), '" + description.split(" ")[0] + "')]")
                .confidence(0.2)
                .reasoning("Emergency fallback: All visual parsing methods failed, using text-based selector")
                .alternatives(emergencyAlternatives)
                .build();
    }

    /**
     * Provider-specific visual analysis implementation
     * Routes to appropriate provider based on configuration
     */
    private AIAnalysisResult performProviderSpecificVisualAnalysis(byte[] screenshot, String description, long startTime) {
        switch (config.getProvider()) {
            case OPENAI:
                return performOpenAIVisualAnalysis(screenshot, description, startTime);
            case GOOGLE_GEMINI:
                return performGeminiVisualAnalysis(screenshot, description, startTime);
            case MOCK:
                return performMockVisualAnalysis(description);
            default:
                throw new UnsupportedOperationException(
                    String.format("Visual analysis not implemented for provider: %s", config.getProvider())
                );
        }
    }

    /**
     * OpenAI-specific visual analysis using their vision API
     */
    private AIAnalysisResult performOpenAIVisualAnalysis(byte[] screenshot, String description, long startTime) {
        try {
            // Encode screenshot to base64 for OpenAI API
            String base64Image = java.util.Base64.getEncoder().encodeToString(screenshot);

            // Create OpenAI visual analysis request
            String prompt = createVisualAnalysisPrompt(description);
            String requestBody = createVisualAnalysisRequestBody(prompt, base64Image);

            // Make API call to OpenAI Vision
            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseVisualAnalysisResponse(responseBody, description);
            }

        } catch (Exception e) {
            logger.error("OpenAI visual analysis failed for: {}", description, e);
            throw new RuntimeException("OpenAI visual analysis failed", e);
        }
    }

    /**
     * Google Gemini-specific visual analysis using their vision API
     */
    private AIAnalysisResult performGeminiVisualAnalysis(byte[] screenshot, String description, long startTime) {
        try {
            // Encode screenshot to base64 for Gemini API
            String base64Image = java.util.Base64.getEncoder().encodeToString(screenshot);

            // Create Gemini visual analysis request
            String prompt = createVisualAnalysisPrompt(description);
            String requestBody = createGeminiVisualAnalysisRequestBody(prompt, base64Image);
            String url = config.getApiUrl() + "/" + config.getModel() + ":generateContent";

            // Make API call to Gemini Vision
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Gemini API call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseGeminiVisualAnalysisResponse(responseBody, description);
            }

        } catch (Exception e) {
            logger.error("Gemini visual analysis failed for: {}", description, e);
            throw new RuntimeException("Gemini visual analysis failed", e);
        }
    }

    /**
     * Mock visual analysis for testing
     */
    private AIAnalysisResult performMockVisualAnalysis(String description) {
        logger.debug("Performing mock visual analysis for: {}", description);

        // Generate a reasonable mock selector based on description
        String mockSelector = generateMockSelectorFromDescription(description);

        return AIAnalysisResult.builder()
                .recommendedSelector(mockSelector)
                .confidence(0.9)
                .reasoning("Mock visual analysis: Generated selector based on description")
                .alternatives(Arrays.asList(
                    new ElementCandidate("input[type='text']", 0.8, "Mock alternative", null, new HashMap<>()),
                    new ElementCandidate("*[contains(@placeholder, 'user')]", 0.7, "Mock XPath alternative", null, new HashMap<>())
                ))
                .build();
    }

    /**
     * Generate a mock selector based on the element description
     */
    private String generateMockSelectorFromDescription(String description) {
        String desc = description.toLowerCase();

        if (desc.contains("username") || desc.contains("user")) {
            return "#user-name";
        } else if (desc.contains("password")) {
            return "#password";
        } else if (desc.contains("login") && desc.contains("button")) {
            return "#login-button";
        } else if (desc.contains("button")) {
            return "button[type='submit']";
        } else if (desc.contains("input") || desc.contains("field")) {
            return "input[type='text']";
        } else {
            return "*[contains(text(), '" + description.split(" ")[0] + "')]";
        }
    }

    // ==================== DISAMBIGUATION HELPER METHODS ====================

    private String getElementText(WebElement element) {
        try {
            String text = element.getText();
            return text != null && !text.trim().isEmpty() ? text.trim() : "No visible text";
        } catch (Exception e) {
            return "Unable to retrieve text";
        }
    }

    private String buildDisambiguationPrompt(String description, String elementsContext) {
        return String.format("""
            You are a web automation expert helping to identify the correct element from multiple candidates.

            Target element description: "%s"

            Available elements:
            %s

            TASK: Select the element that BEST matches the description "%s".

            Consider:
            1. Text content that matches the description
            2. Element attributes (id, class, name, aria-label, data-testid)
            3. Element type and context
            4. Semantic meaning

            IMPORTANT: Respond with ONLY the element number (1, 2, 3, etc.) that best matches the description.
            Do not provide any explanation or additional text.

            Element number:
            """, description, elementsContext, description);
    }

    private int callAIForDisambiguation(String prompt) {
        try {
            return performProviderSpecificDisambiguation(prompt);
        } catch (Exception e) {
            logger.error("AI disambiguation call failed", e);
            throw new RuntimeException("AI disambiguation call failed", e);
        }
    }

    private int performProviderSpecificDisambiguation(String prompt) {
        switch (config.getProvider()) {
            case OPENAI:
                return performOpenAIDisambiguation(prompt);
            case GOOGLE_GEMINI:
                return performGeminiDisambiguation(prompt);
            case DEEPSEEK:
                return performDeepSeekDisambiguation(prompt);
            case ANTHROPIC_CLAUDE:
                return performAnthropicDisambiguation(prompt);
            case GROK:
                return performGrokDisambiguation(prompt);
            case LOCAL_MODEL:
                return performLocalModelDisambiguation(prompt);
            case MOCK:
                return 1; // Always return first element for mock
            default:
                logger.warn("Disambiguation not implemented for provider: {}, returning first element", config.getProvider());
                return 1;
        }
    }

    private int performOpenAIDisambiguation(String prompt) {
        try {
            String requestBody = createOpenAIDisambiguationRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseOpenAIDisambiguationResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("OpenAI disambiguation failed", e);
        }
    }

    private int performGeminiDisambiguation(String prompt) {
        try {
            String requestBody = createGeminiDisambiguationRequestBody(prompt);
            String url = config.getApiUrl() + "/" + config.getModel() + ":generateContent";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Gemini disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseGeminiDisambiguationResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gemini disambiguation failed", e);
        }
    }

    private String createOpenAIDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", 10);
        request.put("temperature", 0.1);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpenAI disambiguation request body", e);
        }
    }

    private String createGeminiDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        ArrayNode contents = mapper.createArrayNode();
        ObjectNode content = mapper.createObjectNode();
        ArrayNode parts = mapper.createArrayNode();
        ObjectNode part = mapper.createObjectNode();

        String systemPrompt = "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.\n\n";

        part.put("text", systemPrompt + prompt);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        request.set("contents", contents);

        ObjectNode generationConfig = mapper.createObjectNode();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 10);
        request.set("generationConfig", generationConfig);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Gemini disambiguation request body", e);
        }
    }

    private int parseOpenAIDisambiguationResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("choices").get(0).path("message").path("content").asText().trim();

            // Parse the number from the response
            try {
                return Integer.parseInt(content);
            } catch (NumberFormatException e) {
                // Try to extract number from text
                String numberOnly = content.replaceAll("\\D+", "");
                if (!numberOnly.isEmpty()) {
                    return Integer.parseInt(numberOnly);
                }
                throw e;
            }

        } catch (Exception e) {
            logger.error("Failed to parse OpenAI disambiguation response: {}", responseBody, e);
            return 1; // Default to first element
        }
    }

    private int parseGeminiDisambiguationResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText().trim();

            // Parse the number from the response
            try {
                return Integer.parseInt(content);
            } catch (NumberFormatException e) {
                // Try to extract number from text
                String numberOnly = content.replaceAll("\\D+", "");
                if (!numberOnly.isEmpty()) {
                    return Integer.parseInt(numberOnly);
                }
                throw e;
            }

        } catch (Exception e) {
            logger.error("Failed to parse Gemini disambiguation response: {}", responseBody, e);
            return 1; // Default to first element
        }
    }

    private int performDeepSeekDisambiguation(String prompt) {
        try {
            String requestBody = createDeepSeekDisambiguationRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("DeepSeek disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseOpenAIDisambiguationResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek disambiguation failed", e);
        }
    }

    private int performAnthropicDisambiguation(String prompt) {
        try {
            String requestBody = createAnthropicDisambiguationRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("x-api-key", config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Anthropic disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseAnthropicDisambiguationResponse(responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Anthropic disambiguation failed", e);
        }
    }

    private int performGrokDisambiguation(String prompt) {
        try {
            String requestBody = createGrokDisambiguationRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Grok disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseOpenAIDisambiguationResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("Grok disambiguation failed", e);
        }
    }

    private int performLocalModelDisambiguation(String prompt) {
        try {
            String requestBody = createLocalModelDisambiguationRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Local model disambiguation call failed: " + response.code());
                }

                String responseBody = response.body().string();
                return parseOpenAIDisambiguationResponse(responseBody); // Uses OpenAI-compatible format
            }
        } catch (Exception e) {
            throw new RuntimeException("Local model disambiguation failed", e);
        }
    }

    private String createDeepSeekDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", 10);
        request.put("temperature", 0.1);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DeepSeek disambiguation request body", e);
        }
    }

    private String createAnthropicDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", 10);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        message.put("content", "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.\n\n" + prompt);

        messages.add(message);
        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Anthropic disambiguation request body", e);
        }
    }

    private String createGrokDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", 10);
        request.put("temperature", 0.1);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Grok disambiguation request body", e);
        }
    }

    private String createLocalModelDisambiguationRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());
        request.put("max_tokens", 10);
        request.put("temperature", 0.1);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a web automation expert. When given multiple elements and a description, " +
            "respond with only the number of the element that best matches the description. " +
            "Respond with just the number, no other text.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Local Model disambiguation request body", e);
        }
    }

    private int parseAnthropicDisambiguationResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            String content = root.path("content")
                    .path(0)
                    .path("text")
                    .asText().trim();

            // Parse the number from the response
            try {
                return Integer.parseInt(content);
            } catch (NumberFormatException e) {
                // Try to extract number from text
                String numberOnly = content.replaceAll("\\D+", "");
                if (!numberOnly.isEmpty()) {
                    return Integer.parseInt(numberOnly);
                }
                throw e;
            }

        } catch (Exception e) {
            logger.error("Failed to parse Anthropic disambiguation response: {}", responseBody, e);
            return 1; // Default to first element
        }
    }

    // ==================== OLLAMA-SPECIFIC HELPER METHODS ====================

    private String createOllamaDOMRequestBody(String prompt) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();

        request.put("model", config.getModel());

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are an expert web automation engineer. Analyze HTML DOM to find the correct CSS selector for elements. " +
            "Always respond with valid JSON containing: selector, confidence (0.0-1.0), reasoning, and alternatives array.");

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        messages.add(systemMessage);
        messages.add(userMessage);

        request.set("messages", messages);

        try {
            return mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Ollama request body", e);
        }
    }

    private AIAnalysisResult parseOllamaResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Ollama returns streaming JSON chunks, we need to parse the last complete response
            String[] lines = responseBody.split("\n");
            String lastValidJson = null;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("{") && line.contains("\"done\":true")) {
                    try {
                        JsonNode testParse = mapper.readTree(line);
                        if (testParse.has("message")) {
                            lastValidJson = testParse.path("message").path("content").asText();
                            break;
                        }
                    } catch (Exception ignored) {
                        // Continue to next line
                    }
                } else if (line.startsWith("{") && line.contains("\"message\"")) {
                    try {
                        JsonNode testParse = mapper.readTree(line);
                        if (testParse.has("message")) {
                            String content = testParse.path("message").path("content").asText();
                            if (lastValidJson == null) {
                                lastValidJson = content;
                            } else {
                                lastValidJson += content;
                            }
                        }
                    } catch (Exception ignored) {
                        // Continue to next line
                    }
                }
            }

            if (lastValidJson == null || lastValidJson.trim().isEmpty()) {
                throw new RuntimeException("No valid content found in Ollama response");
            }

            logger.debug("Ollama combined content: {}", lastValidJson);

            // Clean the content if needed (remove markdown formatting)
            String cleanContent = lastValidJson.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7);
            }
            if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3);
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
            }
            cleanContent = cleanContent.trim();

            logger.debug("Cleaned Ollama content: {}", cleanContent);

            // Parse the JSON response from AI
            JsonNode aiResponse = mapper.readTree(cleanContent);

            String selector = aiResponse.path("selector").asText();
            double confidence = aiResponse.path("confidence").asDouble(0.8);
            String reasoning = aiResponse.path("reasoning").asText("Ollama-generated selector");

            logger.debug("Parsed Ollama selector: '{}', confidence: {}", selector, confidence);

            List<ElementCandidate> alternatives = new ArrayList<>();
            JsonNode alternativesNode = aiResponse.path("alternatives");
            if (alternativesNode.isArray()) {
                for (JsonNode alt : alternativesNode) {
                    if (alt.isTextual()) {
                        alternatives.add(new ElementCandidate(alt.asText(), confidence * 0.8,
                            "Ollama alternative selector", null, new HashMap<>()));
                    }
                }
            }

            return AIAnalysisResult.builder()
                    .recommendedSelector(selector)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .alternatives(alternatives)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to parse Ollama response. ResponseBody: {}", responseBody, e);
            throw new RuntimeException("Failed to parse Ollama response", e);
        }
    }

}