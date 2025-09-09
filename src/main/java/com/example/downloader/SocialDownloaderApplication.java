package com.example.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SocialDownloaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialDownloaderApplication.class, args);
    }
}
