package com.serv.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DEV ONLY — active when Spring profile is "dev".
 *
 * Mimics what Nginx does in production: serves files from disk under
 * the /media/** URL prefix so that media.public.base-url can point to
 * http://localhost:8080/media during local development.
 *
 * This controller is NEVER active in production (profile = "prod").
 * In production, Nginx handles /media/** before requests even reach Spring.
 *
 * Usage:
 *   Set spring.profiles.active=dev in your local application.properties.
 *   Set media.public.base-url=http://localhost:8080/media
 *
 * The {*path} pattern matches any depth:
 *   /media/originals/1/abc.jpg
 *   /media/thumbs/main/1/abc_main.jpg
 *   /media/videos/1/abc.mp4
 */
@RestController
@RequestMapping("/media")
@Profile("dev")   // ← Only loaded when running with --spring.profiles.active=dev
public class DevMediaController {

    @Value("${media.upload.base}")
    private String uploadBase;

    @GetMapping("/**")
    public ResponseEntity<Resource> serveMedia(
            @RequestAttribute(name = "jakarta.servlet.forward.request_uri",
                    required = false) String forwardUri,
            jakarta.servlet.http.HttpServletRequest request) {

        // Extract the path after /media/
        String requestPath = request.getRequestURI();
        String subPath = requestPath.replaceFirst("^/media/", "");

        try {
            Path filePath = Paths.get(uploadBase).resolve(subPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String filename = filePath.getFileName().toString();
            MediaType mediaType = guessMediaType(filename);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private MediaType guessMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".mp4"))  return MediaType.parseMediaType("video/mp4");
        if (lower.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}