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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Minimal yt-dlp wrapper service.
 * Requires 'yt-dlp' available in PATH.
 */
@Service
public class YtDlpService {
    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode fetchInfo(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-j", "--no-warnings", "--no-call-home", url);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (InputStream is = p.getInputStream()) {
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("yt-dlp info timeout");
            }
            int exit = p.exitValue();
            if (exit != 0) {
                throw new IOException("yt-dlp info failed: " + out);
            }
            return mapper.readTree(out);
        }
    }

    public Path downloadFormat(String url, String format, Path outDir) throws IOException, InterruptedException {
        outDir.toFile().mkdirs();
        Path tempOut = outDir.resolve("download_%(id)s.%(ext)s"); // template for yt-dlp
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("-f");
        cmd.add(format);
        cmd.add("-o");
        cmd.add(outDir.toAbsolutePath().toString() + "/%(title)s.%(ext)s");
        cmd.add("--no-warnings");
        cmd.add("--no-call-home");
        cmd.add(url);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (InputStream is = p.getInputStream()) {
            boolean finished = p.waitFor(10, TimeUnit.MINUTES);
            String out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("yt-dlp download timeout");
            }
            int exit = p.exitValue();
            if (exit != 0) {
                throw new IOException("yt-dlp download failed: " + out);
            }
        }
        // find downloaded file (simple heuristic: latest file in outDir)
        return findLatestFile(outDir);
    }

    private Path findLatestFile(Path outDir) throws IOException {
        try (var ds = Files.newDirectoryStream(outDir)) {
            Path latest = null;
            for (Path p : ds) {
                if (Files.isDirectory(p)) continue;
                if (latest == null || Files.getLastModifiedTime(p).toMillis() > Files.getLastModifiedTime(latest).toMillis()) {
                    latest = p;
                }
            }
            if (latest == null) throw new IOException("no output file found");
            return latest;
        }
    }

    public Path downloadThumbnail(JsonNode info, Path outDir) throws IOException {
        if (info == null || info.get("thumbnail") == null) throw new IOException("no thumbnail in info");
        String thumbUrl = info.get("thumbnail").asText();
        URL u = new URL(thumbUrl);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestProperty("User-Agent", "Java-yt-dlp-client");
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
