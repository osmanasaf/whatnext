package com.asaf.whatnext.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cron görevleri (scraping + Instagram paylaşımı) buradan açılıp kapanır.
 * Varsayılan açık; ilk deploy ve testte {@code whatnext.scheduler.enabled=false} verilerek
 * kapatılır — bean'ler ayakta kalır, {@code /api/instagram/**} manuel uçları çalışmaya devam eder,
 * yalnızca zamanlanmış tetikleme durur (aksi halde ayağa kalkar kalkmaz 11:50/12:00 story
 * oluşturup gerçek hesaba paylaşır).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "whatnext.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
