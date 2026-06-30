CREATE TABLE audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id    BIGINT       NOT NULL,
    user_id       BIGINT       NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50)  NULL,
    resource_id   BIGINT       NULL,
    details_json  JSON         NULL,
    ip_address    VARCHAR(45)  NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES rmpd_user (id)
);

CREATE INDEX idx_audit_carrier_created ON audit_log (carrier_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_log (action);
