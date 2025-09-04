package com.example.downloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class YtDlpService {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode getInfo(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-j", url);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            p.waitFor();
            if (sb.length() == 0) {
                throw new IOException("yt-dlp returned empty output");
            }
            return mapper.readTree(sb.toString());
        }
    }

    public Path downloadToTempFile(String url, String formatId) throws IOException, InterruptedException {
        String id = UUID.randomUUID().toString();
        Path tmpDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "socialdownloader"));
        String outputPattern = tmpDir.resolve(id + ".%(ext)s").toString();
        ProcessBuilder pb;
        if (formatId == null || formatId.isBlank()) {
            pb = new ProcessBuilder("yt-dlp", "-f", "best", "-o", outputPattern, url);
        } else {
            pb = new ProcessBuilder("yt-dlp", "-f", formatId, "-o", outputPattern, url);
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // consume output (yt-dlp progress)
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("yt-dlp exit code: " + code);
        }
        // find the downloaded file (match id.*)
        try (var stream = Files.list(tmpDir)) {
            var optional = stream.filter(path -> path.getFileName().toString().startsWith(id + ".")).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        throw new IOException("Downloaded file not found");
    }
}
