# Development Guide

Contributing to AutoHeal Locator development.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Git
- IDE (IntelliJ IDEA recommended)
- OpenAI API key for testing

## Development Setup

### 1. Clone and Setup

```bash
git clone https://github.com/yourusername/autoheal-locator.git
cd autoheal-locator

# Install dependencies
mvn clean install
```

### 2. IDE Configuration

**IntelliJ IDEA:**
1. Import as Maven project
2. Enable annotation processing
3. Install recommended plugins:
   - Lombok
   - SonarLint
   - CheckStyle

### 3. Environment Variables

Create `.env` file (not committed):
```bash
OPENAI_API_KEY=your-test-api-key
TEST_BROWSER=chrome
```

## Project Structure

```
src/
├── main/java/com/autoheal/
│   ├── core/                    # Core interfaces
│   ├── impl/                    # Implementations
│   ├── config/                  # Configuration classes
│   ├── model/                   # Data models
│   ├── exception/               # Custom exceptions
│   ├── monitoring/              # Metrics and health
│   └── util/                    # Utilities
├── test/java/com/autoheal/
│   ├── unit/                    # Unit tests
│   ├── integration/             # Integration tests
│   └── performance/             # Performance tests
└── test/resources/              # Test configurations
```

## Coding Standards

### Code Style

We use Google Java Style with modifications:

```xml
<!-- In pom.xml -->
<plugin>
    <groupId>com.google.googlejavaformat</groupId>
    <artifactId>google-java-format-maven-plugin</artifactId>
    <configuration>
        <style>GOOGLE</style>
    </configuration>
</plugin>
```

**Key Rules:**
- 2-space indentation
- 100-character line limit
- No wildcard imports
- Use Optional for nullable returns
- Builder pattern for complex objects

### Documentation

- **JavaDoc**: Required for all public APIs
- **README**: Update for significant changes
- **CHANGELOG**: Document all changes

Example JavaDoc:
```java
/**
 * Locates web elements using AI-powered healing strategies.
 *
 * @param description Natural language description of the element
 * @param fallbackSelector Optional CSS/XPath selector as fallback
 * @return Located WebElement
 * @throws ElementNotFoundException if element cannot be found
 * @throws AutoHealException if configuration or AI service issues occur
 */
public WebElement findElement(String description, String fallbackSelector) {
    // Implementation
}
```

## Testing Guidelines

### Test Structure

```java
@Test
@DisplayName("Should find element using AI when CSS selector fails")
void shouldFindElementUsingAIWhenCSSSelectorFails() {
    // Given
    when(driver.findElement(By.cssSelector("#missing"))).thenThrow(NoSuchElementException.class);
    when(aiService.analyzeElement(any())).thenReturn(successfulResult);

    // When
    WebElement result = locator.findElement("Submit button", "#missing");

    // Then
    assertThat(result).isNotNull();
    verify(aiService).analyzeElement(any());
}
```

### Test Categories

1. **Unit Tests**: Fast, isolated, mocked dependencies
2. **Integration Tests**: Test component interactions
3. **Performance Tests**: Measure response times and costs

### Running Tests

```bash
# All tests
mvn test

# Unit tests only
mvn test -Dtest="*Test"

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# With coverage
mvn test jacoco:report
```

### Test Data

Use test builders for complex objects:
```java
public class LocatorRequestBuilder {
    public static LocatorRequest.Builder aLocatorRequest() {
        return LocatorRequest.builder()
            .description("Test element")
            .pageUrl("http://example.com")
            .elementType(ElementType.BUTTON);
    }
}
```

## Making Changes

### 1. Branch Strategy

- `main`: Stable release branch
- `develop`: Integration branch
- `feature/description`: Feature branches
- `hotfix/description`: Critical fixes

### 2. Development Workflow

```bash
# Create feature branch
git checkout -b feature/ai-provider-support

# Make changes and test
mvn test

# Commit with conventional commits
git commit -m "feat: add Azure OpenAI provider support"

# Push and create PR
git push origin feature/ai-provider-support
```

### 3. Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): description

feat(ai): add support for Azure OpenAI
fix(cache): resolve memory leak in selector cache
docs(readme): update installation instructions
test(integration): add tests for new AI provider
```

## Performance Considerations

### Profiling

Use JProfiler or async-profiler:
```bash
# Add to JVM args
-javaagent:async-profiler.jar=start,event=cpu,file=profile.html
```

### Benchmarking

We use JMH for performance tests:
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LocatorBenchmark {

    @Benchmark
    public WebElement findElementWithCache() {
        return locator.findElement("Submit button");
    }
}
```

### Memory Testing

```bash
# Heap dump analysis
jmap -dump:live,format=b,file=heap.hprof <pid>

# GC monitoring
java -XX:+PrintGCDetails -XX:+PrintGCTimeStamps YourTest
```

## Release Process

### 1. Version Management

We use Semantic Versioning:
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes

### 2. Release Checklist

- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version bumped in pom.xml
- [ ] Performance regression tests pass
- [ ] Security scan clean

### 3. Maven Release

```bash
# Prepare release
mvn release:prepare

# Perform release
mvn release:perform

# Deploy to Central
mvn deploy -P release
```

## Debugging Tips

### 1. Enable Debug Logging

```properties
logging.level.com.autoheal=DEBUG
logging.level.com.autoheal.ai=TRACE
```

### 2. Local AI Service Testing

```java
// Mock AI service for testing
@TestConfiguration
public class TestAIConfig {
    @Bean
    @Primary
    public AIService mockAIService() {
        return Mockito.mock(AIService.class);
    }
}
```

### 3. Browser Debugging

```java
// Keep browser open for inspection
ChromeOptions options = new ChromeOptions();
options.addArguments("--remote-debugging-port=9222");
WebDriver driver = new ChromeDriver(options);
```

## Getting Help

- **Slack**: #autoheal-dev
- **Email**: dev@autoheal.com
- **Issues**: GitHub Issues for bugs
- **Discussions**: GitHub Discussions for questions

## Code Review Guidelines

### What to Review

- [ ] Functionality correctness
- [ ] Performance impact
- [ ] Security considerations
- [ ] Test coverage
- [ ] Documentation quality
- [ ] Code style compliance

### Review Process

1. Automated checks must pass
2. At least one approving review required
3. Performance tests for significant changes
4. Security review for API changes

Thank you for contributing to AutoHeal Locator! 🎉