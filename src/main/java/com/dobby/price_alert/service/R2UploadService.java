package com.dobby.price_alert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class R2UploadService {

    private final S3Client s3Client;

    @Value("${cloudflare.bucket}")
    private String bucket;


    public R2UploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    public void upload(Path file, String objectKey) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("application/json")
                .build();

        s3Client.putObject(request, file);

        System.out.println("Uploaded " + objectKey);
    }


    /**
     * Downloads a file from R2 to a temporary local file.
     *
     * Returns null if the file does not exist (NoSuchKeyException).
     * Throws RuntimeException for any other failure.
     */
    public Path download(String objectKey) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {

            Path tempFile = Files.createTempFile("r2-download-", ".json");

            // Use OutputStream instead of ResponseTransformer.toFile()
            // — avoids Windows file locking issues with SDK
            try (var outputStream = Files.newOutputStream(tempFile)) {
                s3Client.getObject(request, ResponseTransformer.toOutputStream(outputStream));
            }

            return tempFile;

        } catch (NoSuchKeyException e) {
            return null;

        } catch (S3Exception e) {
            if (e.statusCode() == 404 || "NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
                return null;
            }
            throw new RuntimeException("Failed to download " + objectKey, e);

        } catch (Exception e) {
            throw new RuntimeException("Failed to download " + objectKey, e);
        }
    }
}