package com.example.meetings.e2e.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ProposeMeetingPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private static String csrfToken = null;

    public ProposeMeetingPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void open() {
        driver.get("http://localhost:8080/meetings/new");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));

        if (csrfToken == null) {
            fetchCsrfTokenFromLoginPage();
        }
    }

    private void fetchCsrfTokenFromLoginPage() {
        System.out.println("Fetching CSRF token from login page...");

        String currentUrl = driver.getCurrentUrl();

        driver.get("http://localhost:8080/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

        try {
            WebElement csrfInput = driver.findElement(By.name("_csrf"));
            csrfToken = csrfInput.getAttribute("value");
            System.out.println("CSRF token fetched: " + csrfToken);
        } catch (Exception e) {
            System.out.println("Could not fetch CSRF token: " + e.getMessage());
            csrfToken = "";
        }

        driver.get(currentUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("title")));
    }

    public void createMeetingViaHttp(String title, String start, String end, String invitees) {
        System.out.println("\n=== CREATE MEETING VIA HTTP ===");
        System.out.println("Title: " + title);
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
        System.out.println("Invitees: " + invitees);

        // Preparar os dados do formulário
        String formData = "title=" + encodeURIComponent(title) +
                "&description=" +
                "&start=" + encodeURIComponent(start) +
                "&end=" + encodeURIComponent(end) +
                "&invitees=" + encodeURIComponent(invitees);

        // Fazer a requisição POST via JavaScript
        String script =
                "var callback = arguments[arguments.length - 1];" +
                        "var xhr = new XMLHttpRequest();" +
                        "var url = 'http://localhost:8080/meetings/new';" +
                        "xhr.open('POST', url, true);" +
                        "xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');" +
                        "xhr.setRequestHeader('X-CSRF-TOKEN', '" + csrfToken + "');" +
                        "xhr.withCredentials = true;" +
                        "xhr.onreadystatechange = function() {" +
                        "  if (xhr.readyState === 4) {" +
                        "    callback({status: xhr.status, url: xhr.responseURL});" +
                        "  }" +
                        "};" +
                        "xhr.send('" + formData + "');";

        System.out.println("Executing HTTP request via JavaScript...");

        // Executar o script e esperar pelo resultado
        Object result = ((JavascriptExecutor) driver).executeAsyncScript(script);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println("Request completed. Current URL: " + driver.getCurrentUrl());
    }

    private String encodeURIComponent(String value) {
        if (value == null) return "";
        return ((JavascriptExecutor) driver).executeScript(
                "return encodeURIComponent(arguments[0]);", value).toString();
    }
}