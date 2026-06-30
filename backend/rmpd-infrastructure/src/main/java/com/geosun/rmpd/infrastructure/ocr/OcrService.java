package com.geosun.rmpd.infrastructure.ocr;

import java.util.List;

public interface OcrService {

    OcrExtractionResult extract(byte[] fileContent, String mimeType, String filename);
}
