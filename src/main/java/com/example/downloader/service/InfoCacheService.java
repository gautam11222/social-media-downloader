package com.example.downloader.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class InfoCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(InfoCacheService.class);
    private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> cache = new ConcurrentHashMap<>();
    private final long expiryMillis = 10 * 60 * 1000; // 10 minutes
    private final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();
    
    @Autowired
    private YtDlpService ytDlpService;

    public CompletableFuture<JsonNode> getInfoAsync(String url) {
        Long ts = timestamps.get(url);
        long now = System.currentTimeMillis();

        // Remove expired cache entry
        if (ts != null && now - ts > expiryMillis) {
            cache.remove(url);
            timestamps.remove(url);
            log.debug("Expired cache entry removed for URL: {}", url);
        }

        // Compute if absent: start fetching info async
        CompletableFuture<JsonNode> future = cache.computeIfAbsent(url, u -> 
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Fetching info for URL: {}", u);
                    JsonNode info = ytDlpService.fetchInfo(u);
                    timestamps.put(u, System.currentTimeMillis());
                    return info;
                } catch (Exception e) {
                    log.error("Failed to fetch info for URL: {}", u, e);
                    throw new CompletionException(e);
                }
            }));

        return future;
    }

    public boolean isReady(String url) {
        CompletableFuture<JsonNode> future = cache.get(url);
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    public JsonNode getIfReady(String url) throws ExecutionException, InterruptedException {
        if (!isReady(url)) return null;
        return cache.get(url).get();
    }

    public void clear(String url) {
        cache.remove(url);
        timestamps.remove(url);
        log.debug("Cache cleared for URL: {}", url);
    }

    public void put(String url, JsonNode info) {
        cache.put(url, CompletableFuture.completedFuture(info));
        timestamps.put(url, System.currentTimeMillis());
        log.debug("Cache updated for URL: {}", url);
    }

    public JsonNode get(String url) throws ExecutionException, InterruptedException {
        CompletableFuture<JsonNode> future = cache.get(url);
        if (future == null) return null;
        return future.get();
    }
}
