package com.example.downloader.controller;

import com.example.downloader.service.YtDlpService;
import com.example.downloader.service.VirusScanService;
import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class DownloadController {

    private final YtDlpService ytDlpService;
    private final VirusScanService virusScanService;

    public DownloadController(YtDlpService ytDlpService, VirusScanService virusScanService) {
        this.ytDlpService = ytDlpService;
        this.virusScanService = virusScanService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/info")
    public String getInfo(@RequestParam String url, @RequestParam String type, Model model) {
        model.addAttribute("activeTab", type);
        try {
            String normalized = normalizeUrl(url);
            JsonNode info = ytDlpService.getInfo(normalized);
            model.addAttribute("info", info);
            model.addAttribute("formats", info.get("formats"));
            model.addAttribute("title", info.has("title") ? info.get("title").asText() : "");
            model.addAttribute("thumbnail", info.has("thumbnail") ? info.get("thumbnail").asText() : "");
            model.addAttribute("url", url);
        } catch (Exception e) {
            if (type.startsWith("insta") || type.equals("photo") || type.equals("dp") || type.equals("story")) {
                try {
                    Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
                    String og = doc.selectFirst("meta[property=og:image]") != null ? doc.selectFirst("meta[property=og:image]").attr("content") : null;
                    model.addAttribute("thumbnail", og);
                } catch (IOException ex) {
                    model.addAttribute("error", "Failed to fetch info: " + e.getMessage());
                }
            } else {
                model.addAttribute("error", "Failed to fetch info: " + e.getMessage());
            }
        }
        return "index";
    }

    @GetMapping("/download-file")
    public ResponseEntity<?> downloadFile(@RequestParam String url, @RequestParam(required = false) String formatId) {
        try {
            Path file = ytDlpService.downloadToTempFile(url, formatId);

            // Virus scan
            boolean clean = true;
            try {
                clean = virusScanService.isFileClean(file);
            } catch (Exception e) {
                // If scanning fails, delete file and return error
                Files.deleteIfExists(file);
                return ResponseEntity.status(500).body("Virus scan failed: " + e.getMessage());
            }
            if (!clean) {
                Files.deleteIfExists(file);
                return ResponseEntity.status(403).body("File is infected and has been removed.");
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file.toFile()));
            String filename = file.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(file.toFile().length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Download failed: " + e.getMessage());
        }
    }

    private String normalizeUrl(String n) {
        if (n == null) return "";
        String t = n.trim();
        if (!t.startsWith("http")) t = "https://" + t;
        return t;
    }
}
