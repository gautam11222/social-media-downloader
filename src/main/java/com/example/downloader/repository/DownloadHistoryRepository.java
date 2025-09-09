package com.example.downloader.repository;

import com.example.downloader.entity.DownloadHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadHistoryRepository extends JpaRepository<DownloadHistory, Long> {
}
