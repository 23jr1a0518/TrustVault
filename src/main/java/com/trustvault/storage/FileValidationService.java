package com.trustvault.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FileValidationService {

    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "png", "jpg", "jpeg", "txt", "md", "csv", "docx", "xlsx"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "sh", "bin", "js", "mjs", "html", "htm", "xhtml",
        "jsp", "jspx", "php", "py", "vbs", "ps1", "dll", "scr", "jar", "war", "ear"
    );

    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
        "pdf", "application/pdf",
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "txt", "text/plain",
        "md", "text/markdown",
        "csv", "text/csv",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 20MB");
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Detect path traversal attempts in raw input
        if (rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\") || rawFilename.contains("\0")) {
            throw new IllegalArgumentException("Path traversal or illegal characters detected in filename");
        }

        String cleanName = Paths.get(rawFilename).getFileName().toString();

        String extension = getFileExtension(cleanName);
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Executable or dangerous file types are prohibited: ." + extension);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File extension ." + extension + " is not permitted. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // Verify magic bytes (file signature)
        try (InputStream is = file.getInputStream()) {
            verifyFileSignature(is, extension);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file for signature validation: " + e.getMessage());
        }
    }

    private void verifyFileSignature(InputStream is, String extension) throws IOException {
        byte[] header = new byte[8];
        int bytesRead = is.read(header, 0, 8);
        if (bytesRead < 4) {
            if ("txt".equals(extension) || "csv".equals(extension) || "md".equals(extension)) {
                return; // Plain text files can be very small
            }
            throw new IllegalArgumentException("File header too short for type: ." + extension);
        }

        switch (extension) {
            case "pdf" -> {
                // %PDF = 0x25 0x50 0x44 0x46
                if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                    throw new IllegalArgumentException("File signature mismatch for PDF document");
                }
            }
            case "png" -> {
                // PNG magic: 89 50 4E 47
                if ((header[0] & 0xFF) != 0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
                    throw new IllegalArgumentException("File signature mismatch for PNG image");
                }
            }
            case "jpg", "jpeg" -> {
                // JPEG magic: FF D8 FF
                if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                    throw new IllegalArgumentException("File signature mismatch for JPEG image");
                }
            }
            case "docx", "xlsx" -> {
                // ZIP header: PK\x03\x04 = 0x50 0x4B 0x03 0x04
                if (header[0] != 0x50 || header[1] != 0x4B || header[2] != 0x03 || header[3] != 0x04) {
                    throw new IllegalArgumentException("File signature mismatch for OpenXML Office document");
                }
            }
            case "txt", "csv", "md" -> {
                // Text files: ensure no binary null bytes in first 512 bytes
                for (int i = 0; i < bytesRead; i++) {
                    if (header[i] == 0) {
                        throw new IllegalArgumentException("Illegal binary content detected in text file");
                    }
                }
            }
        }
    }

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_document";
        }
        String name = Paths.get(filename).getFileName().toString();
        // Replace non-alphanumeric (except dot, dash, underscore) with underscore
        String sanitized = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() > 100) {
            String ext = getFileExtension(sanitized);
            String base = sanitized.substring(0, Math.min(sanitized.length() - ext.length() - 1, 90));
            sanitized = base + "." + ext;
        }
        return sanitized;
    }

    public String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx == -1 || dotIdx == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }

    public String resolveMimeType(String filename, String reportedMime) {
        String ext = getFileExtension(filename);
        return EXTENSION_TO_MIME.getOrDefault(ext, reportedMime != null ? reportedMime : "application/octet-stream");
    }

    public String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm unavailable", e);
        }
    }
}
