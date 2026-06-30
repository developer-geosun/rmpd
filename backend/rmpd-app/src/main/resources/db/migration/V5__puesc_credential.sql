CREATE TABLE puesc_credential (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id          BIGINT       NOT NULL,
    environment         VARCHAR(10)  NOT NULL DEFAULT 'test',
    username            VARCHAR(255) NOT NULL,
    password_encrypted  VARBINARY(512) NOT NULL,
    password_iv         VARBINARY(16)  NOT NULL,
    signing_cert_path   VARCHAR(500),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    last_test_at        TIMESTAMP,
    last_test_ok        BOOLEAN,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_puesc_credential_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT uq_puesc_credential_carrier_env UNIQUE (carrier_id, environment)
);
