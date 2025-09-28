# Installation Guide

## System Requirements

- **Java**: 11 or higher (AutoHeal Locator is a Java library)
- **Test Framework**: Selenium WebDriver 4.15.0+ or Playwright 1.40.0+
- **Build Tool**: Maven or Gradle for dependency management
- **AI Provider**: OpenAI, Gemini, or Local LLM

## Maven Installation

### Option 1: Maven Central (Coming Soon)

Add the following dependency to your `pom.xml`:

```xml
<dependencies>
    <!-- AutoHeal Core -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>autoheal-locator</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Choose ONE automation framework: -->

    <!-- Option A: Selenium WebDriver -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>

    <!-- Option B: Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.40.0</version>
    </dependency>

    <!-- SLF4J for logging (optional) -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

### Option 2: Build from Source (Recommended for Latest Features)

Clone and build the project locally to use the latest snapshot version:

```bash
# Clone the repository
git clone https://github.com/your-org/autoheal-locator.git
cd autoheal-locator

# Build and install to local Maven repository
mvn clean install

# Or build without running tests (faster)
mvn clean install -DskipTests
```

Then add the snapshot dependency to your `pom.xml`:

```xml
<dependencies>
    <!-- AutoHeal Core - Latest Snapshot -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>autoheal-locator</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

    <!-- Choose ONE automation framework: -->

    <!-- Option A: Selenium WebDriver -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>

    <!-- Option B: Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.40.0</version>
    </dependency>

    <!-- SLF4J for logging (optional) -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

**Benefits of building from source:**
- ✅ Latest features and bug fixes
- ✅ Ability to customize and contribute
- ✅ Access to unreleased improvements
- ✅ Full source code access for debugging

## Gradle Installation

### Option 1: Gradle Central (Coming Soon)

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'org.example:autoheal-locator:1.0.0'

    // Choose ONE automation framework:

    // Option A: Selenium WebDriver
    implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'

    // Option B: Playwright
    implementation 'com.microsoft.playwright:playwright:1.40.0'

    implementation 'org.slf4j:slf4j-simple:2.0.9' // optional
}
```

### Option 2: Build from Source

After building the project with `mvn clean install`, add to your `build.gradle`:

```gradle
dependencies {
    // AutoHeal Core - Latest Snapshot from local build
    implementation 'org.example:autoheal-locator:1.0-SNAPSHOT'

    // Choose ONE automation framework:

    // Option A: Selenium WebDriver
    implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'

    // Option B: Playwright
    implementation 'com.microsoft.playwright:playwright:1.40.0'

    implementation 'org.slf4j:slf4j-simple:2.0.9' // optional
}

repositories {
    mavenCentral()
    mavenLocal() // Required for locally built snapshots
}
```

## Manual Installation

### Option 1: Download Release JAR

1. Download the JAR file from [releases](https://github.com/your-org/autoheal-locator/releases)
2. Add to your classpath
3. Ensure Selenium WebDriver is also in classpath

### Option 2: Build JAR from Source

```bash
# Clone and build the project
git clone https://github.com/your-org/autoheal-locator.git
cd autoheal-locator

# Create JAR file
mvn clean package

# JAR file will be created in target/ directory
# autoheal-locator-1.0-SNAPSHOT.jar
```

Add the generated JAR to your classpath along with required dependencies.

## Verification

Create a simple test to verify installation:

### Selenium Verification
```java
import com.autoheal.AutoHealLocator;
import com.autoheal.impl.adapter.SeleniumWebAutomationAdapter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class SeleniumInstallationTest {
    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();

        try {
            AutoHealLocator autoHeal = AutoHealLocator.builder()
                .withWebAdapter(new SeleniumWebAutomationAdapter(driver))
                .build();

            System.out.println("✅ AutoHeal with Selenium installation successful!");

        } finally {
            driver.quit();
        }
    }
}
```

### Playwright Verification
```java
import com.autoheal.AutoHealLocator;
import com.autoheal.impl.adapter.PlaywrightWebAutomationAdapter;
import com.microsoft.playwright.*;

public class PlaywrightInstallationTest {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            PlaywrightWebAutomationAdapter adapter = new PlaywrightWebAutomationAdapter(page);
            AutoHealLocator autoHeal = new AutoHealLocator(adapter);

            System.out.println("✅ AutoHeal with Playwright installation successful!");

            browser.close();
        }
    }
}
```

## Next Steps

1. **Choose Your Framework**:
   - [Selenium Integration Guide](./selenium-integration.md) - Traditional WebDriver approach
   - [Playwright Integration Guide](./playwright-integration.md) - Modern automation framework
2. [Configure AI Provider](./ai-configuration.md) - Enable intelligent healing
3. [Quick Start Guide](./quick-start.md) - Basic usage examples
4. **Integration Options**:
   - [**Spring Boot Integration**](./spring-boot-integration.md) - Auto-configuration for Spring Boot projects
   - [**Page Object Pattern**](./examples/page-object-examples.md) - Best practices for organizing tests
5. **Examples by Framework**:
   - [Selenium Examples](./examples/selenium-examples.md)
   - [Playwright Examples](./examples/playwright-examples.md)