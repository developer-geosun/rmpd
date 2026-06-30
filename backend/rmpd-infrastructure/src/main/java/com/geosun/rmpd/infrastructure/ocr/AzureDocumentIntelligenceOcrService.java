package com.geosun.rmpd.infrastructure.ocr;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentField;
import com.azure.ai.documentintelligence.models.DocumentFieldType;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rmpd.ocr.provider", havingValue = "azure")
public class AzureDocumentIntelligenceOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(AzureDocumentIntelligenceOcrService.class);

    private final DocumentIntelligenceClient client;
    private final CmrFieldMapper fieldMapper;

    public AzureDocumentIntelligenceOcrService(
            @Value("${rmpd.ocr.azure.endpoint}") String endpoint,
            @Value("${rmpd.ocr.azure.api-key}") String apiKey,
            CmrFieldMapper fieldMapper) {
        this.client = new DocumentIntelligenceClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
        this.fieldMapper = fieldMapper;
    }

    @Override
    public OcrExtractionResult extract(byte[] fileContent, String mimeType, String filename) {
        try {
            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(fileContent);
            SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller =
                    client.beginAnalyzeDocument("prebuilt-layout", options);
            AnalyzeResult result = poller.getFinalResult();

            StringBuilder raw = new StringBuilder();
            if (result.getContent() != null) {
                raw.append(result.getContent());
            }

            Map<String, String> keyValues = new LinkedHashMap<>();
            if (result.getDocuments() != null) {
                result.getDocuments().forEach(doc -> {
                    if (doc.getFields() == null) {
                        return;
                    }
                    for (Map.Entry<String, DocumentField> entry : doc.getFields().entrySet()) {
                        String value = fieldValue(entry.getValue());
                        if (value != null) {
                            keyValues.put(entry.getKey(), value);
                        }
                    }
                });
            }

            log.info("Azure OCR processed {} ({} bytes), fields={}", filename, fileContent.length, keyValues.size());
            return fieldMapper.mapFromKeyValues(keyValues, raw.toString());
        } catch (Exception ex) {
            log.warn("Azure OCR failed for {}: {}", filename, ex.getMessage());
            return fieldMapper.mapFromRawText("");
        }
    }

    private String fieldValue(DocumentField field) {
        if (field == null) {
            return null;
        }
        if (field.getType() == DocumentFieldType.STRING) {
            return field.getValueString();
        }
        if (field.getType() == DocumentFieldType.DATE) {
            return field.getValueDate() != null ? field.getValueDate().toString() : null;
        }
        if (field.getContent() != null) {
            return field.getContent();
        }
        return null;
    }
}
