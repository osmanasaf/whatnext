package com.asaf.whatnext.scraper.common;

/**
 * Liste sayfasındaki tarih alanına göre veri toplama stratejisi.
 */
public enum DateScrapeStrategy {
    /** Detay gerekmeden (tek gün / tek aralık satırı) liste veya aynı sayfadan. */
    FROM_LIST_ONLY,
    /** Çok gün, kesik liste vb.: detay sayfasından seans / tarih okunacak. */
    REQUIRES_DETAIL_PAGE
}
