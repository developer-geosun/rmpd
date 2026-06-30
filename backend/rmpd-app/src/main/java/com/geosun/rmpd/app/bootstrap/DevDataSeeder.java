package com.geosun.rmpd.app.bootstrap;

import com.geosun.rmpd.domain.enums.UserRole;
import com.geosun.rmpd.domain.model.Carrier;
import com.geosun.rmpd.domain.model.User;
import com.geosun.rmpd.infrastructure.persistence.CarrierRepository;
import com.geosun.rmpd.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final CarrierRepository carrierRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            UserRepository userRepository,
            CarrierRepository carrierRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.carrierRepository = carrierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Carrier carrier = new Carrier();
        carrier.setIdType("INNY");
        carrier.setIdNumber("12345678");
        carrier.setName("Demo Carrier Sp. z o.o.");
        carrier.setEmail("carrier@demo.local");
        carrier.setAddressJson(
                "{\"country\":\"UA\",\"city\":\"Kyiv\",\"postalCode\":\"01001\",\"street\":\"Khreshchatyk\",\"buildingNumber\":\"1\",\"unitNumber\":\"BRAK\"}");
        carrier = carrierRepository.save(carrier);

        User admin = new User();
        admin.setCarrier(carrier);
        admin.setEmail("admin@demo.local");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        log.info("Створено demo-користувача: admin@demo.local / admin123");
    }
}
