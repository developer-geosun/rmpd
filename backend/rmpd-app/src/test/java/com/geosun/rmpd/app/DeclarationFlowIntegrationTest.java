package com.geosun.rmpd.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosun.rmpd.application.dto.DeclarationUpsertDto;
import com.geosun.rmpd.application.dto.LoginCommand;
import com.geosun.rmpd.application.dto.PuescCredentialUpsertDto;
import com.geosun.rmpd.domain.enums.PartyRole;
import com.geosun.rmpd.domain.enums.PuescEnvironment;
import com.geosun.rmpd.domain.enums.TransportType;
import com.geosun.rmpd.domain.enums.UserRole;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.Party;
import com.geosun.rmpd.domain.model.Permit;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.domain.model.Vehicle;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.PartyRepository;
import com.geosun.rmpd.infrastructure.persistence.PermitRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import com.geosun.rmpd.infrastructure.persistence.VehicleRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeclarationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PermitRepository permitRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private long vehicleId;
    private long permitId;
    private long senderId;
    private long receiverId;

    @BeforeEach
    void seed() throws Exception {
        vehicleRepository.deleteAll();
        permitRepository.deleteAll();
        partyRepository.deleteAll();
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

        Vehicle vehicle = new Vehicle();
        vehicle.setCarrier(carrier);
        vehicle.setRegistrationCountry("UA");
        vehicle.setTractorNumber("AA1234BB");
        vehicle.setGpsDeviceId("GPS-001");
        vehicle = vehicleRepository.save(vehicle);
        vehicleId = vehicle.getId();

        Permit permit = new Permit();
        permit.setCarrier(carrier);
        permit.setPermitType("EKMT");
        permit.setPermitNumber("PERMIT-001");
        permit.setValidUntil(LocalDate.now().plusYears(1));
        permit = permitRepository.save(permit);
        permitId = permit.getId();

        Party sender = new Party();
        sender.setCarrier(carrier);
        sender.setPartyRole(PartyRole.SENDER);
        sender.setIdType("INNY");
        sender.setIdNumber("SENDER-1");
        sender.setName("Sender Co");
        sender.setAddressJson(
                "{\"country\":\"UA\",\"city\":\"Kyiv\",\"postalCode\":\"01001\",\"street\":\"S\",\"buildingNumber\":\"1\",\"unitNumber\":\"BRAK\"}");
        sender = partyRepository.save(sender);
        senderId = sender.getId();

        Party receiver = new Party();
        receiver.setCarrier(carrier);
        receiver.setPartyRole(PartyRole.RECEIVER);
        receiver.setIdType("INNY");
        receiver.setIdNumber("RECEIVER-1");
        receiver.setName("Receiver Co");
        receiver.setAddressJson(
                "{\"country\":\"PL\",\"city\":\"Warsaw\",\"postalCode\":\"00-001\",\"street\":\"R\",\"buildingNumber\":\"2\",\"unitNumber\":\"BRAK\"}");
        receiver = partyRepository.save(receiver);
        receiverId = receiver.getId();

        accessToken = login();

        PuescCredentialUpsertDto cred = new PuescCredentialUpsertDto(
                PuescEnvironment.TEST, "admin@test.local", "puesc-test-pass", null);
        mockMvc.perform(put("/api/v1/settings/puesc")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cred)))
                .andExpect(status().isOk());
    }

    @Test
    void submitDeclaration_mockPuesc_endToEnd() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/declarations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        long id = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        DeclarationUpsertDto upsert = new DeclarationUpsertDto(
                TransportType.LADEN,
                "CMR-TEST-001",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                "UA",
                "PL",
                vehicleId,
                permitId,
                senderId,
                receiverId,
                "[{\"type\":\"ENTRY\",\"name\":\"Swiecko\",\"country\":\"PL\"}]",
                null,
                true);

        mockMvc.perform(put("/api/v1/declarations/" + id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upsert)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/declarations/" + id + "/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(post("/api/v1/declarations/" + id + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sysRef").exists());

        mockMvc.perform(post("/api/v1/declarations/" + id + "/poll")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.referenceNumber").exists());

        MvcResult events = mockMvc.perform(get("/api/v1/declarations/" + id + "/events")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(events.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(arr.size() >= 4);
    }

    private String login() throws Exception {
        LoginCommand command = new LoginCommand("admin@test.local", "secret");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
