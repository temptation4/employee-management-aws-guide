package com.employeemgmt.employeeservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    String uploadFile(MultipartFile file, String objectKey);

    String generatePreSignedUrl(String objectKey);

    void deleteFile(String objectKey);
}
