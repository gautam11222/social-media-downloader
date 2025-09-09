package com.example.downloader.controller;

import com.example.downloader.entity.DownloadHistory;
import com.example.downloader.repository.DownloadHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloaderController {

    @Autowired
    private DownloadHistoryRepository repository;

    @GetMapping("/")
    public String home(org.springframework.ui.Model model) {
        model.addAttribute("downloads", repository.findAll());
        return "index";
    }

    /**
     * Stream video directly to browser (GET endpoint)
     */
    @GetMapping("/download")
    public void download(@RequestParam String url, HttpServletResponse response) {
        try {
            response.setContentType("video/mp4");
            response.setHeader("Content-Disposition", "attachment; filename=video.mp4");

            // yt-dlp outputs video directly to stdout ("-o -")
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "best", "-o", "-", url);
            Process process = pb.start();

            try (var in = process.getInputStream(); var out = response.getOutputStream()) {
                in.transferTo(out);
            }

            process.waitFor();

            // Save history
            DownloadHistory history = new DownloadHistory();
            history.setUrl(url);
            history.setDownloadedAt(LocalDateTime.now());
            repository.save(history);

        } catch (Exception e) {
            try {
                response.setContentType("text/plain");
                response.getWriter().write("Download failed: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Get preview metadata for frontend
     */
    @ResponseBody
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--dump-json", "-f", "best", url);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            process.waitFor();

            result.put("status", "ok");
            result.put("data", jsonBuilder.toString());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}
