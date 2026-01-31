package com.asaf.whatnext.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScraperConfig {
    
    @Value("${scraper.chrome.path:}")
    private String chromePath;
    
    @Value("${scraper.wait.timeout:30}")
    private int waitTimeoutSeconds;
    
    @Value("${scraper.max.scroll.attempts:15}")
    private int maxScrollAttempts;
    
    @Value("${scraper.scroll.wait.ms:1500}")
    private int scrollWaitMs;
    
    @Value("${scraper.max.load.more.attempts:15}")
    private int maxLoadMoreAttempts;
    
    @Value("${scraper.load.more.wait.ms:1500}")
    private int loadMoreWaitMs;
    
    @Value("${scraper.image.upload.enabled:true}")
    private boolean imageUploadEnabled;
    
    public String getChromePath() {
        String envChromeBin = System.getenv("CHROME_BIN");
        if (envChromeBin != null && !envChromeBin.isEmpty()) {
            return envChromeBin;
        }
        
        if (chromePath == null || chromePath.isEmpty() || 
            chromePath.contains("Program Files")) {
            return null;
        }
        
        return chromePath;
    }

    public int getWaitTimeoutSeconds() {
        return waitTimeoutSeconds;
    }

    public int getMaxScrollAttempts() {
        return maxScrollAttempts;
    }

    public int getScrollWaitMs() {
        return scrollWaitMs;
    }

    public int getMaxLoadMoreAttempts() {
        return maxLoadMoreAttempts;
    }

    public int getLoadMoreWaitMs() {
        return loadMoreWaitMs;
    }

    public boolean isImageUploadEnabled() {
        return imageUploadEnabled;
    }
}
