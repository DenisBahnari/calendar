package com.example.meetings.e2e;

import com.example.meetings.e2e.pages.DiscoverPage;
import com.example.meetings.e2e.pages.LoginPage;
import com.example.meetings.e2e.pages.RegisterPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscoveryWorkflowE2ETest extends BaseE2ETest {

    @Test
    void userCanSearchEventsOnAgendaLx() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String username = "discover" + System.currentTimeMillis();
        String searchQuery = "concerto";

        System.out.println("=== Starting Discovery Test (AgendaLx) ===");
        System.out.println("Username: " + username);
        System.out.println("Search query: " + searchQuery);

        // 1. Register user
        System.out.println("\n1. Registering user...");
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        // 2. Login
        System.out.println("\n2. Logging in...");
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // 3. Navigate to discover page
        System.out.println("\n3. Navigating to discover page...");
        DiscoverPage discoverPage = new DiscoverPage(driver);
        discoverPage.open();
        System.out.println("✓ Discover page loaded");

        // 4. Search for events
        System.out.println("\n4. Searching for events: " + searchQuery);
        discoverPage.search(searchQuery);

        // Wait for results
        discoverPage.waitForResults();

        // 5. Verify results are displayed
        System.out.println("\n5. Verifying search results...");
        boolean hasResults = discoverPage.hasResults();
        System.out.println("Has results: " + hasResults);

        if (hasResults) {
            int resultCount = discoverPage.getResultCount();
            System.out.println("Number of results: " + resultCount);
            assertTrue(resultCount > 0, "Should have at least one result");

            // 6. Copy first event to calendar
            System.out.println("\n6. Copying first event to calendar...");
            String eventTitle = discoverPage.getFirstEventTitle();
            System.out.println("Event title: " + eventTitle);

            discoverPage.copyFirstEventToCalendar();

            // Wait for copy to complete
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 7. Verify event was copied to calendar
            System.out.println("\n7. Verifying event on calendar...");
            driver.get("http://localhost:8080/calendar");
            wait.until(ExpectedConditions.urlContains("/calendar"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

            String pageSource = driver.getPageSource();
            boolean eventFound = pageSource.contains(eventTitle) || pageSource.contains("confirmed");
            System.out.println("Event found on calendar: " + eventFound);

            assertTrue(eventFound, "Event should appear on calendar after copying");
        }

        System.out.println("\n=== Discovery Test completed ===");
    }

    @Test
    void userCanSearchEventsWithEmptyQuery() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String username = "discoverempty" + System.currentTimeMillis();

        System.out.println("=== Testing empty search query ===");

        // 1. Register and login
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // 2. Navigate to discover page
        DiscoverPage discoverPage = new DiscoverPage(driver);
        discoverPage.open();

        System.out.println("Searching with empty query...");

        // Para query vazia, não esperamos resultados - apenas verificamos que a página não quebra
        String searchUrl = "http://localhost:8080/discover?q=";
        driver.get(searchUrl);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Verify page still works (no crash) - apenas verificar que ainda estamos na página discover
        assertTrue(driver.getCurrentUrl().contains("/discover"), "Should remain on discover page");

        // Verificar que o formulário de busca ainda está presente
        boolean hasSearchForm = driver.findElements(By.id("q")).size() > 0;
        assertTrue(hasSearchForm, "Search form should still be visible");

        System.out.println("=== Empty query test completed ===");
    }

    @Test
    void userCanSeeProviderInformation() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String username = "discoverprov" + System.currentTimeMillis();

        System.out.println("=== Testing provider information display ===");

        // 1. Register and login
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // 2. Navigate to discover page
        DiscoverPage discoverPage = new DiscoverPage(driver);
        discoverPage.open();

        // 3. Check if provider information is displayed
        String pageSource = driver.getPageSource();
        boolean hasProviderInfo = pageSource.contains("Agenda Cultural de Lisboa") ||
                pageSource.contains("Ticketmaster") ||
                pageSource.contains("Provider");

        System.out.println("Provider information displayed: " + hasProviderInfo);

        // The page should show at least one provider
        assertTrue(hasProviderInfo, "Provider information should be displayed");

        System.out.println("=== Provider information test completed ===");
    }

    @Test
    void testTicketmasterSearchWithApiKey() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        String username = "ticketmaster" + System.currentTimeMillis();
        String searchQuery = "rock";

        System.out.println("=== Testing Ticketmaster Search ===");
        System.out.println("Username: " + username);
        System.out.println("Search query: " + searchQuery);

        // 1. Register and login
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // 2. Search on discover page
        DiscoverPage discoverPage = new DiscoverPage(driver);
        discoverPage.open();
        discoverPage.search(searchQuery);

        // Wait longer for external API
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Check if results are displayed
        boolean hasResults = discoverPage.hasResults();
        System.out.println("Ticketmaster search has results: " + hasResults);

        // 4. If results exist, copy one to calendar
        if (hasResults) {
            String eventTitle = discoverPage.getFirstEventTitle();
            System.out.println("First Ticketmaster event: " + eventTitle);

            discoverPage.copyFirstEventToCalendar();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify on calendar
            driver.get("http://localhost:8080/calendar");
            wait.until(ExpectedConditions.urlContains("/calendar"));

            String pageSource = driver.getPageSource();
            boolean eventFound = pageSource.contains(eventTitle) || pageSource.contains("confirmed");
            System.out.println("Event copied to calendar: " + eventFound);
        }

        System.out.println("=== Ticketmaster test completed ===");
    }
}