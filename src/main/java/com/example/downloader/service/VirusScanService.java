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
        log.info("Starting virus scan for file: {}", file.getFileName());
        
        ProcessBuilder pb = new ProcessBuilder("clamscan", "--no-summary", "--infected", file.toAbsolutePath().toString());
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                log.error("Virus scan timeout for file: {}", file.getFileName());
                throw new IOException("clamscan timeout");
            }

            int exit = p.exitValue();
            
            switch (exit) {
                case 0:
                    log.info("File is clean: {}", file.getFileName());
                    return true; // clean
                case 1:
                    log.warn("File is infected: {}", file.getFileName());
                    return false; // infected
                default:
                    log.error("Virus scan error for file: {}, exit code: {}", file.getFileName(), exit);
                    throw new IOException("clamscan error, exit=" + exit);
            }
            
        } catch (IOException e) {
            // If clamscan is not present, treat as clean but log warning
            log.warn("clamscan not available or I/O error; treating file as clean: {}", e.toString());
            return true;
        }
    }
    
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("clamscan", "--version");
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
            }
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
