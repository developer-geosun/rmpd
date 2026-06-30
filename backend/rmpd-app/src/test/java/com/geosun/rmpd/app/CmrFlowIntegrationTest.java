package com.geosun.rmpd.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.LoginCommand;
import com.geosun.rmpd.domain.enums.UserRole;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.infrastructure.persistence.AuditLogRepository;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.CmrDocumentRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationEventRepository;
import com.geosun.rmpd.infrastructure.persistence.DeclarationRepository;
import com.geosun.rmpd.infrastructure.persistence.PuescCredentialRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CmrFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private CmrDocumentRepository cmrDocumentRepository;

    @Autowired
    private DeclarationEventRepository declarationEventRepository;

    @Autowired
    private DeclarationRepository declarationRepository;

    @Autowired
    private PuescCredentialRepository puescCredentialRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;

    @BeforeEach
    void seed() throws Exception {
        cmrDocumentRepository.deleteAll();
        declarationEventRepository.deleteAll();
        declarationRepository.deleteAll();
        auditLogRepository.deleteAll();
        puescCredentialRepository.deleteAll();
        userRepository.deleteAll();
        carrierRepository.deleteAll();

        Carrier carrier = new Carrier();
        carrier.setIdType("INNY");
        carrier.setIdNumber("99887766");
        carrier.setName("CMR Carrier");
        carrier.setEmail("cmr@test.local");
        carrier.setAddressJson(
                "{\"country\":\"UA\",\"city\":\"Kyiv\",\"postalCode\":\"01001\",\"street\":\"Test\",\"buildingNumber\":\"1\",\"unitNumber\":\"BRAK\"}");
        carrier = carrierRepository.save(carrier);

        User user = new User();
        user.setCarrier(carrier);
        user.setEmail("cmr@test.local");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);

        accessToken = login();
    }

    @Test
    void uploadCmr_extractsAtLeastFiveFields() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/declarations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        long id = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        String cmrText = """
                CMR No: OCR-99999
                Sender: ТОВ Тест
                Receiver: Spedycja PL
                Place of loading: UA
                Place of delivery: PL
                Date: 20.06.2026
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "cmr.txt", "text/plain", cmrText.getBytes());

        mockMvc.perform(multipart("/api/v1/declarations/" + id + "/cmr/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractedFields.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.previewUrl").exists());
    }

    private String login() throws Exception {
        LoginCommand command = new LoginCommand("cmr@test.local", "secret");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
