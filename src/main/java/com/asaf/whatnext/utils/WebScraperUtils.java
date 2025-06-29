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
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebScraperUtils {
    private static final Logger LOGGER = Logger.getLogger(WebScraperUtils.class.getName());
    
    public static ChromeOptions configureChromeBrowser(String chromePath) {
        LOGGER.info("Configuring Chrome browser options");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        LOGGER.info("Chrome arguments configured");
        
        // Chrome binary path'ini belirle
        String finalChromePath = determineChromePath(chromePath);
        
        if (finalChromePath != null && !finalChromePath.isEmpty()) {
            try {
                options.setBinary(finalChromePath);
                LOGGER.info("Chrome binary path set to: " + finalChromePath);
            } catch (Exception e) {
                LOGGER.warning("Failed to set Chrome binary path: " + finalChromePath + ". Using default Chrome installation.");
            }
        } else {
            LOGGER.info("Using default Chrome installation (WebDriverManager will handle the driver)");
        }
        
        LOGGER.info("ChromeOptions configuration completed");
        return options;
    }
    
    private static String determineChromePath(String configChromePath) {
        LOGGER.info("Determining Chrome path...");
        LOGGER.info("Config Chrome path: " + configChromePath);
        
        // Önce environment variable'ları kontrol et
        String envChromeBin = System.getenv("CHROME_BIN");
        LOGGER.info("CHROME_BIN environment variable: " + envChromeBin);
        
        if (envChromeBin != null && !envChromeBin.isEmpty()) {
            LOGGER.info("Using Chrome path from CHROME_BIN environment variable: " + envChromeBin);
            return envChromeBin;
        }
        
        // Sonra config'den gelen path'i kontrol et
        if (configChromePath != null && !configChromePath.isEmpty() && 
            !configChromePath.equals("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")) {
            LOGGER.info("Using Chrome path from configuration: " + configChromePath);
            return configChromePath;
        }
        
        // Hiçbiri yoksa null döndür (WebDriverManager otomatik bulacak)
        LOGGER.info("No specific Chrome path found, WebDriverManager will auto-detect");
        return null;
    }
    
    public static WebDriver initializeBrowser(String chromePath, int timeoutSeconds) {
        LOGGER.info("Initializing browser with timeout: " + timeoutSeconds + " seconds");
        LOGGER.info("Chrome path parameter: " + chromePath);
        
        try {
            // WebDriverManager'ı kullanarak ChromeDriver'ı otomatik olarak yönet
            LOGGER.info("Setting up ChromeDriver with WebDriverManager...");
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = configureChromeBrowser(chromePath);
            LOGGER.info("ChromeOptions configured successfully");
            
            LOGGER.info("Creating ChromeDriver instance...");
            WebDriver driver = new ChromeDriver(options);
            
            LOGGER.info("Browser initialized successfully");
            return driver;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize browser", e);
            throw new RuntimeException("Browser initialization failed", e);
        }
    }
    
    public static void closeBrowser(WebDriver driver) {
        if (driver != null) {
            LOGGER.info("Closing browser");
            try {
                driver.quit();
                LOGGER.info("Browser closed successfully");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing browser", e);
            }
        }
    }
    
    public static void navigateToUrl(WebDriver driver, WebDriverWait wait, String url) {
        LOGGER.fine("Navigating to: " + url);
        driver.get(url);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }
    
    public static void scrollToElement(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(500);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error scrolling to element", e);
        }
    }
    
    public static void clickElementWithJavaScript(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
    
    public static void handleCookieConsent(WebDriver driver) {
        try {
            String[] consentSelectors = {
                "#onetrust-accept-btn-handler",
                ".ot-sdk-container button.accept-cookies",
                ".ot-sdk-container .ot-sdk-button",
                ".cookie-consent-button",
                ".accept-cookies",
                ".cookie-accept",
                "button[aria-label='Accept cookies']"
            };

            String[] xpathSelectors = {
                "//button[contains(text(), 'Accept')]",
                "//button[contains(text(), 'Kabul')]",
                "//a[contains(text(), 'Accept')]",
                "//a[contains(text(), 'Kabul')]"
            };

            for (String selector : consentSelectors) {
                try {
                    List<WebElement> consentButtons = driver.findElements(By.cssSelector(selector));
                    if (!consentButtons.isEmpty()) {
                        clickElementWithJavaScript(driver, consentButtons.get(0));
                        LOGGER.info("Clicked cookie consent button with CSS selector: " + selector);
                        Thread.sleep(1000);
                        return;
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }

            for (String xpath : xpathSelectors) {
                try {
                    List<WebElement> consentButtons = driver.findElements(By.xpath(xpath));
                    if (!consentButtons.isEmpty()) {
                        clickElementWithJavaScript(driver, consentButtons.get(0));
                        LOGGER.info("Clicked cookie consent button with XPath: " + xpath);
                        Thread.sleep(1000);
                        return;
                    }
                } catch (Exception e) {
                    // Continue to next selector
                }
            }

            List<WebElement> otSdkRows = driver.findElements(By.cssSelector("div.ot-sdk-row"));
            if (!otSdkRows.isEmpty()) {
                try {
                    WebElement otSdkRow = otSdkRows.get(0);
                    List<WebElement> buttons = otSdkRow.findElements(By.tagName("button"));
                    if (!buttons.isEmpty()) {
                        clickElementWithJavaScript(driver, buttons.get(0));
                        LOGGER.info("Clicked button in ot-sdk-row");
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Failed to interact with ot-sdk-row", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error handling cookie consent", e);
        }
    }
    
    public static void handleOverlays(WebDriver driver) {
        try {
            String otSdkRowScript = 
                "var otRows = document.querySelectorAll('.ot-sdk-row');" +
                "for(var i=0; i<otRows.length; i++) {" +
                "  otRows[i].style.display = 'none';" +
                "  otRows[i].style.zIndex = '-1000';" +
                "  otRows[i].style.pointerEvents = 'none';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(otSdkRowScript);

            String oneTrustScript = 
                "var otContainers = document.querySelectorAll('#onetrust-banner-sdk, #onetrust-consent-sdk, .onetrust-pc-dark-filter');" +
                "for(var i=0; i<otContainers.length; i++) {" +
                "  otContainers[i].style.display = 'none';" +
                "  otContainers[i].style.zIndex = '-1000';" +
                "  otContainers[i].style.pointerEvents = 'none';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(oneTrustScript);

            String removeOverlaysScript = 
                "var overlays = document.querySelectorAll('.cookie-banner, .modal, .popup, .overlay, [class*=\"cookie\"], [id*=\"cookie\"], [class*=\"consent\"], [id*=\"consent\"]);" +
                "for(var i=0; i<overlays.length; i++) {" +
                "  overlays[i].style.display = 'none';" +
                "  overlays[i].style.zIndex = '-1000';" +
                "  overlays[i].style.pointerEvents = 'none';" +
                "}";
            ((JavascriptExecutor) driver).executeScript(removeOverlaysScript);

            Thread.sleep(500);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error handling overlays", e);
        }
    }
    
    public static String extractTextWithMultipleSelectors(WebDriver driver, String elementType, String... selectors) {
        try {
            for (String selector : selectors) {
                try {
                    return driver.findElement(By.cssSelector(selector)).getText();
                } catch (Exception e) {
                    // Continue to next selector
                }
            }

            if (elementType.equals("title")) {
                return driver.getTitle().replace(" - Biletix", "").trim();
            }

            return "";
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error extracting " + elementType, e);
            return "";
        }
    }
}