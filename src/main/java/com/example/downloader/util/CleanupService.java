package com.example.downloader.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Component
public class CleanupService {
    private static final Path TMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final String GLOB = "media_*";
    private static final long MAX_AGE_SECONDS = 2 * 60 * 60; // 2 hours

    @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void cleanupOldFiles() {
        Instant cutoff = Instant.now().minusSeconds(MAX_AGE_SECONDS);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(TMP_DIR, GLOB)) {
            for (Path p : ds) {
                try {
                    FileTime ft = Files.getLastModifiedTime(p);
                    if (ft.toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
