package com.asaf.whatnext.scraper.web;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chrome başlatma, kapatma ve tarama sayacı (base scraper ömrü).
 */
public final class ScraperBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(ScraperBrowser.class);
    private static final int MAX_PAGES_BEFORE_RESTART = 50;
    private static final AtomicInteger PAGE_COUNTER = new AtomicInteger(0);

    private ScraperBrowser() {
    }

    public static ChromeOptions configureChromeBrowser(String chromePath) {
        LOG.debug("Configuring Chrome browser options");
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
        options.addArguments(
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Safari/537.36");

        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.NORMAL);

        String finalChromePath = determineChromePath(chromePath);
        if (finalChromePath != null && !finalChromePath.isEmpty()) {
            try {
                options.setBinary(finalChromePath);
                LOG.debug("Chrome binary path: {}", finalChromePath);
            } catch (Exception e) {
                LOG.warn("Failed to set Chrome binary path: {}", finalChromePath);
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
        LOG.info("Initializing browser");
        PAGE_COUNTER.set(0);

        try {
            boolean useDockerDriver = System.getenv("CHROMEDRIVER_PATH") != null
                    || System.getenv("WEBDRIVER_CHROME_DRIVER") != null;
            if (!useDockerDriver) {
                LOG.info("WebDriverManager: ChromeDriver eşleştiriliyor (ilk çalışmada indirme uzun sürebilir)…");
                var wdm = WebDriverManager.chromedriver();
                String driverVersion = System.getenv("CHROMEDRIVER_VERSION");
                if (driverVersion != null && !driverVersion.isEmpty()) {
                    wdm.driverVersion(driverVersion);
                }
                wdm.setup();
                LOG.info("WebDriverManager: ChromeDriver hazır");
            } else {
                LOG.info("Ortam değişkeninden ChromeDriver kullanılıyor (CHROMEDRIVER_PATH / WEBDRIVER_CHROME_DRIVER)");
            }
            ChromeOptions options = configureChromeBrowser(chromePath);
            LOG.info("Chrome process başlatılıyor…");
            WebDriver driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            LOG.info("Browser initialized successfully");
            return driver;
        } catch (Exception e) {
            LOG.error("Failed to initialize browser", e);
            throw new RuntimeException("Browser initialization failed", e);
        }
    }

    public static void closeBrowser(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                LOG.debug("Browser closed");
            } catch (Exception e) {
                LOG.warn("Error closing browser", e);
            } finally {
                System.gc();
            }
        }
    }

    public static boolean shouldRestartBrowser() {
        return PAGE_COUNTER.incrementAndGet() >= MAX_PAGES_BEFORE_RESTART;
    }

    public static void resetPageCounter() {
        PAGE_COUNTER.set(0);
    }
}
