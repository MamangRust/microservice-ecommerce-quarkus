package com.sanedge.gateway.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import com.sanedge.gateway.service.FileService;

@ApplicationScoped
public class FileServiceImpl implements FileService {
    @Override
    public String createFileImage(FileUpload file, String filepath) {
        try {
            Path destinationPath = Path.of(filepath);

            Files.createDirectories(destinationPath.getParent());

            // RESTEasy Reactive FileUpload's uploadedFile() returns the path to the temp file
            Files.copy(file.uploadedFile(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            return filepath;
        } catch (IOException e) {
            System.err.println("❌ Failed to create file: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteFileImage(String filepath) {
        try {
            Path filePath = Path.of(filepath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("✅ Deleted file: " + filepath);
            } else {
                System.err.println("⚠️ File does not exist: " + filepath);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete file: " + e.getMessage());
        }
    }
}
