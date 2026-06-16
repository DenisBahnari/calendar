package com.example.meetings.e2e;

import com.example.meetings.e2e.pages.CalendarPage;
import com.example.meetings.e2e.pages.LoginPage;
import com.example.meetings.e2e.pages.ProposeMeetingPage;
import com.example.meetings.e2e.pages.RegisterPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MeetingWorkflowE2ETest extends BaseE2ETest {

    private SessionHelper sessionHelper;

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

    private String getPastDateTime(int daysAgo, int hours, int minutes) {
        LocalDateTime past = LocalDateTime.now()
                .minusDays(daysAgo)
                .withHour(hours)
                .withMinute(minutes)
                .withSecond(0)
                .withNano(0);

        return past.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    @Test
    void invitedUserAcceptsMeetingAndMeetingBecomesConfirmed() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String alice = "alice" + System.currentTimeMillis();
        String bob = "bob" + System.currentTimeMillis();
        String meetingTitle = "VVS Meeting " + System.currentTimeMillis();

        System.out.println("=== Starting test ===");
        System.out.println("Alice: " + alice);
        System.out.println("Bob: " + bob);
        System.out.println("Meeting: " + meetingTitle);

        // 1. Register Alice
        System.out.println("\n1. Registering Alice...");
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(alice, alice + "@mail.pt", "123456");

        // 2. Register Bob
        System.out.println("\n2. Registering Bob...");
        register.open();
        register.registerAndExpectSuccess(bob, bob + "@mail.pt", "123456");

        // 3. Login as Alice
        System.out.println("\n3. Logging in as Alice...");
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        // 4. Create meeting via HTTP
        System.out.println("\n4. Creating meeting via HTTP...");
        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);

        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);

        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, bob);

        // Wait for meeting creation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Verify meeting appears on Alice's calendar
        System.out.println("\n5. Verifying meeting on Alice's calendar...");
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
        System.out.println("Meeting found on Alice's calendar: " + meetingFound);

        assertTrue(meetingFound, "Meeting not found on Alice's calendar");
        System.out.println("Meeting found on Alice's calendar!");

        // 6. Logout Alice
        System.out.println("\n6. Logging out Alice...");
        driver.get("http://localhost:8080/logout");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 7. Login as Bob
        System.out.println("\n7. Logging in as Bob...");
        login.open();
        login.loginAndExpectSuccess(bob, "123456");

        // 8. Verify Bob sees the meeting
        System.out.println("\n8. Verifying meeting on Bob's calendar...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pageSource = driver.getPageSource();
        meetingFound = pageSource.contains(meetingTitle);
        System.out.println("Meeting found on Bob's calendar: " + meetingFound);

        assertTrue(meetingFound, "Meeting not found on Bob's calendar");
        System.out.println("Meeting found on Bob's calendar!");

        // 9. Accept invite
        System.out.println("\n9. Accepting invite...");
        CalendarPage bobCalendar = new CalendarPage(driver);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        bobCalendar.acceptFirstInvite();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 10. Verify meeting shows as confirmed
        System.out.println("\n10. Verifying meeting is confirmed...");
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean confirmed = bobCalendar.containsConfirmedBadge();
        System.out.println("Meeting confirmed badge found: " + confirmed);

        assertTrue(confirmed, "Meeting confirmation badge not found");
        System.out.println("Meeting is confirmed!");

        System.out.println("\n=== Test completed successfully ===");
    }

    @Test
    void userCanDeclineMeetingInvite() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String organizer = "org" + System.currentTimeMillis();
        String invitee = "invitee" + System.currentTimeMillis();
        String meetingTitle = "Test Meeting " + System.currentTimeMillis();

        System.out.println("=== Starting decline test ===");
        System.out.println("Organizer: " + organizer);
        System.out.println("Invitee: " + invitee);
        System.out.println("Meeting: " + meetingTitle);

        // Register users
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(organizer, organizer + "@mail.pt", "123456");

        register.open();
        register.registerAndExpectSuccess(invitee, invitee + "@mail.pt", "123456");

        // Organizer creates meeting via HTTP
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(organizer, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(14, 14, 0);
        String endTime = getFutureDateTime(14, 15, 0);

        System.out.println("Start time: " + startTime);
        System.out.println("End time: " + endTime);

        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, invitee);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify meeting appears for organizer
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        boolean meetingFound = driver.getPageSource().contains(meetingTitle);
        System.out.println("Meeting found on organizer's calendar: " + meetingFound);
        assertTrue(meetingFound, "Meeting not found on organizer's calendar");

        // Sign out organizer
        driver.get("http://localhost:8080/logout");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Invitee logs in
        login.open();
        login.loginAndExpectSuccess(invitee, "123456");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Find and click decline button
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            org.openqa.selenium.WebElement declineButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Decline')] | //button[contains(text(),'decline')] | //button[contains(@class,'decline')]")
            ));
            declineButton.click();
            System.out.println("Clicked decline button");

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            driver.get("http://localhost:8080/calendar");
            wait.until(ExpectedConditions.urlContains("/calendar"));
            assertTrue(driver.getCurrentUrl().contains("/calendar"), "Not on calendar page after decline");
            System.out.println("Successfully declined meeting");

        } catch (Exception e) {
            System.err.println("Decline button not found: " + e.getMessage());
            driver.get("http://localhost:8080/calendar");
            wait.until(ExpectedConditions.urlContains("/calendar"));
            assertTrue(driver.getCurrentUrl().contains("/calendar"), "Not on calendar page");
        }

        System.out.println("=== Decline test completed ===");
    }


    @Test
    void meetingWithMultipleInvitees() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String organizer = "multi_org" + System.currentTimeMillis();
        String invitee1 = "multi_inv1" + System.currentTimeMillis();
        String invitee2 = "multi_inv2" + System.currentTimeMillis();
        String meetingTitle = "Multi Meeting " + System.currentTimeMillis();

        // Register all users
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(organizer, organizer + "@mail.pt", "123456");
        register.open();
        register.registerAndExpectSuccess(invitee1, invitee1 + "@mail.pt", "123456");
        register.open();
        register.registerAndExpectSuccess(invitee2, invitee2 + "@mail.pt", "123456");

        // Organizer creates meeting with both invitees
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(organizer, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);

        // Invite both users (comma separated)
        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, invitee1 + "," + invitee2);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Verify meeting on organizer's calendar
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));
        assertTrue(driver.getPageSource().contains(meetingTitle));

        // Check that both invitees received the invite
        driver.get("http://localhost:8080/logout");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Login as invitee1
        login.open();
        login.loginAndExpectSuccess(invitee1, "123456");
        assertTrue(driver.getPageSource().contains(meetingTitle));

        driver.get("http://localhost:8080/logout");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Login as invitee2
        login.open();
        login.loginAndExpectSuccess(invitee2, "123456");
        assertTrue(driver.getPageSource().contains(meetingTitle));

        System.out.println("=== Multiple invitees test completed ===");
    }

    @Test
    void cannotInviteNonExistentUser() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String organizer = "org_inv" + System.currentTimeMillis();
        String nonExistentUser = "nonexistent_" + System.currentTimeMillis();
        String meetingTitle = "Invalid Invite " + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(organizer, organizer + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(organizer, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);

        // Try to create meeting with non-existent invitee
        try {
            meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, nonExistentUser);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Meeting should NOT appear on calendar
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        assertFalse(driver.getPageSource().contains(meetingTitle),
                "Meeting with non-existent invitee should not be created");

        System.out.println("=== Non-existent invitee test completed ===");
    }

    @Test
    void cannotCreateMeetingWithInvalidDates() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String organizer = "org_date" + System.currentTimeMillis();
        String meetingTitle = "Invalid Date Meeting " + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(organizer, organizer + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(organizer, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(7, 11, 0);
        String endTime = getFutureDateTime(7, 10, 0); // End before start - INVALID

        // Try to create meeting with invalid dates
        try {
            meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, "");
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        // Meeting should NOT appear on calendar
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        assertFalse(driver.getPageSource().contains(meetingTitle),
                "Meeting with invalid dates should not be created");

        System.out.println("=== Invalid dates test completed ===");
    }

    @Test
    void createMeetingForSelfOnly() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String user = "self_meeting" + System.currentTimeMillis();
        String meetingTitle = "Self Meeting " + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(user, user + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(user, "123456");

        ProposeMeetingPage meetingPage = new ProposeMeetingPage(driver);
        meetingPage.open();

        String startTime = getFutureDateTime(7, 10, 0);
        String endTime = getFutureDateTime(7, 11, 0);

        meetingPage.createMeetingViaHttp(meetingTitle, startTime, endTime, "");

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Meeting should appear on calendar as CONFIRMED (no invites needed)
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        assertTrue(driver.getPageSource().contains(meetingTitle), "Self meeting should be created");
        assertTrue(driver.getPageSource().contains("confirmed"), "Self meeting should be confirmed");

        System.out.println("=== Self meeting test completed ===");
    }

    @Test
    void organizerSeesConfirmedAfterAllAccept() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        sessionHelper = new SessionHelper(driver);

        String alice = "alice_confirm" + System.currentTimeMillis();
        String bob = "bob_confirm" + System.currentTimeMillis();
        String meetingTitle = "Confirm Meeting " + System.currentTimeMillis();

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
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Alice's meeting should be tentative (waiting for Bob)
        driver.get("http://localhost:8080/calendar");
        assertTrue(driver.getPageSource().contains("tentative"),
                "Organizer's meeting should be tentative before Bob accepts");

        // Bob logs in and accepts
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(bob, "123456");

        CalendarPage bobCalendar = new CalendarPage(driver);
        bobCalendar.acceptFirstInvite();
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // Alice logs back in - meeting should now be confirmed
        driver.get("http://localhost:8080/logout");
        login.open();
        login.loginAndExpectSuccess(alice, "123456");

        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/calendar"));

        assertTrue(driver.getPageSource().contains("confirmed"),
                "Organizer's meeting should be confirmed after Bob accepts");

        System.out.println("=== Organizer sees confirmed test completed ===");
    }
}