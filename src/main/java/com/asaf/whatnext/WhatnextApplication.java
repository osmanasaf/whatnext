package com.asaf.whatnext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.asaf.whatnext.services.BiletixScraper;

@SpringBootApplication
public class WhatnextApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatnextApplication.class, args);
	}

	/**
	 * CommandLineRunner bean that runs after the application context is loaded.
	 * Fetches events from Biletix and saves them to the database with duplicate checking.
	 * 
	 * @param biletixScraper The BiletixScraper bean
	 * @return CommandLineRunner
	 */
	@Bean
	public CommandLineRunner fetchAndSaveEvents(BiletixScraper biletixScraper) {
		return args -> {
			biletixScraper.fetchEvents();
		};
	}
}
