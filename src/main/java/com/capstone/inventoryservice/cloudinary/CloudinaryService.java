package com.capstone.inventoryservice.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, Object> uploadFile(MultipartFile file, String folder) throws IOException {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "auto");
        options.put("folder", folder != null ? folder : "uploads");

        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh");
        }

        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder", folder != null ? folder : "images");
        options.put("public_id", publicId);
        options.put("overwrite", true);

        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadImageWithTransformation(MultipartFile file, String folder,
                                                             Integer width, Integer height) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh");
        }

        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder", folder != null ? folder : "images");
        options.put("public_id", publicId);
        options.put("overwrite", true);
        options.put("quality", "auto:good");

        if (width != null || height != null) {
            Map<String, Object> transformation =  new HashMap<>();
            options.put("crop", "limit");

            if (width != null) transformation.put("width", width);
            if (height != null) transformation.put("height", height);

            options.put("transformation", transformation);
        }

        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public String deleteFile(String publicId) throws IOException {
        var result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return result.get("result").toString();
    }

    public String getFileUrl(String publicId) {
        return cloudinary.url().generate(publicId);
    }
}