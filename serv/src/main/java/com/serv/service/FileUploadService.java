package com.serv.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadService {

    @Value("${image.upload.dir}")
    private String imageUploadDir;

    @Value("${video.upload.dir}")
    private String videoUploadDir;

    public String saveImage(MultipartFile file) throws IOException {
        return saveFile(imageUploadDir,file);
    }

    public String saveVideo(MultipartFile file) throws IOException {
        return saveFile(videoUploadDir ,file);
    }

    private String saveFile(String folder,MultipartFile file) throws IOException {
        // Ensure the upload directory exists
        Path uploadPath = Paths.get(folder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique filename and save the file
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());
        // for large files, consider file.transferTo(filePath.toFile()) to avoid loading the whole file in memory
        return fileName;
    }
}