package com.example.downloader.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VirusScanService {

    private static final Logger log = LoggerFactory.getLogger(VirusScanService.class);

    public boolean scan(Path file) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("clamscan", "--no-summary", file.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("clamscan timeout");
            }
            int exit = p.exitValue();
            if (exit == 0) return true;    // clean
            if (exit == 1) return false;   // infected
            throw new IOException("clamscan error, exit=" + exit);
        } catch (IOException e) {
            // If clamscan is not present, treat as clean but allow operator to notice via logs.
            log.warn("clamscan not available or I/O error; treating file as clean for now: {}", e.toString());
            return true;
        }
    }
}
