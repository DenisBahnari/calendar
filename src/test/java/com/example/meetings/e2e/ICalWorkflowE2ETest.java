package com.example.meetings.e2e;

import com.example.meetings.e2e.pages.CalendarPage;
import com.example.meetings.e2e.pages.LoginPage;
import com.example.meetings.e2e.pages.ProposeMeetingPage;
import com.example.meetings.e2e.pages.RegisterPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class ICalWorkflowE2ETest extends BaseE2ETest {

    private String getFutureDateTime(int daysFromNow, int hours, int minutes) {
        LocalDateTime future = LocalDateTime.now()
                .plusDays(daysFromNow)
                .withHour(hours)
                .withMinute(minutes)
                .withSecond(0)
                .withNano(0);

        if (future.isBefore(LocalDateTime.now())) {
            future = future.plusDays(1);
        }

        return future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    @Test
    void userCanExportCalendarAsIcal() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        String username = "ical" + System.currentTimeMillis();
        String meetingTitle = "ICAL TEST " + System.currentTimeMillis();

        System.out.println("=== Starting iCal Export Test ===");
        System.out.println("Username: " + username);
        System.out.println("Meeting Title: " + meetingTitle);

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

        // 3. Create meeting via HTTP
        System.out.println("\n3. Creating meeting via HTTP...");
        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(30, 15, 0);
        String endTime = getFutureDateTime(30, 16, 0);

        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);

        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, "");

        System.out.println("Waiting for meeting creation...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 4. Verificar se a meeting foi criada
        System.out.println("\n4. Verifying meeting on calendar...");
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String pageSource = driver.getPageSource();
        boolean meetingFound = pageSource.contains(meetingTitle);
        System.out.println("Meeting found on calendar: " + meetingFound);
        assertTrue(meetingFound, "Meeting was not created successfully");

        // 5. Obter URL do iCal
        System.out.println("\n5. Getting iCal URL...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        CalendarPage calendar = new CalendarPage(driver);
        String icalUrl = calendar.getIcalUrl();
        System.out.println("iCal URL found: " + icalUrl);

        // Converter webcal:// para http://
        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
            System.out.println("Converted webcal to http: " + icalUrl);
        }

        if (!icalUrl.startsWith("http")) {
            icalUrl = "http://localhost:8080" + (icalUrl.startsWith("/") ? icalUrl : "/" + icalUrl);
        }
        System.out.println("Full iCal URL: " + icalUrl);

        // 6. Obter conteúdo do iCal via JavaScript (fetch API)
        System.out.println("\n6. Fetching iCal content via HTTP request...");
        String script = "var callback = arguments[arguments.length - 1];" +
                "fetch('" + icalUrl + "', {" +
                "  method: 'GET'," +
                "  credentials: 'include'" +
                "}).then(response => response.text())" +
                ".then(data => callback(data))" +
                ".catch(error => callback('ERROR: ' + error));";

        String icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);
        System.out.println("iCal content fetched, length: " + (icalContent != null ? icalContent.length() : 0));

        if (icalContent == null || icalContent.isEmpty()) {
            System.out.println("Failed to fetch iCal content");
            assertTrue(false, "Could not fetch iCal content");
        }

        // 7. Verificar conteúdo do iCal
        System.out.println("\n7. Verifying iCal content...");

        boolean hasVCalendar = icalContent.contains("BEGIN:VCALENDAR");
        boolean hasMeetingTitle = icalContent.contains(meetingTitle);
        boolean hasVersion = icalContent.contains("VERSION:2.0");
        boolean hasEndCalendar = icalContent.contains("END:VCALENDAR");

        System.out.println("BEGIN:VCALENDAR found: " + hasVCalendar);
        System.out.println("Meeting title found: " + hasMeetingTitle);
        System.out.println("VERSION:2.0 found: " + hasVersion);
        System.out.println("END:VCALENDAR found: " + hasEndCalendar);

        if (!hasVCalendar && icalContent != null) {
            System.out.println("iCal content preview (first 500 chars):");
            System.out.println(icalContent.substring(0, Math.min(500, icalContent.length())));
        }

        assertTrue(hasVCalendar, "iCal does not contain BEGIN:VCALENDAR");
        assertTrue(hasMeetingTitle, "iCal does not contain meeting title");
        assertTrue(hasVersion, "iCal does not contain VERSION:2.0");
        assertTrue(hasEndCalendar, "iCal does not contain END:VCALENDAR");

        System.out.println("\n=== Test completed successfully ===");
    }

    @Test
    void iCalContainsMultipleMeetings() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        String username = "ical_multi" + System.currentTimeMillis();
        String meetingTitle1 = "ICAL Meeting 1 " + System.currentTimeMillis();
        String meetingTitle2 = "ICAL Meeting 2 " + System.currentTimeMillis();

        System.out.println("=== Testing iCal with multiple meetings ===");

        // Register and login
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // Create first meeting
        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();
        String startTime1 = getFutureDateTime(30, 15, 0);
        String endTime1 = getFutureDateTime(30, 16, 0);
        meetingPage.createMeetingViaHttp(meetingTitle1, startTime1, endTime1, "");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Create second meeting
        meetingPage.open();
        String startTime2 = getFutureDateTime(31, 10, 0);
        String endTime2 = getFutureDateTime(31, 11, 0);
        meetingPage.createMeetingViaHttp(meetingTitle2, startTime2, endTime2, "");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Get iCal content
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        CalendarPage calendar = new CalendarPage(driver);
        String icalUrl = calendar.getIcalUrl();

        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        String script = "var callback = arguments[arguments.length - 1];" +
                "fetch('" + icalUrl + "', {method: 'GET', credentials: 'include'})" +
                ".then(response => response.text())" +
                ".then(data => callback(data))" +
                ".catch(error => callback('ERROR: ' + error));";

        String icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);

        // Verify both meetings are in iCal
        assertTrue(icalContent.contains(meetingTitle1), "First meeting should be in iCal");
        assertTrue(icalContent.contains(meetingTitle2), "Second meeting should be in iCal");

        // Count VEVENT entries
        int veventCount = icalContent.split("BEGIN:VEVENT").length - 1;
        assertEquals(2, veventCount, "Should have exactly 2 VEVENT entries");

        System.out.println("=== Multiple meetings iCal test completed ===");
    }


    @Test
    void iCalShowsCorrectMeetingStatus() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String alice = "ical_alice" + System.currentTimeMillis();
        String bob = "ical_bob" + System.currentTimeMillis();
        String meetingTitle = "Status Meeting " + System.currentTimeMillis();

        // Register users
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(alice, alice + "@mail.pt", "123456");
        register.open();
        register.registerAndExpectSuccess(bob, bob + "@mail.pt", "123456");

        // Alice creates meeting with Bob (should be TENTATIVE until Bob accepts)
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();
        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);
        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, bob);
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Get iCal for Alice - meeting should be TENTATIVE
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        CalendarPage calendar = new CalendarPage(driver);
        String icalUrl = calendar.getIcalUrl();

        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        String script = "var callback = arguments[arguments.length - 1];" +
                "fetch('" + icalUrl + "', {method: 'GET', credentials: 'include'})" +
                ".then(response => response.text())" +
                ".then(data => callback(data))" +
                ".catch(error => callback('ERROR: ' + error));";

        String icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);

        assertTrue(icalContent.contains("STATUS:TENTATIVE"),
                "Meeting should be TENTATIVE before acceptance");

        // Bob accepts the meeting
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(bob, "123456");

        calendar = new CalendarPage(driver);
        calendar.acceptFirstInvite();
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Get iCal for Alice again - meeting should be CONFIRMED
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        calendar = new CalendarPage(driver);
        icalUrl = calendar.getIcalUrl();

        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);

        assertTrue(icalContent.contains("STATUS:CONFIRMED"),
                "Meeting should be CONFIRMED after acceptance");

        System.out.println("=== iCal status test completed ===");
    }


    @Test
    void invalidIcalTokenReturnsError() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        String invalidUrl = "http://localhost:8080/ical/invalid-token-12345.ics";

        driver.get(invalidUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        String pageSource = driver.getPageSource();

        // Should show error or be empty (not a valid iCal)
        boolean hasError = pageSource.contains("error") ||
                pageSource.contains("Invalid") ||
                !pageSource.contains("BEGIN:VCALENDAR");

        assertTrue(hasError, "Invalid token should return error or not produce valid iCal");

        System.out.println("=== Invalid iCal token test completed ===");
    }

    @Test
    void iCalIncludesDescription() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        String username = "ical_desc" + System.currentTimeMillis();
        String meetingTitle = "Descriptive Meeting " + System.currentTimeMillis();
        String meetingDescription = "This is a detailed description of the meeting with important information.";

        System.out.println("=== Testing iCal with description ===");

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        // Create meeting with description
        String startTime = getFutureDateTime(30, 15, 0);
        String endTime = getFutureDateTime(30, 16, 0);

        String script = "var callback = arguments[arguments.length - 1];" +
                "var formData = new URLSearchParams();" +
                "formData.append('title', arguments[0]);" +
                "formData.append('description', arguments[1]);" +
                "formData.append('start', arguments[2]);" +
                "formData.append('end', arguments[3]);" +
                "formData.append('invitees', '');" +
                "fetch('http://localhost:8080/meetings/new', {" +
                "  method: 'POST'," +
                "  headers: {'Content-Type': 'application/x-www-form-urlencoded'}," +
                "  credentials: 'include'," +
                "  body: formData.toString()" +
                "}).then(() => callback('done'));";

        ((JavascriptExecutor) driver).executeAsyncScript(script, meetingTitle, meetingDescription, startTime, endTime);
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Get iCal content
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        CalendarPage calendar = new CalendarPage(driver);
        String icalUrl = calendar.getIcalUrl();

        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        String fetchScript = "var callback = arguments[arguments.length - 1];" +
                "fetch('" + icalUrl + "', {method: 'GET', credentials: 'include'})" +
                ".then(response => response.text())" +
                ".then(data => callback(data))" +
                ".catch(error => callback('ERROR: ' + error));";

        String icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(fetchScript);

        // Verify description is in iCal
        assertTrue(icalContent.contains(meetingDescription),
                "Description should be in iCal");

        System.out.println("=== iCal description test completed ===");
    }

    @Test
    void declinedMeetingsDoNotAppearInIcalForDecliner() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String alice = "ical_decline_a" + System.currentTimeMillis();
        String bob = "ical_decline_b" + System.currentTimeMillis();
        String meetingTitle = "Declined Meeting " + System.currentTimeMillis();

        // Register users
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(alice, alice + "@mail.pt", "123456");
        register.open();
        register.registerAndExpectSuccess(bob, bob + "@mail.pt", "123456");

        // Alice creates meeting with Bob
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();
        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);
        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, bob);
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Verify meeting was created
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains(meetingTitle),
                "Meeting should be created by Alice");

        // Bob declines the meeting
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(bob, "123456");

        // Wait for page to load and find decline button
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        CalendarPage bobCalendar = new CalendarPage(driver);

        // Check if Bob sees the meeting before declining
        assertTrue(driver.getPageSource().contains(meetingTitle),
                "Bob should see the meeting invitation");

        bobCalendar.declineFirstInvite();
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Get iCal for Bob - declined meeting should NOT appear
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        String icalUrl = bobCalendar.getIcalUrl();
        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        String script = "var callback = arguments[arguments.length - 1];" +
                "fetch('" + icalUrl + "', {method: 'GET', credentials: 'include'})" +
                ".then(response => response.text())" +
                ".then(data => callback(data))" +
                ".catch(error => callback('ERROR: ' + error));";

        String icalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);

        assertFalse(icalContent.contains(meetingTitle),
                "Declined meeting should NOT appear in iCal for Bob (the decliner)");

        System.out.println("✓ Declined meeting does not appear for decliner");

        // Check if Alice still sees it in her calendar (organizer)
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));

        CalendarPage aliceCalendar = new CalendarPage(driver);
        icalUrl = aliceCalendar.getIcalUrl();
        if (icalUrl.startsWith("webcal://")) {
            icalUrl = icalUrl.replace("webcal://", "http://");
        }

        String aliceIcalContent = (String) ((JavascriptExecutor) driver).executeAsyncScript(script);

        // Note: This may or may not be expected behavior
        System.out.println("Meeting appears in organizer's iCal: " +
                aliceIcalContent.contains(meetingTitle));

        System.out.println("=== Declined meeting iCal test completed ===");
    }
}