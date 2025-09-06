package com.example.downloader.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class InfoCacheService {

    private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> cache = new ConcurrentHashMap<>();

    // Cache expiry in milliseconds (e.g. 10 minutes)
    private final long expiryMillis = 10 * 60 * 1000;

    private final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();

    private final YtDlpService ytDlpService = new YtDlpService();

    public CompletableFuture<JsonNode> getInfoAsync(String url) {
        Long ts = timestamps.get(url);
        long now = System.currentTimeMillis();

        // Remove expired cache entry
        if (ts == null || now - ts > expiryMillis) {
            cache.remove(url);
            timestamps.remove(url);
        }

        // Compute if absent: start fetching info async
        CompletableFuture<JsonNode> future = cache.computeIfAbsent(url, u -> CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode info = ytDlpService.fetchInfo(u);
                timestamps.put(u, System.currentTimeMillis());
                return info;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }));

        return future;
    }

    public boolean isReady(String url) {
        return cache.containsKey(url) && cache.get(url).isDone() && !cache.get(url).isCompletedExceptionally();
    }

    public JsonNode getIfReady(String url) throws ExecutionException, InterruptedException {
        if (!isReady(url)) return null;
        return cache.get(url).get();
    }
    public void clear(String url) {
        cache.remove(url);
        timestamps.remove(url);
    }

    public void put(String url, com.fasterxml.jackson.databind.JsonNode info) {
        cache.put(url, CompletableFuture.completedFuture(info));
        timestamps.put(url, System.currentTimeMillis());
    }

    public com.fasterxml.jackson.databind.JsonNode get(String url) throws ExecutionException, InterruptedException {
        return cache.get(url).get();
    }

}
