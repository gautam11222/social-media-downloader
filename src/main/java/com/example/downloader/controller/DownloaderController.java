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

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("downloads", repository.findAll());
        return "index";
    }

    @PostMapping("/download")
    public String download(@RequestParam String url, Model model) {
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "best", url, "-o", "downloads/%(title)s.%(ext)s");
            pb.redirectErrorStream(true); // merge stderr + stdout
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // log progress
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                DownloadHistory history = new DownloadHistory();
                history.setUrl(url);
                history.setDownloadedAt(LocalDateTime.now());
                repository.save(history);

                model.addAttribute("success", "Download completed successfully!");
            } else {
                model.addAttribute("error", "Download failed. yt-dlp exited with code " + exitCode);
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
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--dump-json", "-f", "best", url);
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
                result.put("status", "error");
                result.put("message", "yt-dlp failed. Logs: " + errBuilder);
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Preview failed: " + e.getMessage());
        }
        return result;
    }

}
