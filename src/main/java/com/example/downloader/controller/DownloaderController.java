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
import java.util.concurrent.TimeUnit;

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
                "--no-warnings",
                "--ignore-errors",
                "--cookies-from-browser", "chrome",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "--sleep-interval", "2",
                "--max-sleep-interval", "10",
                "--socket-timeout", "30",
                "-f", "b", // Use 'b' instead of 'best' to suppress warning
                url, 
                "-o", "downloads/%(title)s.%(ext)s"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Set timeout for the process
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            
            if (!finished) {
                process.destroyForcibly();
                model.addAttribute("error", "Download timed out. Please try again with a shorter video.");
                model.addAttribute("downloads", repository.findAll());
                return "index";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line);
                output.append(line).append("\n");
            }
            
            int exitCode = process.exitValue();

            // Check for actual errors (not warnings)
            String outputStr = output.toString();
            boolean hasRealError = outputStr.contains("ERROR:") && 
                                 !outputStr.toLowerCase().contains("deprecated feature");

            if (exitCode == 0 || (!hasRealError && outputStr.contains("[download]"))) {
                DownloadHistory history = new DownloadHistory();
                history.setUrl(url);
                history.setDownloadedAt(LocalDateTime.now());
                repository.save(history);

                model.addAttribute("success", "Download completed successfully!");
            } else {
                // Handle specific errors with user-friendly messages
                if (outputStr.contains("429") || outputStr.contains("Too Many Requests")) {
                    model.addAttribute("error", "Rate limit exceeded. Please wait 5 minutes and try again.");
                } else if (outputStr.contains("Sign in to confirm")) {
                    model.addAttribute("error", "YouTube verification required. Please try again in 10 minutes.");
                } else if (outputStr.contains("Video unavailable")) {
                    model.addAttribute("error", "Video is unavailable or private. Please check the URL.");
                } else if (outputStr.contains("Unsupported URL")) {
                    model.addAttribute("error", "Unsupported video URL. Please use YouTube, Instagram, or TikTok links.");
                } else if (hasRealError) {
                    model.addAttribute("error", "Download failed. Please check the URL and try again.");
                } else {
                    // If no real error, treat as success
                    DownloadHistory history = new DownloadHistory();
                    history.setUrl(url);
                    history.setDownloadedAt(LocalDateTime.now());
                    repository.save(history);
                    model.addAttribute("success", "Download completed successfully!");
                }
            }
        } catch (Exception e) {
            System.err.println("Download exception: " + e.getMessage());
            e.printStackTrace();
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
                "--no-warnings",
                "--ignore-errors",
                "--cookies-from-browser", "chrome",
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "--sleep-interval", "1",
                "--socket-timeout", "30",
                "-f", "b",
                url
            );
            
            Process process = pb.start();

            // Set timeout for the process
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            
            if (!finished) {
                process.destroyForcibly();
                result.put("status", "error");
                result.put("message", "Preview timed out. Please try again.");
                return result;
            }

            // Capture stdout (JSON)
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = outReader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            // Capture stderr (warnings and errors)
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errBuilder = new StringBuilder();
            while ((line = errReader.readLine()) != null) {
                errBuilder.append(line).append("\n");
            }

            int exitCode = process.exitValue();
            String errorMessage = errBuilder.toString();
            String jsonOutput = jsonBuilder.toString();

            // Debug logging
            System.out.println("=== YT-DLP PREVIEW DEBUG ===");
            System.out.println("Exit Code: " + exitCode);
            System.out.println("JSON Length: " + jsonOutput.length());
            System.out.println("Error Message: " + errorMessage);
            System.out.println("===========================");

            // Check for real errors vs warnings
            boolean hasRealError = errorMessage.contains("ERROR:") && 
                                 !errorMessage.toLowerCase().contains("deprecated feature");

            if ((exitCode == 0 || !hasRealError) && jsonOutput.length() > 0) {
                result.put("status", "ok");
                result.put("data", jsonOutput);
            } else {
                // Handle specific errors with user-friendly messages
                if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                    result.put("status", "error");
                    result.put("message", "Rate limit exceeded. Please wait 5 minutes before trying again.");
                } else if (errorMessage.contains("Sign in to confirm")) {
                    result.put("status", "error");
                    result.put("message", "YouTube verification required. Please try again in 10 minutes.");
                } else if (errorMessage.contains("Video unavailable")) {
                    result.put("status", "error");
                    result.put("message", "Video is unavailable or private. Please check the URL.");
                } else if (errorMessage.contains("Unsupported URL")) {
                    result.put("status", "error");
                    result.put("message", "Unsupported video URL. Please use YouTube, Instagram, or TikTok links.");
                } else if (errorMessage.toLowerCase().contains("deprecated feature")) {
                    // Handle deprecation warning specifically
                    result.put("status", "error");
                    result.put("message", "Service updating. Please try again in a few moments.");
                } else if (hasRealError) {
                    result.put("status", "error");
                    result.put("message", "Preview failed. Please check the URL and try again.");
                } else {
                    // If we have JSON output but some warnings, treat as success
                    if (jsonOutput.length() > 0) {
                        result.put("status", "ok");
                        result.put("data", jsonOutput);
                    } else {
                        result.put("status", "error");
                        result.put("message", "Unable to get video information. Please try again.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Preview exception: " + e.getMessage());
            e.printStackTrace();
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
                long sleepTime = REQUEST_DELAY - timeSinceLastRequest;
                System.out.println("Rate limiting: sleeping for " + sleepTime + "ms");
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    // Health check endpoint for monitoring
    @ResponseBody
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Quick yt-dlp version check
            ProcessBuilder pb = new ProcessBuilder("python3", "-m", "yt_dlp", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (finished && process.exitValue() == 0) {
                status.put("status", "healthy");
                status.put("ytdlp", "available");
            } else {
                status.put("status", "unhealthy");
                status.put("ytdlp", "unavailable");
            }
        } catch (Exception e) {
            status.put("status", "unhealthy");
            status.put("error", e.getMessage());
        }
        
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }
}
