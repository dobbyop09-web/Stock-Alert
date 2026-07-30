package com.dobby.price_alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

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
}
