package com.geosun.rmpd.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageService {

    private final Path basePath;

    public LocalFileStorageService(@Value("${storage.cmr-path:./data/cmr}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    public StoredFile store(byte[] content, String originalFilename) throws IOException {
        Files.createDirectories(basePath);
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = basePath.resolve(UUID.randomUUID() + "_" + safeName);
        Files.write(target, content);
        return new StoredFile(target.toString(), content.length);
    }

    public byte[] read(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }

    public record StoredFile(String path, long sizeBytes) {}
}
