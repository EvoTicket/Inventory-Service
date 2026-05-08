package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Event;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final Cloudinary cloudinary;

    @Async
    public CompletableFuture<Void> uploadImageAsync(Event event, byte[] imageBytes, String type) {
        String folder = "event/" + event.getId() + "/" + type + "/";
        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder", folder);
        options.put("public_id", publicId);
        options.put("overwrite", true);

        try {
            var uploadResult = cloudinary.uploader().upload(imageBytes, options);
            String url = uploadResult.get("url").toString();
            switch (type) {
                case "banner":
                    event.setBannerImage(url);
                    break;
                case "thumbnail":
                    event.setThumbnailImage(url);
                    break;
                case "seat_map":
                    event.setSeatMapImage(url);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> uploadTicketTypeImageAsync(com.capstone.inventoryservice.model.entity.TicketType ticketType, byte[] imageBytes) {
        String folder = "event/" + ticketType.getShowtime().getEvent().getId() + "/ticket_types/";
        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder", folder);
        options.put("public_id", publicId);
        options.put("overwrite", true);

        try {
            var uploadResult = cloudinary.uploader().upload(imageBytes, options);
            String url = uploadResult.get("url").toString();
            ticketType.setThumbnailImage(url);
        } catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh vé lên Cloudinary: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}
