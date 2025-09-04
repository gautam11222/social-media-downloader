package com.example.downloader.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Service
public class VirusScanService {

    /**
     * Scans the given file path using `clamscan` command-line tool.
     * Requires clamav (clamscan) installed in the container/host.
     *
     * Returns true if clean, false if infected.
     * Throws Exception on scan errors.
     */
    public boolean isFileClean(Path file) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("clamscan", "--no-summary", file.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append("\n");
            }
            int code = p.waitFor();
            // clamscan exit codes: 0 = no virus, 1 = virus found, 2 = error
            if (code == 0) return true;
            if (code == 1) return false;
            throw new Exception("clamscan error: exit code=" + code + " output=" + out.toString());
        }
    }
}
