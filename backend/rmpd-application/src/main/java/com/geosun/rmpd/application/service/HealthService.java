package com.geosun.rmpd.application.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public String status() {
        return "UP";
    }
}
