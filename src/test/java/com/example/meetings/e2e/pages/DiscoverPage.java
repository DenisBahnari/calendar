package com.example.meetings.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class DiscoverPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public DiscoverPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void open() {
        driver.get("http://localhost:8080/discover");

        if (driver.getCurrentUrl().contains("/login")) {
            throw new RuntimeException("Not logged in - session expired");
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    public void search(String query) {
        System.out.println("Searching for: " + query);

        // Navegar diretamente para a URL de busca (GET request)
        String searchUrl = "http://localhost:8080/discover?q=" + encodeURIComponent(query);
        driver.get(searchUrl);

        // Aguardar os resultados
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    private String encodeURIComponent(String value) {
        if (value == null) return "";
        return ((JavascriptExecutor) driver).executeScript(
                "return encodeURIComponent(arguments[0]);", value).toString();
    }

    public boolean hasResults() {
        try {
            // Os resultados têm classe "meeting" no HTML
            List<WebElement> meetings = driver.findElements(By.className("meeting"));
            return !meetings.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public int getResultCount() {
        try {
            List<WebElement> meetings = driver.findElements(By.className("meeting"));
            return meetings.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getFirstEventTitle() {
        try {
            // O título está dentro de um <strong> dentro do elemento meeting
            WebElement firstMeeting = driver.findElement(By.className("meeting"));
            WebElement titleElement = firstMeeting.findElement(By.tagName("strong"));
            return titleElement.getText();
        } catch (Exception e) {
            throw new RuntimeException("No events found to get title from: " + e.getMessage());
        }
    }

    public String getEventTitleAtIndex(int index) {
        try {
            List<WebElement> meetings = driver.findElements(By.className("meeting"));
            if (index < meetings.size()) {
                WebElement titleElement = meetings.get(index).findElement(By.tagName("strong"));
                return titleElement.getText();
            }
            throw new RuntimeException("Event at index " + index + " not found");
        } catch (Exception e) {
            throw new RuntimeException("No events found to get title from");
        }
    }

    public void copyFirstEventToCalendar() {
        try {
            // Procurar o primeiro formulário de copy dentro do primeiro meeting
            WebElement firstMeeting = driver.findElement(By.className("meeting"));
            WebElement copyButton = firstMeeting.findElement(By.xpath(".//button[contains(text(),'Copy')]"));
            copyButton.click();
            System.out.println("Clicked copy button for first event");

            // Aguardar o copy completar
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not copy first event: " + e.getMessage());
        }
    }

    public void copyEventToCalendarAtIndex(int index) {
        try {
            List<WebElement> meetings = driver.findElements(By.className("meeting"));
            if (index < meetings.size()) {
                WebElement copyButton = meetings.get(index).findElement(By.xpath(".//button[contains(text(),'Copy')]"));
                copyButton.click();
                System.out.println("Clicked copy button for event at index " + index);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new RuntimeException("Event at index " + index + " not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not copy event: " + e.getMessage());
        }
    }

    public boolean hasProvider(String providerName) {
        try {
            return driver.getPageSource().contains(providerName);
        } catch (Exception e) {
            return false;
        }
    }

    public void waitForResults() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Aguardar até que haja resultados ou mensagem de "No events found"
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.className("meeting")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//p[contains(text(),'No events found')]"))
        ));
    }

    public boolean hasNoResultsMessage() {
        try {
            WebElement noResultsMsg = driver.findElement(By.xpath("//p[contains(text(),'No events found')]"));
            return noResultsMsg.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}