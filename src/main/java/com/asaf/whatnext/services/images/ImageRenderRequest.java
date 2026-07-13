package com.asaf.whatnext.services.images;

import java.time.LocalDate;

/**
 * Bir Instagram görseli üretim isteği — {@code InstagramService.createImage}'in bugünkü
 * {@code (eventType, postType, city, date)} sözleşmesiyle birebir. Üç motor da
 * ({@link CustomImageEngine}, {@code RemotionStillEngine}, {@code AiBackgroundEngine}) bunu tüketir.
 *
 * @param eventType "concert" | "theater" | "standup" | "cover" | "smart_story"
 * @param postType  "post" | "story" | "cover"
 * @param city      genelde "istanbul"; cover/story için kullanılmaz (null olabilir)
 */
public record ImageRenderRequest(String eventType, String postType, String city, LocalDate date) {
}
