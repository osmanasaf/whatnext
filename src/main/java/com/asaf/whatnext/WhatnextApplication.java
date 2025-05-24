package com.asaf.whatnext;

import com.asaf.whatnext.services.BiletixScraper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WhatnextApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatnextApplication.class, args);
	}
}
