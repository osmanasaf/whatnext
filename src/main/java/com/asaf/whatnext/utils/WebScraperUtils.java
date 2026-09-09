package com.asaf.whatnext.utils;

import com.asaf.whatnext.scraper.web.ScraperBrowser;
import com.asaf.whatnext.scraper.web.ScraperNavigation;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Tüm scraper çağrıları için istikrarlı API; uygulama
 * {@link com.asaf.whatnext.scraper.web.ScraperBrowser} ve
 * {@link com.asaf.whatnext.scraper.web.ScraperNavigation} üzerinden yönlendirilir.
 */
public final class WebScraperUtils {

    private WebScraperUtils() {
    }

    public static ChromeOptions configureChromeBrowser(String chromePath) {
        return ScraperBrowser.configureChromeBrowser(chromePath);
    }

    public static WebDriver initializeBrowser(String chromePath, int timeoutSeconds) {
        return ScraperBrowser.initializeBrowser(chromePath, timeoutSeconds);
    }

    public static void closeBrowser(WebDriver driver) {
        ScraperBrowser.closeBrowser(driver);
    }

    public static boolean shouldRestartBrowser() {
        return ScraperBrowser.shouldRestartBrowser();
    }

    public static void resetPageCounter() {
        ScraperBrowser.resetPageCounter();
    }

    public static void navigateToUrl(WebDriver driver, WebDriverWait wait, String url) {
        ScraperNavigation.navigateToUrl(driver, wait, url);
    }

    public static void scrollToElement(WebDriver driver, WebElement element) {
        ScraperNavigation.scrollToElement(driver, element);
    }

    public static void clickElementWithJavaScript(WebDriver driver, WebElement element) {
        ScraperNavigation.clickElementWithJavaScript(driver, element);
    }

    public static void handleCookieConsent(WebDriver driver) {
        ScraperNavigation.handleCookieConsent(driver);
    }

    public static void handleOverlays(WebDriver driver) {
        ScraperNavigation.handleOverlays(driver);
    }

    public static String extractTextWithMultipleSelectors(
            WebDriver driver, String elementType, String... selectors) {
        return ScraperNavigation.extractTextWithMultipleSelectors(driver, elementType, selectors);
    }

    public static int scrollToLoadAll(
            WebDriver driver, int maxScrolls, int waitMs, String contentSelector) {
        return ScraperNavigation.scrollToLoadAll(driver, maxScrolls, waitMs, contentSelector);
    }

    public static int clickLoadMoreUntilEnd(
            WebDriver driver,
            WebDriverWait wait,
            String buttonSelector,
            String contentSelector,
            int maxAttempts,
            int waitMs) {
        return ScraperNavigation.clickLoadMoreUntilEnd(
                driver, wait, buttonSelector, contentSelector, maxAttempts, waitMs);
    }
}
