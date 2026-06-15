package com.example.meetings.e2e;

import com.example.meetings.e2e.pages.LoginPage;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SessionHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public SessionHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public boolean isLoggedIn() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl.contains("/calendar") || currentUrl.contains("/meetings")) {
            return true;
        }

        Cookie sessionCookie = driver.manage().getCookieNamed("JSESSIONID");
        return sessionCookie != null && !currentUrl.contains("/login");
    }

    public void ensureLoggedIn(String username, String password) {
        if (!isLoggedIn() || driver.getCurrentUrl().contains("/login")) {
            System.out.println("Session lost, re-logging in as: " + username);
            LoginPage login = new LoginPage(driver);
            login.open();
            login.loginAndExpectSuccess(username, password);
        }
    }
}