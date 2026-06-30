package com.geosun.rmpd.api.controller;

import com.geosun.rmpd.api.audit.Audited;
import com.geosun.rmpd.application.dto.CmrBatchResultDto;
import com.geosun.rmpd.application.service.CmrBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cmr")
@Tag(name = "CMR Batch", description = "Пакетне завантаження CMR")
public class CmrBatchController {

    private final CmrBatchService cmrBatchService;

    public CmrBatchController(CmrBatchService cmrBatchService) {
        this.cmrBatchService = cmrBatchService;
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    @Audited(action = "CMR_BATCH_UPLOAD", resourceType = "cmr")
    @Operation(summary = "Пакетне завантаження CMR — черновик на кожен файл")
    public ResponseEntity<CmrBatchResultDto> batchUpload(@RequestPart("files") List<MultipartFile> files)
            throws IOException {
        return ResponseEntity.ok(cmrBatchService.uploadBatch(files));
    }
}
