package com.asaf.whatnext.scraper.web;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Sayfa gezintisi, çerez/overlay, kaydırma ve "daha fazla yükle".
 */
public final class ScraperNavigation {

    private static final Logger LOG = LoggerFactory.getLogger(ScraperNavigation.class);

    private ScraperNavigation() {
    }

    public static void navigateToUrl(WebDriver driver, WebDriverWait wait, String url) {
        LOG.info("Sayfa açılıyor: {}", url);
        driver.get(url);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        LOG.info("Sayfa yüklendi (body hazır): {}", url);
    }

    public static void scrollToElement(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(200);
        } catch (Exception e) {
            LOG.debug("Error scrolling to element", e);
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
            } catch (Exception ignored) {
            }
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
            } catch (Exception ignored) {
            }
        }
    }

    public static void handleOverlays(WebDriver driver) {
        try {
            String removeAllOverlaysScript =
                    "var selectors = ["
                            + "  '.ot-sdk-row', '#onetrust-banner-sdk', '#onetrust-consent-sdk',"
                            + "  '.onetrust-pc-dark-filter', '.cookie-banner', '.modal', '.popup',"
                            + "  '.overlay', '[class*=\"cookie\"]', '[id*=\"cookie\"]',"
                            + "  '[class*=\"consent\"]', '[id*=\"consent\"]'"
                            + "];"
                            + "selectors.forEach(function(sel) {"
                            + "  document.querySelectorAll(sel).forEach(function(el) {"
                            + "    el.style.display = 'none';"
                            + "    el.style.pointerEvents = 'none';"
                            + "  });"
                            + "});";
            ((JavascriptExecutor) driver).executeScript(removeAllOverlaysScript);
        } catch (Exception e) {
            LOG.debug("Error handling overlays", e);
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
            } catch (Exception ignored) {
            }
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

    /**
     * Gerçek tekerlek olayıyla (CDP mouseWheel) sayfa sonuna kaydırır — {@code window.scrollTo}
     * bazı Angular sonsuz-kaydırma listelerini (Biletix) tetiklemez. Kart sayısı iki denemede
     * artmazsa durur.
     */
    public static int wheelScrollToLoadAll(WebDriver driver, int maxScrolls, int waitMs, String contentSelector) {
        int previousCount = driver.findElements(By.cssSelector(contentSelector)).size();
        int sameCountAttempts = 0;
        for (int i = 0; i < maxScrolls; i++) {
            try {
                new Actions(driver).scrollByAmount(0, 5000).perform();
                Thread.sleep(waitMs);
                int currentCount = driver.findElements(By.cssSelector(contentSelector)).size();
                if (currentCount == previousCount) {
                    if (++sameCountAttempts >= 2) {
                        break;
                    }
                } else {
                    sameCountAttempts = 0;
                    previousCount = currentCount;
                }
            } catch (Exception e) {
                LOG.debug("Wheel scroll failed", e);
                break;
            }
        }
        return previousCount;
    }

    public static int clickLoadMoreUntilEnd(
            WebDriver driver,
            WebDriverWait wait,
            String buttonSelector,
            String contentSelector,
            int maxAttempts,
            int waitMs) {

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
                LOG.debug("Load more click failed", e);
                break;
            }
        }

        return driver.findElements(By.cssSelector(contentSelector)).size();
    }
}
