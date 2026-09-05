package com.sanedge.gateway.service;

import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface FileService {
    String createFileImage(FileUpload file, String filepath);
    void deleteFileImage(String filepath);
}
