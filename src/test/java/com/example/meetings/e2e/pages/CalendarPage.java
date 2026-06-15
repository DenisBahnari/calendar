package com.example.meetings.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CalendarPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public CalendarPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public boolean containsMeeting(String title) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        return driver.getPageSource().contains(title);
    }

    public boolean containsConfirmedBadge() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        return driver.getPageSource().toLowerCase().contains("confirmed");
    }

    public boolean containsTentativeBadge() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
        return driver.getPageSource().toLowerCase().contains("tentative");
    }

    public void acceptFirstInvite() {
        try {
            // Primeiro espera que a página carregue completamente
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));

            // Procura por qualquer botão ou link que contenha Accept (case insensitive)
            List<WebElement> acceptElements = driver.findElements(
                    By.xpath("//button[contains(translate(text(), 'ACCEPT', 'accept'), 'accept')] | " +
                            "//a[contains(translate(text(), 'ACCEPT', 'accept'), 'accept')] | " +
                            "//button[contains(@class, 'accept')] | " +
                            "//input[@value='Accept']")
            );

            if (acceptElements.isEmpty()) {
                // Se não encontrar, procura por qualquer elemento que pareça um botão de accept
                acceptElements = driver.findElements(By.xpath("//*[contains(translate(text(), 'ACCEPT', 'accept'), 'accept') and (self::button or self::a)]"));
            }

            if (acceptElements.isEmpty()) {
                throw new RuntimeException("No accept button found on the page");
            }

            WebElement acceptButton = acceptElements.get(0);

            // Scroll até o botão
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", acceptButton);
            Thread.sleep(500);

            // Clica no botão
            acceptButton.click();
            System.out.println("Clicked accept button");

            // Aguarda o processamento
            Thread.sleep(3000);

        } catch (Exception e) {
            System.err.println("Accept button not found or not clickable: " + e.getMessage());
            throw new RuntimeException("Could not accept invite", e);
        }
    }

    public void declineFirstInvite() {
        try {
            WebElement declineButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(translate(text(), 'DECLINE', 'decline'), 'decline')] | " +
                            "//a[contains(translate(text(), 'DECLINE', 'decline'), 'decline')]")
            ));
            declineButton.click();
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("Decline button not found or not clickable: " + e.getMessage());
            throw new RuntimeException("Could not decline invite", e);
        }
    }

    public String getIcalUrl() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("url")));
        List<WebElement> urlElements = driver.findElements(By.className("url"));

        for (WebElement element : urlElements) {
            String text = element.getText();
            if (text.contains("ical") && text.contains("ics")) {
                return text.trim();
            }
        }

        if (!urlElements.isEmpty()) {
            String lastUrl = urlElements.get(urlElements.size() - 1).getText();
            return lastUrl.trim();
        }

        throw new RuntimeException("iCal URL not found on the page");
    }

    public void signOut() {
        driver.get("http://localhost:8080/logout");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}