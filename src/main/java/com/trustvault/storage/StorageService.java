package com.trustvault.storage;

import java.io.InputStream;

public interface StorageService {
    /**
     * Upload raw binary content to private bucket.
     * @param objectKey server-generated unique object key
     * @param content byte array content
     * @param mimeType MIME type
     */
    void uploadObject(String objectKey, byte[] content, String mimeType);

    /**
     * Download binary stream of private object.
     * @param objectKey storage object key
     * @return InputStream of object bytes
     */
    InputStream downloadObject(String objectKey);

    /**
     * Delete object from storage (e.g. cleanup on failure or purge).
     * @param objectKey storage object key
     */
    void deleteObject(String objectKey);
}
