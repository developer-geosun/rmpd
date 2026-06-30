CREATE TABLE vehicle (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id           BIGINT       NOT NULL,
    registration_country VARCHAR(2)  NOT NULL,
    tractor_number       VARCHAR(20)  NOT NULL,
    trailer_number       VARCHAR(20),
    gps_device_id        VARCHAR(20)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id)
);

CREATE TABLE permit (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id    BIGINT       NOT NULL,
    permit_type   VARCHAR(50)  NOT NULL,
    permit_number VARCHAR(100) NOT NULL,
    valid_until   DATE         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_permit_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id)
);

CREATE TABLE party (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_id   BIGINT       NOT NULL,
    party_role   VARCHAR(20)  NOT NULL,
    id_type      VARCHAR(20)  NOT NULL,
    id_number    VARCHAR(50)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    address_json JSON,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_carrier FOREIGN KEY (carrier_id) REFERENCES carrier (id)
);

CREATE INDEX idx_vehicle_carrier ON vehicle (carrier_id);
CREATE INDEX idx_permit_carrier ON permit (carrier_id);
CREATE INDEX idx_party_carrier ON party (carrier_id);
