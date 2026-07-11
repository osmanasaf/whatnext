package com.asaf.whatnext.scraper.bubilet;

import com.asaf.whatnext.config.BubiletScraperConfig;
import com.asaf.whatnext.scraper.common.ElementTextExtractor;
import com.asaf.whatnext.scraper.common.TurkishDateParser;
import com.asaf.whatnext.utils.WebScraperUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bubilet etkinlik detay (çoklu tarih, tek günlü mekân) okuma.
 */
public class BubiletDetailParser {

    private static final Logger LOG = LoggerFactory.getLogger(BubiletDetailParser.class);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final int biletlerSectionRolloverDays;

    public BubiletDetailParser(WebDriver driver, WebDriverWait wait, int biletlerSectionRolloverDays) {
        this.driver = driver;
        this.wait = wait;
        this.biletlerSectionRolloverDays = biletlerSectionRolloverDays;
    }

    public record MultiDateDetail(List<LocalDate> dates, String venue) {
    }

    public MultiDateDetail loadMultiDate(String url) {
        try {
            WebScraperUtils.navigateToUrl(driver, wait, url);
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id(BubiletScraperConfig.DETAIL_BILETLER_SECTION_ID)));
            String detailVenue = readVenueFromOpenPage();
            WebElement section = driver.findElement(By.id(BubiletScraperConfig.DETAIL_BILETLER_SECTION_ID));
            List<LocalDate> dates = TurkishDateParser.extractDayMonthFromTurkishText(
                    section.getText(), com.asaf.whatnext.utils.TrClock.today(), biletlerSectionRolloverDays);
            return new MultiDateDetail(dates, detailVenue);
        } catch (Exception e) {
            LOG.warn("Bubilet detay sayfası okunamadı: {} — {}", url, e.getMessage());
            return new MultiDateDetail(List.of(), null);
        }
    }

    public String loadVenueForSingleDate(String url) {
        try {
            WebScraperUtils.navigateToUrl(driver, wait, url);
            WebScraperUtils.handleCookieConsent(driver);
            WebScraperUtils.handleOverlays(driver);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("main, body")));
            return readVenueFromOpenPage();
        } catch (Exception e) {
            LOG.debug("Bubilet detay (tek seans) mekân okunamadı: {} — {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Açık detay sayfasındayken; boşsa {@code null} (eski tryReadBubiletDetailVenue ile uyumlu).
     */
    public String readVenueFromOpenPage() {
        String s = ElementTextExtractor.firstNonBlank(
                sel -> {
                    try {
                        return driver.findElement(By.cssSelector(sel)).getText();
                    } catch (Exception e) {
                        return "";
                    }
                },
                BubiletScraperConfig.DETAIL_VENUE_LINK_SELECTORS);
        return s.isEmpty() ? null : s;
    }
}
