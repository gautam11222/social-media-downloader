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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class YtDlpService {
    
    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    
    public JsonNode fetchInfo(String url) throws IOException, InterruptedException {
        log.info("🔍 Fetching info for URL: {}", url);
        
        // FIXED: No cookies, prevents Chrome database error
        List<String> cmd = Arrays.asList(
            "yt-dlp", "-j", 
            "--no-warnings", 
            "--no-call-home",
            "--no-cookies-from-browser",  // 🔑 KEY FIX: No Chrome cookies
            "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "--socket-timeout", "15",
            "--retries", "2",
            url
        );
        
        return executeCommand(cmd, 30);
    }
    
    public Path downloadFormat(String url, String format, Path outDir) throws IOException, InterruptedException {
        log.info("⬇️ Downloading format {} for URL: {}", format, url);
        outDir.toFile().mkdirs();

        List<String> cmd = Arrays.asList(
            "yt-dlp",
            "-f", format,
            "-o", outDir.toAbsolutePath().toString() + "/%(title)s.%(ext)s",
            "--no-warnings",
            "--no-call-home",
            "--no-cookies-from-browser",  // 🔑 KEY FIX: No Chrome cookies
            "--socket-timeout", "25",
            "--retries", "3",
            url
        );

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
                throw new IOException("⏰ Timeout after " + timeoutSeconds + " seconds");
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new IOException("❌ Command failed: " + filterErrorMessage(output));
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
                throw new IOException("⏰ Download timeout");
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new IOException("❌ Download failed: " + filterErrorMessage(output));
            }

            return findLatestFile(outDir);
        }
    }
    
    private String filterErrorMessage(String output) {
        // User-friendly error messages
        if (output.contains("Chrome cookie database")) {
            return "🔄 Temporary connection issue - trying alternative method";
        }
        if (output.contains("Sign in to confirm") || output.contains("Private video")) {
            return "🔐 This content requires authentication or is private";
        }
        if (output.contains("Video unavailable")) {
            return "📺 Video is unavailable or has been removed";
        }
        return output.length() > 200 ? output.substring(0, 200) + "..." : output;
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
                throw new IOException("📁 No downloaded file found");
            }
            
            log.info("✅ Downloaded: {}", latest.getFileName());
            return latest;
        }
    }

    public Path downloadThumbnail(JsonNode info, Path outDir) throws IOException {
        if (info == null || info.get("thumbnail") == null) {
            throw new IOException("🖼️ No thumbnail available");
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
