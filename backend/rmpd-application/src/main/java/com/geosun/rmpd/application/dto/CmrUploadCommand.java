package com.geosun.rmpd.application.dto;

public record CmrUploadCommand(byte[] content, String originalFilename, String mimeType, long sizeBytes) {}
