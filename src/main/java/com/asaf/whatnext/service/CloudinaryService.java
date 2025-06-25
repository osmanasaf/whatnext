package com.asaf.whatnext.service;

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
                "resource_type", "image"
        );
        return cloudinary.uploader().upload(file, options);
    }
} 