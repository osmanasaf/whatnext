package com.asaf.whatnext.services.templates;

import java.io.File;
import java.util.Properties;

/**
 * Bir Instagram şablonu için: PNG dosyası + eşleşen koordinat/layout properties'i.
 * Kaynağı ne olursa olsun (static classpath, üretilmiş, AI) çıktı sözleşmesi aynıdır —
 * {@code ImageGenerationService}'in ImageMagick komut üretimi bunu bilmez.
 */
public record TemplateAssets(File templatePng, Properties coordinates) {
}
