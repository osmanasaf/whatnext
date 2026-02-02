package com.asaf.whatnext.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebScraperUtils {
    private static final Logger LOGGER = Logger.getLogger(WebScraperUtils.class.getName());
    
    private static final int MAX_PAGES_BEFORE_RESTART = 50;
    private static final AtomicInteger pageCounter = new AtomicInteger(0);
    
    public static ChromeOptions configureChromeBrowser(String chromePath) {
        LOGGER.fine("Configuring Chrome browser options");
        ChromeOptions options = new ChromeOptions();
        
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-images");
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NORMAL);
        
        String finalChromePath = determineChromePath(chromePath);
        if (finalChromePath != null && !finalChromePath.isEmpty()) {
            try {
                options.setBinary(finalChromePath);
                LOGGER.fine("Chrome binary path: " + finalChromePath);
            } catch (Exception e) {
                LOGGER.warning("Failed to set Chrome binary path: " + finalChromePath);
            }
        }
        
        return options;
    }
    
    private static String determineChromePath(String configChromePath) {
        String envChromeBin = System.getenv("CHROME_BIN");
        if (envChromeBin != null && !envChromeBin.isEmpty()) {
            return envChromeBin;
        }
        if (configChromePath != null && !configChromePath.isEmpty()) {
            return configChromePath;
        }
        return null;
    }
    
    public static WebDriver initializeBrowser(String chromePath, int timeoutSeconds) {
        LOGGER.info("Initializing browser");
        pageCounter.set(0);
        
        try {
            boolean useDockerDriver = System.getenv("CHROMEDRIVER_PATH") != null 
                || System.getenv("WEBDRIVER_CHROME_DRIVER") != null;
            if (!useDockerDriver) {
                var wdm = WebDriverManager.chromedriver();
                String driverVersion = System.getenv("CHROMEDRIVER_VERSION");
                if (driverVersion != null && !driverVersion.isEmpty()) {
                    wdm.driverVersion(driverVersion);
                }
                wdm.setup();
            }
            ChromeOptions options = configureChromeBrowser(chromePath);
            WebDriver driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            LOGGER.info("Browser initialized successfully");
            return driver;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize browser", e);
            throw new RuntimeException("Browser initialization failed", e);
        }
    }
    
    public static void closeBrowser(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                LOGGER.fine("Browser closed");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing browser", e);
            } finally {
                System.gc();
            }
        }
    }
    
    public static boolean shouldRestartBrowser() {
        return pageCounter.incrementAndGet() >= MAX_PAGES_BEFORE_RESTART;
    }
    
    public static void resetPageCounter() {
        pageCounter.set(0);
    }
    
    public static void navigateToUrl(WebDriver driver, WebDriverWait wait, String url) {
        LOGGER.fine("Navigating to: " + url);
        driver.get(url);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }
    
    public static void scrollToElement(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(200);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error scrolling to element", e);
        }
    }
    
    public static void clickElementWithJavaScript(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
    
    public static void handleCookieConsent(WebDriver driver) {
        String[] consentSelectors = {
            "#onetrust-accept-btn-handler",
            ".cookie-consent-button",
            ".accept-cookies",
            ".cookie-accept",
            "button[aria-label='Accept cookies']"
        };

        for (String selector : consentSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                    clickElementWithJavaScript(driver, buttons.get(0));
                    return;
                }
            } catch (Exception ignored) {}
        }
        
        String[] xpathSelectors = {
            "//button[contains(text(), 'Accept')]",
            "//button[contains(text(), 'Kabul')]"
        };

        for (String xpath : xpathSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.xpath(xpath));
                if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                    clickElementWithJavaScript(driver, buttons.get(0));
                    return;
                }
            } catch (Exception ignored) {}
        }
    }
    
    public static void handleOverlays(WebDriver driver) {
        try {
            String removeAllOverlaysScript = 
                "var selectors = [" +
                "  '.ot-sdk-row', '#onetrust-banner-sdk', '#onetrust-consent-sdk'," +
                "  '.onetrust-pc-dark-filter', '.cookie-banner', '.modal', '.popup'," +
                "  '.overlay', '[class*=\"cookie\"]', '[id*=\"cookie\"]'," +
                "  '[class*=\"consent\"]', '[id*=\"consent\"]'" +
                "];" +
                "selectors.forEach(function(sel) {" +
                "  document.querySelectorAll(sel).forEach(function(el) {" +
                "    el.style.display = 'none';" +
                "    el.style.pointerEvents = 'none';" +
                "  });" +
                "});";
            ((JavascriptExecutor) driver).executeScript(removeAllOverlaysScript);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error handling overlays", e);
        }
    }
    
    public static String extractTextWithMultipleSelectors(WebDriver driver, String elementType, String... selectors) {
        for (String selector : selectors) {
            try {
                WebElement element = driver.findElement(By.cssSelector(selector));
                String text = element.getText();
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            } catch (Exception ignored) {}
        }

        if ("title".equals(elementType)) {
            return driver.getTitle().replace(" - Biletix", "").trim();
        }

        return "";
    }
    
    public static int scrollToLoadAll(WebDriver driver, int maxScrolls, int waitMs, String contentSelector) {
        int previousCount = 0;
        int sameCountAttempts = 0;
        
        for (int i = 0; i < maxScrolls; i++) {
            try {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(waitMs);
                
                int currentCount = driver.findElements(By.cssSelector(contentSelector)).size();
                
                if (currentCount == previousCount) {
                    sameCountAttempts++;
                    if (sameCountAttempts >= 2) {
                        break;
                    }
                } else {
                    sameCountAttempts = 0;
                    previousCount = currentCount;
                }
            } catch (Exception e) {
                break;
            }
        }
        
        return previousCount;
    }
    
    public static int clickLoadMoreUntilEnd(WebDriver driver, WebDriverWait wait, 
            String buttonSelector, String contentSelector, int maxAttempts, int waitMs) {
        
        int previousCount = 0;
        handleCookieConsent(driver);
        handleOverlays(driver);
        
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(contentSelector)));
            previousCount = driver.findElements(By.cssSelector(contentSelector)).size();
        } catch (Exception e) {
            return 0;
        }
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(buttonSelector));
                if (buttons.isEmpty()) {
                    break;
                }
                
                WebElement loadMoreButton = buttons.get(0);
                if (!loadMoreButton.isDisplayed() || !loadMoreButton.isEnabled()) {
                    break;
                }
                
                scrollToElement(driver, loadMoreButton);
                
                try {
                    loadMoreButton.click();
                } catch (Exception e) {
                    clickElementWithJavaScript(driver, loadMoreButton);
                }
                
                Thread.sleep(waitMs);
                
                int currentCount = driver.findElements(By.cssSelector(contentSelector)).size();
                if (currentCount == previousCount && attempt > 0) {
                    break;
                }
                previousCount = currentCount;
                
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Load more click failed", e);
                break;
            }
        }
        
        return driver.findElements(By.cssSelector(contentSelector)).size();
    }
}