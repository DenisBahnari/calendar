package com.example.meetings.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class RegisterPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void open() {
        driver.get("http://localhost:8080/register");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
    }

    public void register(String username, String email, String password) {
        System.out.println("Registering user: " + username);

        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameField.clear();
        usernameField.sendKeys(username);

        WebElement emailField = driver.findElement(By.id("email"));
        emailField.clear();
        emailField.sendKeys(email);

        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.clear();
        passwordField.sendKeys(password);

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Aguarda um pouco para a página processar o submit
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Registration submitted. Current URL: " + driver.getCurrentUrl());
    }

    public void registerAndExpectSuccess(String username, String email, String password) {
        register(username, email, password);

        // Após registo bem sucedido, deve redirecionar para login
        try {
            wait.until(ExpectedConditions.urlContains("/login"));
            System.out.println("Registration successful, redirected to login");
        } catch (Exception e) {
            System.err.println("Registration failed. Current URL: " + driver.getCurrentUrl());
            String pageSource = driver.getPageSource();
            if (pageSource.contains("taken") || pageSource.contains("exist")) {
                throw new RuntimeException("Registration failed: Username already exists");
            }
            throw e;
        }

        // Aguardar mais um pouco para garantir que o utilizador foi criado
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void registerAndExpectFailure(String username, String email, String password) {
        register(username, email, password);

        // Após registo falhado, deve permanecer na página de registo
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean hasError(String errorText) {
        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class,'error')] | //span[contains(@class,'error')] | //div[contains(text(),'" + errorText + "')]")
            ));
            return errorElement != null;
        } catch (Exception e) {
            return driver.getPageSource().contains(errorText);
        }
    }

    public boolean isOnRegisterPage() {
        return driver.getCurrentUrl().contains("/register");
    }
}