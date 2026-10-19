package com.vyroflix.identity.seeder;

import com.vyroflix.common.util.Constants;
import com.vyroflix.identity.entity.User;
import com.vyroflix.identity.entity.UserProfile;
import com.vyroflix.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {

    public static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String ADMIN_EMAIL = "admin@vyroflix.com";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsById(ADMIN_USER_ID)) {
            log.debug("Default admin user already exists");
            return;
        }

        log.info("Seeding default VyroFlix admin user: id={} email={}", ADMIN_USER_ID, ADMIN_EMAIL);

        User adminUser = User.builder()
                .id(ADMIN_USER_ID)
                .email(ADMIN_EMAIL)
                .isNew(true)
                .roles(new HashSet<>())
                .build();

        UserProfile adminProfile = UserProfile.builder()
                .user(adminUser)
                .displayName("VyroFlix Administrator")
                .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb")
                .build();

        adminUser.setProfile(adminProfile);
        adminUser.addRole("ROLE_" + Constants.ROLE_ADMIN);
        adminUser.addRole("ROLE_" + Constants.ROLE_USER);

        userRepository.save(adminUser);
        log.info("Default admin user successfully seeded");
    }
}
