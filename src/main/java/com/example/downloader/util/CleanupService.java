package com.example.downloader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

@Component
public class CleanupService {
    
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);
    private static final Path TMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final String GLOB_PATTERN = "{media_*,download_*,thumb_*,socialdownloader}";
    private static final long MAX_AGE_SECONDS = 2 * 60 * 60; // 2 hours

    @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void cleanupOldFiles() {
        log.info("Starting cleanup of old files...");
        Instant cutoff = Instant.now().minusSeconds(MAX_AGE_SECONDS);
        int deletedCount = 0;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(TMP_DIR, GLOB_PATTERN)) {
            for (Path p : ds) {
                try {
                    if (Files.isDirectory(p)) {
                        // Clean directory contents
                        deletedCount += cleanupDirectory(p, cutoff);
                    } else {
                        // Clean individual file
                        FileTime ft = Files.getLastModifiedTime(p);
                        if (ft.toInstant().isBefore(cutoff)) {
                            if (Files.deleteIfExists(p)) {
                                deletedCount++;
                                log.debug("Deleted old file: {}", p.getFileName());
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to process path: {}", p, e);
                }
            }
        } catch (Exception e) {
            log.error("Error during cleanup process", e);
        }
        
        log.info("Cleanup completed. Deleted {} old files", deletedCount);
    }
    
    private int cleanupDirectory(Path dir, Instant cutoff) {
        int deletedCount = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path file : ds) {
                try {
                    FileTime ft = Files.getLastModifiedTime(file);
                    if (ft.toInstant().isBefore(cutoff)) {
                        if (Files.deleteIfExists(file)) {
                            deletedCount++;
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete file: {}", file, e);
                }
            }
            
            // Remove empty directory
            if (isDirectoryEmpty(dir)) {
                try {
                    Files.deleteIfExists(dir);
                    log.debug("Deleted empty directory: {}", dir.getFileName());
                } catch (IOException e) {
                    log.warn("Failed to delete empty directory: {}", dir, e);
                }
            }
        } catch (IOException e) {
            log.warn("Error cleaning directory: {}", dir, e);
        }
        
        return deletedCount;
    }
    
    private boolean isDirectoryEmpty(Path dir) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            return !ds.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
}
