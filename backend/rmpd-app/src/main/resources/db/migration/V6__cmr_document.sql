CREATE TABLE cmr_document (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    declaration_id          BIGINT       NOT NULL,
    file_path               VARCHAR(500) NOT NULL,
    original_filename       VARCHAR(255) NOT NULL,
    mime_type               VARCHAR(50)  NOT NULL,
    file_size_bytes         BIGINT       NOT NULL,
    ocr_raw_text            LONGTEXT,
    extracted_fields_json   JSON,
    applied_at              TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cmr_document_declaration FOREIGN KEY (declaration_id) REFERENCES declaration (id)
);

CREATE INDEX idx_cmr_document_declaration ON cmr_document (declaration_id);
