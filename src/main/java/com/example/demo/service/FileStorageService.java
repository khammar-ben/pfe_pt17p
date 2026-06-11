package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String saveImage(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }

        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Format image non supporte: " + extension);
        }

        try {
            Path categoryDir = uploadDir.resolve(sanitize(category)).normalize();
            Files.createDirectories(categoryDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = categoryDir.resolve(filename).normalize();
            if (!target.startsWith(categoryDir)) {
                throw new IllegalArgumentException("Nom de fichier invalide");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return sanitize(category) + "/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier", exception);
        }
    }

    public Resource load(String relativePath) {
        try {
            Path file = uploadDir.resolve(relativePath).normalize();
            if (!file.startsWith(uploadDir) || !Files.exists(file)) {
                throw new NotFoundException("Fichier introuvable");
            }
            return new UrlResource(file.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de lire le fichier", exception);
        }
    }

    private String extension(String filename) {
        String clean = StringUtils.cleanPath(filename == null ? "" : filename);
        int index = clean.lastIndexOf('.');
        if (index < 0 || index == clean.length() - 1) {
            throw new IllegalArgumentException("Extension image obligatoire");
        }
        return clean.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
