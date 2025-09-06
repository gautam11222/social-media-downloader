package com.example.downloader.controller;

import com.example.downloader.service.InfoCacheService;
import com.example.downloader.service.YtDlpService;
import com.fasterxml.jackson.databind.JsonNode;
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

    @Autowired
    private YtDlpService ytDlpService;

    @Autowired
    private InfoCacheService infoCacheService;

    private final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"), "socialdownloader");

    @GetMapping({"/","/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/fetch")
    public String fetch(@RequestParam("url") String url, Model model) {
        try {
            JsonNode info = ytDlpService.fetchInfo(url);
            // cache for quick access
            infoCacheService.put(url, info);
            model.addAttribute("info", info);
            model.addAttribute("url", url);
            return "result";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "index";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestParam("url") String url, @RequestParam("format") String format) {
        try {
            Path outDir = TMP.resolve(String.valueOf(Math.abs(url.hashCode())));
            Files.createDirectories(outDir);
            Path file = ytDlpService.downloadFormat(url, format, outDir);
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
            String fname = file.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fname + "\"")
                    .contentLength(Files.size(file))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Download failed: " + e.getMessage());
        }
    }
}
