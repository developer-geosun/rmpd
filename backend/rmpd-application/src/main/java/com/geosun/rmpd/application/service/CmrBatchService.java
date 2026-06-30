package com.geosun.rmpd.application.service;

import com.geosun.rmpd.application.dto.CmrBatchItemResultDto;
import com.geosun.rmpd.application.dto.CmrBatchResultDto;
import com.geosun.rmpd.application.dto.CmrUploadCommand;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CmrBatchService {

    private static final int MAX_FILES = 20;

    private final DeclarationService declarationService;
    private final CmrService cmrService;

    public CmrBatchService(DeclarationService declarationService, CmrService cmrService) {
        this.declarationService = declarationService;
        this.cmrService = cmrService;
    }

    @Transactional
    public CmrBatchResultDto uploadBatch(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Потрібен хоча б один файл CMR");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Максимум " + MAX_FILES + " файлів за раз");
        }
        List<CmrBatchItemResultDto> items = new ArrayList<>();
        int succeeded = 0;
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cmr.bin";
            try {
                Long declarationId = declarationService.create().id();
                var doc = cmrService.upload(
                        declarationId,
                        new CmrUploadCommand(
                                file.getBytes(),
                                filename,
                                file.getContentType(),
                                file.getSize()));
                items.add(new CmrBatchItemResultDto(
                        declarationId, filename, true, null, doc.extractedFields().size()));
                succeeded++;
            } catch (Exception ex) {
                items.add(new CmrBatchItemResultDto(null, filename, false, ex.getMessage(), 0));
            }
        }
        return new CmrBatchResultDto(files.size(), succeeded, items);
    }
}
