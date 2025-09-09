package com.example.downloader.controller;

import com.example.downloader.service.InfoCacheService;
import com.example.downloader.service.YtDlpService;
import com.example.downloader.service.VirusScanService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class DownloadController {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);
    
    @Autowired
    private YtDlpService ytDlpService;
    
    @Autowired
    private InfoCacheService infoCacheService;
    
    @Autowired
    private VirusScanService virusScanService;

    private final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"), "socialdownloader");

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/fetch")
    public String fetch(@RequestParam("url") String url, Model model) {
        try {
            log.info("Fetching info for URL: {}", url);
            
            // Check cache first
            JsonNode cachedInfo = infoCacheService.getIfReady(url);
            JsonNode info;
            
            if (cachedInfo != null) {
                log.info("Using cached info for URL: {}", url);
                info = cachedInfo;
            } else {
                // Fetch new info
                info = ytDlpService.fetchInfo(url);
                infoCacheService.put(url, info);
            }

            model.addAttribute("info", info);
            model.addAttribute("url", url);
            
            // Determine platform
            String platform = detectPlatform(url);
            model.addAttribute("platform", platform);
            
            return "result";
            
        } catch (Exception e) {
            log.error("Error fetching info for URL: {}", url, e);
            
            // Provide user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Chrome cookie database")) {
                errorMessage = "Chrome browser is blocking cookie access. Please close Chrome completely and try again, or use Firefox/Edge browser instead.";
            }
            
            model.addAttribute("error", errorMessage);
            return "index";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestParam("url") String url, 
                                    @RequestParam("format") String format) {
        Path file = null;
        
        try {
            log.info("Download request - URL: {}, Format: {}", url, format);
            
            Path outDir = TMP.resolve(String.valueOf(Math.abs(url.hashCode())));
            Files.createDirectories(outDir);

            file = ytDlpService.downloadFormat(url, format, outDir);

            // Virus scan
            log.info("Scanning file for viruses: {}", file.getFileName());
            if (!virusScanService.scan(file)) {
                log.warn("File failed virus scan: {}", file.getFileName());
                Files.deleteIfExists(file);
                return ResponseEntity.badRequest().body("File failed security scan and was deleted");
            }

            // Prepare file for download
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            String fileName = file.getFileName().toString();
            
            log.info("Serving file for download: {}", fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentLength(Files.size(file))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            log.error("Download failed for URL: {}", url, e);
            
            // Cleanup on error
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException cleanup) {
                    log.warn("Failed to cleanup file after error: {}", file, cleanup);
                }
            }
            
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Chrome cookie database")) {
                errorMessage = "Chrome browser is blocking cookie access. Please close Chrome and try again.";
            }
            
            return ResponseEntity.badRequest().body("Download failed: " + errorMessage);
        }
    }
    
    @GetMapping("/thumbnail")
    public ResponseEntity<?> getThumbnail(@RequestParam("url") String url) {
        try {
            JsonNode info = infoCacheService.getIfReady(url);
            if (info == null) {
                return ResponseEntity.badRequest().body("Media info not available. Please fetch info first.");
            }
            
            Path outDir = TMP.resolve(String.valueOf(Math.abs(url.hashCode())));
            Files.createDirectories(outDir);
            
            Path thumbnail = ytDlpService.downloadThumbnail(info, outDir);
            
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(thumbnail));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + thumbnail.getFileName() + "\"")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Thumbnail download failed for URL: {}", url, e);
            return ResponseEntity.badRequest().body("Thumbnail download failed: " + e.getMessage());
        }
    }
    
    private String detectPlatform(String url) {
        if (url.contains("instagram.com")) {
            return "instagram";
        } else if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return "youtube";
        } else if (url.contains("tiktok.com")) {
            return "tiktok";
        } else if (url.contains("twitter.com") || url.contains("x.com")) {
            return "twitter";
        }
        return "unknown";
    }
}
