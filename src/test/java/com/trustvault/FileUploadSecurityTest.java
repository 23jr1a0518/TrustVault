package com.trustvault;

import com.trustvault.storage.FileValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

public class FileUploadSecurityTest {

    private final FileValidationService validator = new FileValidationService();

    @Test
    @DisplayName("Oversized uploads (> 20MB) are rejected immediately")
    void testOversizedFileRejected() {
        byte[] largeBytes = new byte[100];
        // Mock a file reporting size > 20MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.pdf", "application/pdf", largeBytes
        ) {
            @Override
            public long getSize() {
                return 25 * 1024 * 1024L; // 25 MB
            }
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validateUpload(largeFile));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed limit"));
    }

    @Test
    @DisplayName("Executable file extensions are rejected immediately")
    void testExecutableExtensionsRejected() {
        String[] dangerous = {"payload.exe", "script.sh", "run.bat", "exploit.js", "webshell.jsp", "trojan.ps1"};
        for (String filename : dangerous) {
            MockMultipartFile file = new MockMultipartFile("file", filename, "application/octet-stream", "test".getBytes());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validateUpload(file));
            assertTrue(ex.getMessage().contains("prohibited") || ex.getMessage().contains("not permitted"));
        }
    }

    @Test
    @DisplayName("Path traversal attempts in filenames are detected and blocked")
    void testPathTraversalBlocked() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "../../etc/passwd.pdf", "application/pdf", "%PDF-1.4 sample".getBytes()
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validateUpload(file));
        assertTrue(ex.getMessage().contains("traversal"));
    }

    @Test
    @DisplayName("Spoofed file extensions failing magic byte signature are rejected")
    void testMagicByteSpoofingRejected() {
        // Disguised as PDF but contains plain ASCII without %PDF header
        MockMultipartFile fakePdf = new MockMultipartFile(
            "file", "spoofed.pdf", "application/pdf", "MZ executable binary bytes...".getBytes()
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validateUpload(fakePdf));
        assertTrue(ex.getMessage().contains("signature mismatch"));
    }

    @Test
    @DisplayName("Legitimate files with valid magic bytes pass security validation")
    void testValidFileAccepted() {
        MockMultipartFile validPdf = new MockMultipartFile(
            "file", "annual_report.pdf", "application/pdf", "%PDF-1.7 sample document".getBytes()
        );
        assertDoesNotThrow(() -> validator.validateUpload(validPdf));

        MockMultipartFile validPng = new MockMultipartFile(
            "file", "diagram.png", "image/png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
        assertDoesNotThrow(() -> validator.validateUpload(validPng));
    }
}
