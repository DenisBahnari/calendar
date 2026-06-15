package com.example.meetings.e2e;

import com.example.meetings.e2e.pages.LoginPage;
import com.example.meetings.e2e.pages.RegisterPage;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationWorkflowE2ETest extends BaseE2ETest {

    @Test
    void userCanRegisterAndLogin() {
        String username = "user" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        assertTrue(driver.getCurrentUrl().contains("/calendar"));
    }

    @Test
    void userCannotRegisterWithDuplicateUsername() {
        String username = "duplicate" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);

        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        register.open();
        register.registerAndExpectFailure(username, "different@mail.pt", "123456");

        assertTrue(register.isOnRegisterPage());
        assertTrue(register.hasError("taken") || register.hasError("exist"));
    }

    @Test
    void userCannotLoginWithWrongPassword() {
        String username = "wrongpass" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "correct123");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectFailure(username, "wrongpassword");

        assertTrue(login.isOnLoginPage());
        assertTrue(login.hasLoginError());
    }

    @Test
    void userCannotLoginWithNonExistentUsername() {
        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectFailure("nonexistent_" + System.currentTimeMillis(), "anypassword");

        assertTrue(login.isOnLoginPage());
        assertTrue(login.hasLoginError());
    }

    @Test
    void userCannotRegisterWithEmptyFields() {
        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.register("", "", "");

        assertTrue(register.isOnRegisterPage());
    }

    @Test
    void userCannotRegisterWithInvalidEmail() {
        String username = "invalidemail" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();

        register.register(username, "invalid-email", "123456");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(register.isOnRegisterPage() || driver.getPageSource().contains("email"));
    }

    @Test
    void userCanLogout() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        String username = "logout" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        assertTrue(driver.getCurrentUrl().contains("/calendar"));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Find and click logout button
        driver.findElement(By.xpath("//button[contains(text(),'Sign out')] | //button[contains(text(),'Logout')]")).click();

        // Should redirect to login page
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));

        // Try to access protected page - should redirect to login
        driver.get("http://localhost:8080/calendar");
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void userCannotAccessCalendarWithoutLogin() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Try to access calendar directly without logging in
        driver.get("http://localhost:8080/calendar");

        // Should redirect to login page
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void userCannotAccessProposeMeetingWithoutLogin() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Try to access propose meeting page directly without logging in
        driver.get("http://localhost:8080/meetings/new");

        // Should redirect to login page
        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void sessionPersistsAfterPageRefresh() {
        String username = "session" + System.currentTimeMillis();

        RegisterPage register = new RegisterPage(driver);
        register.open();
        register.registerAndExpectSuccess(username, username + "@mail.pt", "123456");

        LoginPage login = new LoginPage(driver);
        login.open();
        login.loginAndExpectSuccess(username, "123456");

        assertTrue(driver.getCurrentUrl().contains("/calendar"));

        // Refresh the page
        driver.navigate().refresh();

        // Should still be on calendar page (still logged in)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue(driver.getCurrentUrl().contains("/calendar"));
    }
}