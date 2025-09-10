package com.example.downloader.controller;

import com.example.downloader.entity.DownloadHistory;
import com.example.downloader.repository.DownloadHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloaderController {

    @Autowired
    private DownloadHistoryRepository repository;

    // Rate limiting variables
    private static long lastRequestTime = 0;
    private static final long REQUEST_DELAY = 3000; // 3 seconds between requests

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("downloads", repository.findAll());
        return "index";
    }

    @PostMapping("/download")
    public String download(@RequestParam String url, Model model) {
        try {
            // Enforce rate limiting
            enforceRateLimit();

            ProcessBuilder pb = new ProcessBuilder(
                "python3", "-m", "yt_dlp",
                "--cookies-from-browser", "chrome",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "--sleep-interval", "2",
                "--max-sleep-interval", "10", 
                "-f", "b", // Use 'b' instead of 'best' to suppress warning
                url, 
                "-o", "downloads/%(title)s.%(ext)s"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                DownloadHistory history = new DownloadHistory();
                history.setUrl(url);
                history.setDownloadedAt(LocalDateTime.now());
                repository.save(history);

                model.addAttribute("success", "Download completed successfully!");
            } else {
                // Handle specific errors
                String errorOutput = output.toString();
                if (errorOutput.contains("429") || errorOutput.contains("Too Many Requests")) {
                    model.addAttribute("error", "Rate limit exceeded. Please wait and try again later.");
                } else if (errorOutput.contains("Sign in to confirm")) {
                    model.addAttribute("error", "YouTube requires verification. Please try again in a few minutes.");
                } else {
                    model.addAttribute("error", "Download failed. yt-dlp exited with code " + exitCode);
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Download failed: " + e.getMessage());
        }
        model.addAttribute("downloads", repository.findAll());
        return "index";
    }

    @ResponseBody
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Enforce rate limiting
            enforceRateLimit();

            ProcessBuilder pb = new ProcessBuilder(
                "python3", "-m", "yt_dlp",
                "--dump-json",
                "--cookies-from-browser", "chrome",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "--sleep-interval", "2",
                "-f", "b", // Use 'b' instead of 'best'
                url
            );
            Process process = pb.start();

            // Capture stdout (JSON)
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = outReader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            // Capture stderr (warnings)
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errBuilder = new StringBuilder();
            while ((line = errReader.readLine()) != null) {
                errBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && jsonBuilder.length() > 0) {
                result.put("status", "ok");
                result.put("data", jsonBuilder.toString());
            } else {
                String errorMessage = errBuilder.toString();
                
                // Handle specific errors with user-friendly messages
                if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                    result.put("status", "error");
                    result.put("message", "Rate limit exceeded. Please wait before trying again.");
                } else if (errorMessage.contains("Sign in to confirm")) {
                    result.put("status", "error");
                    result.put("message", "YouTube verification required. Please try again later.");
                } else if (errorMessage.contains("Deprecated Feature")) {
                    result.put("status", "error");
                    result.put("message", "Service temporarily unavailable. Please try again.");
                } else {
                    result.put("status", "error");
                    result.put("message", "Preview failed: " + (errorMessage.isEmpty() ? "Unknown error" : errorMessage));
                }
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Preview failed: " + e.getMessage());
        }
        return result;
    }

    // Rate limiting method
    private synchronized void enforceRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < REQUEST_DELAY) {
            try {
                Thread.sleep(REQUEST_DELAY - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
