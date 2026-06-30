CREATE TABLE declaration (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id          BIGINT       NOT NULL,
    created_by          BIGINT       NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    transport_type      VARCHAR(30),
    cmr_number          VARCHAR(50),
    route_start_date    DATE,
    route_end_date      DATE,
    loading_country     VARCHAR(2),
    unloading_country   VARCHAR(2),
    vehicle_id          BIGINT,
    permit_id           BIGINT,
    sender_party_id     BIGINT,
    receiver_party_id   BIGINT,
    route_points_json   JSON,
    puesc_sys_ref       VARCHAR(100),
    reference_number    VARCHAR(100),
    comment_text        TEXT,
    xsd_version         VARCHAR(30)  NOT NULL DEFAULT 'RMPD_v20.11.2024',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_declaration_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT fk_declaration_user FOREIGN KEY (created_by) REFERENCES rmpd_user (id),
    CONSTRAINT fk_declaration_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle (id),
    CONSTRAINT fk_declaration_permit FOREIGN KEY (permit_id) REFERENCES permit (id),
    CONSTRAINT fk_declaration_sender FOREIGN KEY (sender_party_id) REFERENCES party (id),
    CONSTRAINT fk_declaration_receiver FOREIGN KEY (receiver_party_id) REFERENCES party (id)
);

CREATE INDEX idx_declaration_carrier_status ON declaration (carrier_id, status);
CREATE INDEX idx_declaration_puesc_sys_ref ON declaration (puesc_sys_ref);

CREATE TABLE declaration_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    declaration_id  BIGINT       NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload_json    JSON,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_declaration_event_declaration FOREIGN KEY (declaration_id) REFERENCES declaration (id)
);

CREATE INDEX idx_declaration_event_declaration ON declaration_event (declaration_id, created_at);
