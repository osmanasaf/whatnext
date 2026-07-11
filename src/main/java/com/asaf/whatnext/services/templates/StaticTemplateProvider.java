package com.asaf.whatnext.services.templates;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Bugünkü davranış birebir korunur: classpath'teki {@code instagram-posts/<month>/<postType>-template.png}
 * + {@code -coordinates.properties} çiftini geçici dosyalara kopyalar.
 */
@Component
public class StaticTemplateProvider implements TemplateProvider {

    @Override
    public TemplateAssets resolve(String monthName, String postType) throws IOException {
        String templateResource = "instagram-posts/" + monthName + "/" + postType + "-template.png";
        String coordinatesResource = resolveCoordinatesResource(monthName, postType);

        File templateFile = copyResourceToTempFile(templateResource, ".png");
        File coordinatesFile = copyResourceToTempFile(coordinatesResource, ".properties");
        return new TemplateAssets(templateFile, loadPropertiesUtf8(coordinatesFile));
    }

    private static String resolveCoordinatesResource(String monthName, String postType) {
        String specific = "instagram-posts/" + monthName + "/" + postType + "-coordinates.properties";
        if (StaticTemplateProvider.class.getClassLoader().getResource(specific) != null) {
            return specific;
        }
        return "instagram-posts/" + monthName + "/post-coordinates.properties";
    }

    private static Properties loadPropertiesUtf8(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }
        Properties p = new Properties();
        p.load(new StringReader(content));
        return p;
    }

    private static File copyResourceToTempFile(String resourcePath, String suffix) throws IOException {
        InputStream input = StaticTemplateProvider.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        File tempFile = File.createTempFile("whatnext-resource-", suffix);
        tempFile.deleteOnExit();
        Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        input.close();
        return tempFile;
    }
}
