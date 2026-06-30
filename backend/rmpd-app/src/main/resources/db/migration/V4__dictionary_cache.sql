CREATE TABLE dictionary_cache (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(50)  NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    label_pl    VARCHAR(255) NOT NULL,
    label_en    VARCHAR(255) NOT NULL,
    synced_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dictionary_cache_type_code UNIQUE (dict_type, code)
);

INSERT INTO dictionary_cache (dict_type, code, label_pl, label_en) VALUES
('country', 'PL', 'Polska', 'Poland'),
('country', 'UA', 'Ukraina', 'Ukraine'),
('country', 'DE', 'Niemcy', 'Germany'),
('country', 'LT', 'Litwa', 'Lithuania'),
('country', 'CZ', 'Czechy', 'Czech Republic'),
('country', 'SK', 'Słowacja', 'Slovakia'),
('country', 'BY', 'Białoruś', 'Belarus'),
('country', 'RO', 'Rumunia', 'Romania'),
('country', 'HU', 'Węgry', 'Hungary'),
('country', 'FR', 'Francja', 'France');
