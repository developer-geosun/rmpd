package com.geosun.rmpd.infrastructure.ocr;

public interface OcrService {

    OcrExtractionResult extract(byte[] fileContent, String mimeType, String filename);
}
