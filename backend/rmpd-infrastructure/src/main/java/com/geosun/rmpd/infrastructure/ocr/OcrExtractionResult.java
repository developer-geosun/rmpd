package com.geosun.rmpd.infrastructure.ocr;

import java.util.List;

public record OcrExtractionResult(String rawText, List<ExtractedField> fields) {}
