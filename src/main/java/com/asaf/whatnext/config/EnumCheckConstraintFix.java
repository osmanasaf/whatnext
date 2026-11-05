package com.asaf.whatnext.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Hibernate 6, {@code @Enumerated(STRING)} sütunlarına tablo oluşturulurken {@code check (source in (...))}
 * koyar; {@code ddl-auto=update} bu kısıtı yeni enum değerleriyle GÜNCELLEMEZ. Prod'daki {@code event}
 * tablosu Bubilet'e kadarki kaynaklarla oluşturuldu → BUGECE/PASSO kayıtları kısıt ihlaliyle düşerdi.
 * Enum zaten Java'da doğrulanıyor; kısıt kaldırılır (yalnız PostgreSQL, idempotent).
 */
@Component
@Order(0)
public class EnumCheckConstraintFix implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnumCheckConstraintFix.class);

    private final DataSource dataSource;

    public EnumCheckConstraintFix(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection c = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(c.getMetaData().getDatabaseProductName())) {
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("Veritabanı türü okunamadı, enum kısıt düzeltmesi atlandı: " + e.getMessage());
            return;
        }
        try {
            new JdbcTemplate(dataSource).execute("ALTER TABLE event DROP CONSTRAINT IF EXISTS event_source_check");
            LOGGER.info("event_source_check kısıtı kaldırıldı (varsa).");
        } catch (Exception e) {
            LOGGER.warn("event_source_check kaldırılamadı: " + e.getMessage());
        }
    }
}
