package com.trustvault.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucketName;
    private final HttpClient httpClient;

    // In-memory fallback cache for local dev / tests when real Supabase key is placeholder
    private final Map<String, byte[]> localFallbackStore = new ConcurrentHashMap<>();

    public SupabaseStorageService(
            @Value("${supabase.url:https://nxjtgudsmeolyolrgdqu.supabase.co}") String supabaseUrl,
            @Value("${supabase.secret-key:}") String secretKey,
            @Value("${supabase.storage.bucket:trustvault-documents}") String bucketName) {
        this.supabaseUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.bucketName = bucketName;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    private boolean isConfigured() {
        return !secretKey.isBlank() && !secretKey.startsWith("your-") && !secretKey.equals("test");
    }

    @Override
    public void uploadObject(String objectKey, byte[] content, String mimeType) {
        if (!isConfigured()) {
            log.info("Supabase secret key not configured; using secure in-memory storage for key: {}", objectKey);
            localFallbackStore.put(objectKey, content);
            return;
        }

        try {
            String endpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectKey);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .header("Content-Type", mimeType)
                .header("x-upsert", "true")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Failed to upload object to Supabase. HTTP {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Supabase storage upload failed with status " + response.statusCode());
            }
            log.info("Successfully uploaded object to Supabase Storage: {} (size: {} bytes)", objectKey, content.length);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Network failure communicating with Supabase Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadObject(String objectKey) {
        if (!isConfigured()) {
            byte[] content = localFallbackStore.get(objectKey);
            if (content == null) {
                throw new IllegalArgumentException("Object not found in local fallback storage: " + objectKey);
            }
            return new ByteArrayInputStream(content);
        }

        try {
            // Retrieve via authenticated private object endpoint
            String endpoint = String.format("%s/storage/v1/object/authenticated/%s/%s", supabaseUrl, bucketName, objectKey);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 404) {
                throw new IllegalArgumentException("Storage object not found: " + objectKey);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to download object from Supabase. HTTP: " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Network failure downloading from Supabase Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        if (!isConfigured()) {
            localFallbackStore.remove(objectKey);
            return;
        }

        try {
            String endpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectKey);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .timeout(Duration.ofSeconds(15))
                .DELETE()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                log.warn("Supabase delete returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Error deleting object {} from Supabase Storage: {}", objectKey, e.getMessage());
        }
    }
}
