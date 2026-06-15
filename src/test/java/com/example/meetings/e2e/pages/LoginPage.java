package com.example.meetings.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void open() {
        driver.get("http://localhost:8080/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        // Limpa qualquer parâmetro da URL
        if (driver.getCurrentUrl().contains("logout") || driver.getCurrentUrl().contains("error")) {
            driver.get("http://localhost:8080/login");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        }
    }

    public void login(String username, String password) {
        System.out.println("Attempting login with username: " + username);

        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameField.clear();
        usernameField.sendKeys(username);

        String enteredUsername = usernameField.getAttribute("value");
        System.out.println("Entered username: " + enteredUsername);

        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.clear();
        passwordField.sendKeys(password);

        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));
        submitButton.click();

        System.out.println("Login form submitted");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Current URL after login: " + driver.getCurrentUrl());
    }

    public void loginAndExpectSuccess(String username, String password) {
        login(username, password);

        // Aguarda redirect para calendar
        try {
            wait.until(ExpectedConditions.urlContains("/calendar"));
            System.out.println("Successfully logged in, redirected to calendar");
        } catch (Exception e) {
            // Se não foi para calendar, verificar erro
            String pageSource = driver.getPageSource();
            if (pageSource.contains("Invalid username or password") || pageSource.contains("Bad credentials")) {
                throw new RuntimeException("Login failed: Invalid username or password for user: " + username);
            }
            System.err.println("Failed to redirect to calendar. Current URL: " + driver.getCurrentUrl());
            throw e;
        }
    }

    public void loginAndExpectFailure(String username, String password) {
        login(username, password);
        // Deve permanecer na página de login
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assert driver.getCurrentUrl().contains("/login");
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("/login");
    }

    public boolean hasLoginError() {
        try {
            return wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(text(),'Invalid username or password')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(text(),'Bad credentials')]")),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(text(),'error')]"))
            )) != null;
        } catch (Exception e) {
            return driver.getPageSource().contains("Invalid username or password") ||
                    driver.getPageSource().contains("Bad credentials");
        }
    }
}