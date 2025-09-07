package com.example.downloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class YtDlpService {
    
    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    
    public JsonNode fetchInfo(String url) throws IOException, InterruptedException {
        log.info("Fetching info for URL: {}", url);
        
        // Strategy 1: No cookies, basic extraction (FIXES CHROME ERROR)
        List<String> basicCmd = Arrays.asList(
            "yt-dlp", "-j", 
            "--no-warnings", 
            "--no-call-home",
            "--no-cookies-from-browser",  // IMPORTANT: Disable cookie extraction
            "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "--socket-timeout", "15",
            url
        );
        
        try {
            log.info("Trying basic extraction without cookies...");
            return executeCommand(basicCmd, 30);
        } catch (IOException e) {
            log.warn("Basic extraction failed: {}", e.getMessage());
        }
        
        // Strategy 2: Force IPv4 with extended timeout
        List<String> ipv4Cmd = Arrays.asList(
            "yt-dlp", "-j",
            "--no-warnings",
            "--no-call-home", 
            "--no-cookies-from-browser",
            "--force-ipv4",
            "--socket-timeout", "20",
            url
        );
        
        try {
            log.info("Trying IPv4 extraction...");
            return executeCommand(ipv4Cmd, 35);
        } catch (IOException e) {
            log.warn("IPv4 extraction failed: {}", e.getMessage());
        }
        
        // Strategy 3: Simple fallback
        List<String> simpleCmd = Arrays.asList(
            "yt-dlp", "-j",
            "--no-warnings",
            "--ignore-errors",
            "--no-cookies-from-browser",
            url
        );
        
        try {
            log.info("Trying simple extraction...");
            return executeCommand(simpleCmd, 25);
        } catch (IOException e) {
            log.warn("Simple extraction failed: {}", e.getMessage());
        }
        
        throw new IOException(buildUserFriendlyError(url));
    }
    
    public Path downloadFormat(String url, String format, Path outDir) throws IOException, InterruptedException {
        log.info("Downloading format {} for URL: {}", format, url);
        outDir.toFile().mkdirs();

        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(
            "yt-dlp",
            "-f", format,
            "-o", outDir.toAbsolutePath().toString() + "/%(title)s.%(ext)s",
            "--no-warnings",
            "--no-call-home",
            "--no-cookies-from-browser",  // IMPORTANT: Disable cookie extraction
            "--socket-timeout", "25",
            "--retries", "3"
        ));
        cmd.add(url);

        return executeDownload(cmd, outDir);
    }
    
    private JsonNode executeCommand(List<String> cmd, int timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (InputStream is = p.getInputStream()) {
            boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String output = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (!finished) {
                p.destroyForcibly();
                throw new IOException("Timeout after " + timeoutSeconds + " seconds");
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new IOException("Command failed: " + filterErrorMessage(output));
            }

            return mapper.readTree(output);
        }
    }
    
    private Path executeDownload(List<String> cmd, Path outDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (InputStream is = p.getInputStream()) {
            boolean finished = p.waitFor(10, TimeUnit.MINUTES);
            String output = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (!finished) {
                p.destroyForcibly();
                throw new IOException("Download timeout");
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new IOException("Download failed: " + filterErrorMessage(output));
            }

            return findLatestFile(outDir);
        }
    }
    
    private String filterErrorMessage(String output) {
        // Remove cookie-related errors from user messages
        if (output.contains("Chrome cookie database")) {
            return "Connection temporarily blocked - trying alternative method";
        }
        if (output.contains("Sign in to confirm")) {
            return "Content requires authentication - please try a different URL";
        }
        if (output.contains("Private video")) {
            return "This video is private and cannot be downloaded";
        }
        return output.length() > 150 ? output.substring(0, 150) + "..." : output;
    }
    
    private String buildUserFriendlyError(String url) {
        return String.format("""
            ❌ Unable to access content from %s
            
            This could be due to:
            • Content is private or requires login
            • Geographic restrictions
            • Rate limiting from the platform
            • Network connectivity issues
            
            💡 Try:
            • Different video URL
            • Wait 30 minutes and try again
            • Use VPN if content is geo-blocked
            • Check if the content is publicly accessible
            """, detectPlatform(url));
    }
    
    private String detectPlatform(String url) {
        if (url.contains("youtube.com") || url.contains("youtu.be")) return "YouTube";
        if (url.contains("instagram.com")) return "Instagram";
        if (url.contains("tiktok.com")) return "TikTok";
        return "this platform";
    }
    
    private Path findLatestFile(Path outDir) throws IOException {
        try (var ds = Files.newDirectoryStream(outDir)) {
            Path latest = null;
            long latestTime = 0;
            
            for (Path p : ds) {
                if (Files.isDirectory(p)) continue;
                
                long modTime = Files.getLastModifiedTime(p).toMillis();
                if (latest == null || modTime > latestTime) {
                    latest = p;
                    latestTime = modTime;
                }
            }
            
            if (latest == null) {
                throw new IOException("No downloaded file found in " + outDir);
            }
            
            return latest;
        }
    }

    public Path downloadThumbnail(JsonNode info, Path outDir) throws IOException {
        if (info == null || info.get("thumbnail") == null) {
            throw new IOException("No thumbnail available");
        }

        String thumbUrl = info.get("thumbnail").asText();
        URL u = new URL(thumbUrl);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        con.setConnectTimeout(10000);
        con.setReadTimeout(20000);

        try (InputStream is = con.getInputStream()) {
            String fileName = "thumb_" + System.currentTimeMillis() + ".jpg";
            Path out = outDir.resolve(fileName);
            Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
            return out;
        }
    }
}
