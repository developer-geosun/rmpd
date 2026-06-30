package com.geosun.rmpd.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.LoginCommand;
import com.geosun.rmpd.domain.enums.UserRole;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        userRepository.deleteAll();
        carrierRepository.deleteAll();

        Carrier carrier = new Carrier();
        carrier.setIdType("INNY");
        carrier.setIdNumber("12345678");
        carrier.setName("Test Carrier");
        carrier.setEmail("carrier@test.local");
        carrier.setAddressJson(
                "{\"country\":\"UA\",\"city\":\"Kyiv\",\"postalCode\":\"01001\",\"street\":\"Test\",\"buildingNumber\":\"1\",\"unitNumber\":\"BRAK\"}");
        carrier = carrierRepository.save(carrier);

        User user = new User();
        user.setCarrier(carrier);
        user.setEmail("admin@test.local");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
    }

    @Test
    void loginReturnsTokens() throws Exception {
        LoginCommand command = new LoginCommand("admin@test.local", "secret");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@test.local"));
    }
}
