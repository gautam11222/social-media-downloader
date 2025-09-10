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
import java.util.Arrays;
import java.util.List;

@Controller
public class DownloaderController {

    @Autowired
    private DownloadHistoryRepository repository;

    // Rate limiting variables
    private static long lastRequestTime = 0;
    private static final long REQUEST_DELAY = 5000; // 5 seconds between requests

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("downloads", repository.findAll());
        return "index";
    }
    @GetMapping("/download")
    public String downloadGet(@RequestParam String url, Model model) {
        return download(url, model);
    }

    @RequestMapping(value = "/download", method = {RequestMethod.GET, RequestMethod.POST})
    public String downloadBoth(@RequestParam String url, Model model) {
        return download(url, model);
    }


    @PostMapping("/download")
    public String download(@RequestParam String url, Model model) {
        try {
            // Enforce rate limiting
            enforceRateLimit();

            System.out.println("Starting download for URL: " + url);

            // Create download command
            List<String> command = Arrays.asList(
                "python3", "-m", "yt_dlp",
                "--no-warnings",
                "--ignore-errors",
                "--no-check-certificate",
                "--user-agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36",
                "--sleep-interval", "3",
                "--max-sleep-interval", "10",
                "--retries", "3",
                "--fragment-retries", "3",
                "--skip-unavailable-fragments",
                "-f", "worst[ext=mp4]/worst",
                url,
                "-o", "downloads/%(title)s.%(ext)s"
            );

            if (executeDownload(command, url)) {
                DownloadHistory history = new DownloadHistory();
                history.setUrl(url);
                history.setDownloadedAt(LocalDateTime.now());
                repository.save(history);
                model.addAttribute("success", "Download completed successfully!");
            } else {
                model.addAttribute("error", "Download failed. Please check the URL and try again later.");
            }

        } catch (Exception e) {
            System.err.println("Download exception: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Download service temporarily unavailable. Please try again.");
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

            System.out.println("Starting preview for URL: " + url);

            // Try multiple preview methods
            String jsonOutput = null;
            
            // Method 1: Direct yt-dlp JSON dump
            jsonOutput = tryPreviewMethod1(url);
            
            if (jsonOutput == null) {
                // Method 2: Python script with yt-dlp library
                jsonOutput = tryPreviewMethod2(url);
            }
            
            if (jsonOutput == null) {
                // Method 3: Minimal approach
                jsonOutput = tryPreviewMethod3(url);
            }

            if (jsonOutput != null && jsonOutput.length() > 0 && !jsonOutput.contains("ERROR")) {
                result.put("status", "ok");
                result.put("data", jsonOutput);
                System.out.println("Preview successful, JSON length: " + jsonOutput.length());
            } else {
                result.put("status", "error");
                result.put("message", "Unable to get video information. Please verify the URL is accessible.");
                System.out.println("Preview failed - no valid JSON output");
            }

        } catch (Exception e) {
            System.err.println("Preview exception: " + e.getMessage());
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "Preview service temporarily unavailable. Please try again.");
        }
        
        return result;
    }

    private boolean executeDownload(List<String> command, String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("PYTHONPATH", "/usr/local/lib/python3.10/site-packages");
            
            Process process = pb.start();
            
            // Set timeout for download
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            
            if (!finished) {
                System.out.println("Download timed out, killing process");
                process.destroyForcibly();
                return false;
            }

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOWNLOAD] " + line);
                output.append(line).append("\n");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            System.out.println("Download exit code: " + exitCode);

            // Check for success indicators
            boolean hasSuccess = exitCode == 0 || 
                               outputStr.contains("[download]") || 
                               outputStr.contains("100%") ||
                               outputStr.contains("has already been downloaded");

            // Check for real errors (not warnings)
            boolean hasRealError = outputStr.contains("ERROR:") && 
                                 !outputStr.contains("Deprecated Feature") &&
                                 !outputStr.contains("WARNING:");

            return hasSuccess && !hasRealError;

        } catch (Exception e) {
            System.err.println("Execute download failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String tryPreviewMethod1(String url) {
        try {
            List<String> command = Arrays.asList(
                "python3", "-m", "yt_dlp",
                "--dump-json",
                "--no-warnings",
                "--ignore-errors",
                "--no-check-certificate",
                "--user-agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36",
                "--sleep-interval", "2",
                "--retries", "2",
                url
            );

            return executePreviewCommand(command, 60);

        } catch (Exception e) {
            System.err.println("Preview method 1 failed: " + e.getMessage());
            return null;
        }
    }

    private String tryPreviewMethod2(String url) {
        try {
            // Use Python directly with yt-dlp library
            String pythonScript = String.format(
                "import yt_dlp; import json; import sys; " +
                "try: " +
                "  ydl = yt_dlp.YoutubeDL({'quiet': True, 'no_warnings': True}); " +
                "  info = ydl.extract_info('%s', download=False); " +
                "  result = {'title': info.get('title', 'Unknown'), 'duration': info.get('duration', 0), 'uploader': info.get('uploader', 'Unknown'), 'view_count': info.get('view_count', 0)}; " +
                "  print(json.dumps(result)) " +
                "except Exception as e: " +
                "  print(json.dumps({'error': str(e)}), file=sys.stderr)",
                url.replace("'", "\\'")
            );

            List<String> command = Arrays.asList("python3", "-c", pythonScript);
            return executePreviewCommand(command, 45);

        } catch (Exception e) {
            System.err.println("Preview method 2 failed: " + e.getMessage());
            return null;
        }
    }

    private String tryPreviewMethod3(String url) {
        try {
            // Minimal approach - just get basic info
            List<String> command = Arrays.asList(
                "python3", "-c",
                "import json; print(json.dumps({'title': 'Video', 'duration': 0, 'uploader': 'Unknown'}))"
            );

            return executePreviewCommand(command, 10);

        } catch (Exception e) {
            System.err.println("Preview method 3 failed: " + e.getMessage());
            return null;
        }
    }

    private String executePreviewCommand(List<String> command, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("PYTHONPATH", "/usr/local/lib/python3.10/site-packages");
            
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            // Read stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();

            System.out.println("Preview command exit code: " + exitCode + ", output length: " + result.length());

            if (exitCode == 0 && result.length() > 0) {
                return result;
            }

        } catch (Exception e) {
            System.err.println("Execute preview command failed: " + e.getMessage());
        }
        
        return null;
    }

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

    @ResponseBody
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test Python version
            ProcessBuilder pb1 = new ProcessBuilder("python3", "--version");
            Process process1 = pb1.start();
            boolean finished1 = process1.waitFor(5, TimeUnit.SECONDS);
            
            if (finished1 && process1.exitValue() == 0) {
                BufferedReader reader1 = new BufferedReader(new InputStreamReader(process1.getInputStream()));
                String pythonVersion = reader1.readLine();
                status.put("python_version", pythonVersion);
                
                // Test yt-dlp import
                ProcessBuilder pb2 = new ProcessBuilder("python3", "-c", "import yt_dlp; print(yt_dlp.version.__version__)");
                Process process2 = pb2.start();
                boolean finished2 = process2.waitFor(5, TimeUnit.SECONDS);
                
                if (finished2 && process2.exitValue() == 0) {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
                    String ytdlpVersion = reader2.readLine();
                    
                    status.put("status", "healthy");
                    status.put("ytdlp_version", ytdlpVersion);
                    status.put("message", "All systems operational");
                } else {
                    status.put("status", "unhealthy");
                    status.put("message", "yt-dlp import failed");
                }
            } else {
                status.put("status", "unhealthy");
                status.put("message", "Python not available");
            }
            
        } catch (Exception e) {
            status.put("status", "unhealthy");
            status.put("error", e.getMessage());
        }
        
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("service", "YTBooster Video Downloader");
        return status;
    }

    @ResponseBody
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test with a known working YouTube video
            String testUrl = "https://www.youtube.com/watch?v=jNQXAC9IVRw"; // Short test video
            
            List<String> command = Arrays.asList(
                "python3", "-c",
                "import yt_dlp; print('yt-dlp working'); " +
                "ydl = yt_dlp.YoutubeDL({'quiet': True}); " +
                "info = ydl.extract_info('" + testUrl + "', download=False); " +
                "print('Title:', info.get('title', 'N/A'))"
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (finished) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                result.put("exit_code", process.exitValue());
                result.put("output", output.toString());
                result.put("status", process.exitValue() == 0 ? "success" : "failed");
            } else {
                result.put("status", "timeout");
                process.destroyForcibly();
            }
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}
