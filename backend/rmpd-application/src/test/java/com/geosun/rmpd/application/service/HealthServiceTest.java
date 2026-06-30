package com.geosun.rmpd.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HealthServiceTest {

    @Test
    void statusIsUp() {
        assertEquals("UP", new HealthService().status());
    }
}
