package com.example.downloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class CookieService {
    
    private static final Logger log = LoggerFactory.getLogger(CookieService.class);
    private final Path cookieFile = Paths.get(System.getProperty("java.io.tmpdir"), "cookies.txt");
    
    public boolean extractBrowserCookies() {
        try {
            log.info("Extracting browser cookies...");
            
            ProcessBuilder pb = new ProcessBuilder(
                "yt-dlp", 
                "--cookies-from-browser", "chrome",
                "--cookies", cookieFile.toString(),
                "--simulate",
                "https://www.instagram.com" // Dummy URL to extract cookies
            );
            
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                p.destroyForcibly();
                log.warn("Cookie extraction timeout");
                return false;
            }
            
            int exit = p.exitValue();
            if (exit == 0 && Files.exists(cookieFile)) {
                log.info("Browser cookies extracted successfully");
                return true;
            } else {
                log.warn("Cookie extraction failed with exit code: {}", exit);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error extracting browser cookies", e);
            return false;
        }
    }
    
    public Path getCookieFile() {
        return cookieFile;
    }
    
    public boolean hasCookies() {
        return Files.exists(cookieFile) && cookieFile.toFile().length() > 0;
    }
    
    public void refreshCookies() {
        try {
            Files.deleteIfExists(cookieFile);
            extractBrowserCookies();
        } catch (IOException e) {
            log.error("Failed to refresh cookies", e);
        }
    }
}
