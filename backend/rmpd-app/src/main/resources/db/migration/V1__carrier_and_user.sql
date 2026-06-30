CREATE TABLE carrier (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_type      VARCHAR(20)  NOT NULL,
    id_number    VARCHAR(50)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    address_json JSON,
    email        VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rmpd_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id    BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rmpd_user_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT uk_rmpd_user_email UNIQUE (email)
);

CREATE INDEX idx_rmpd_user_carrier ON rmpd_user (carrier_id);
