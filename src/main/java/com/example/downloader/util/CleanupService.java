package com.example.downloader.util;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Component
public class CleanupService {

    // Delete files older than this many seconds (default 2 hours = 7200s)
    private final long maxAgeSeconds = 7200;

    // Runs every 30 minutes
    @Scheduled(fixedDelayString = "PT30M")
    public void cleanupTempFiles() {
        try {
            Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "socialdownloader");
            if (!Files.exists(tmpDir)) return;
            DirectoryStream<Path> ds = Files.newDirectoryStream(tmpDir);
            Instant cutoff = Instant.now().minusSeconds(maxAgeSeconds);
            for (Path p : ds) {
                try {
                    FileTime ft = Files.getLastModifiedTime(p);
                    if (ft.toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException ignored) {}
            }
        } catch (Exception e) {
            // log if you have logger (omitted for brevity)
        }
    }
}
