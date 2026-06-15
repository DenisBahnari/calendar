package com.example.meetings.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "ci"})
@TestPropertySource(properties = {
        "spring.security.csrf.enabled=false",
        "security.enable-csrf=false"
})
public abstract class BaseE2ETest {

    protected WebDriver driver;

    @BeforeEach
    void setup() {
        // Verificar se a aplicação está disponível
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection)
                    new java.net.URL("http://localhost:8080/login").openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Warning: Application returned " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException("Application is not running on http://localhost:8080. Please start it with 'mvn spring-boot:run' before running tests.", e);
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        // Detectar se está em ambiente CI
        boolean isCI = System.getenv("CI") != null || System.getProperty("CI") != null;
        if (isCI) {
            options.addArguments("--headless=new");
            System.out.println("Running in CI mode - using headless Chrome");
        }

        // Desabilitar CDP logging
        System.setProperty("webdriver.chrome.silentOutput", "true");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.SEVERE);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void cleanup() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error quitting driver: " + e.getMessage());
            }
        }
    }
}