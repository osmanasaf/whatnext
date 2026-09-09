package com.asaf.whatnext;

import com.asaf.whatnext.scraper.BiletixScraper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WhatnextApplication {

	public static void main(String[] args) {
		System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
		SpringApplication.run(WhatnextApplication.class, args);
	}
}
