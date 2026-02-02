package com.asaf.whatnext.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map<String, Object> uploadImage(File file, String folder) throws IOException {
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "format", "webp",
                "quality", "auto:good"
        );
        return cloudinary.uploader().upload(file, options);
    }

    public Map<String, Object> uploadImageFromBytes(byte[] imageBytes, String folder, String fileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }
        
        if (imageBytes.length < 5120) {
            throw new IllegalArgumentException("Image too small, minimum size is 5KB, got: " + imageBytes.length + " bytes");
        }
        
        if (imageBytes.length > 10485760) {
            throw new IllegalArgumentException("Image too large, maximum size is 10MB, got: " + imageBytes.length + " bytes");
        }
        
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "public_id", fileName,
                "overwrite", true,
                "invalidate", true,
                "format", "webp",
                "quality", "auto:good"
        );
        
        try {
            return cloudinary.uploader().upload(imageBytes, options);
        } catch (Exception e) {
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> uploadVideo(File file, String folder) throws IOException {
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "video"
        );
        return cloudinary.uploader().upload(file, options);
    }

    public String getImageUrl(Map<String, Object> uploadResult) {
        return (String) uploadResult.get("secure_url");
    }

    public Map<String, Object> uploadImageFromUrl(String imageUrl, String folder, String fileName) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "public_id", fileName,
                "overwrite", true,
                "invalidate", true,
                "format", "webp",
                "quality", "auto:good"
        );
        
        try {
            return cloudinary.uploader().upload(imageUrl, options);
        } catch (Exception e) {
            throw new IOException("Failed to upload image from URL to Cloudinary: " + e.getMessage(), e);
        }
    }

    public boolean deleteByPublicId(String publicId) throws IOException {
        if (publicId == null || publicId.isEmpty()) return false;
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            Object status = result.get("result");
            return status != null && ("ok".equals(status) || "not_found".equals(status));
        } catch (Exception e) {
            throw new IOException("Failed to delete Cloudinary asset: " + publicId, e);
        }
    }
} 