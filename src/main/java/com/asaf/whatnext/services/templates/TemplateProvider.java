package com.asaf.whatnext.services.templates;

import java.io.IOException;

/**
 * Ay + gönderi türüne göre şablon PNG'si + koordinat dosyası çözer.
 * {@code postType}: {@code post}, {@code cover}, {@code story}.
 */
public interface TemplateProvider {
    TemplateAssets resolve(String monthName, String postType) throws IOException;
}
