package doan3.tourdulich.khang.config;

import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.entity.users;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(userRepo userRepository){

        return args -> {
            if (userRepository.findByUsername("admin") == null){
                users user = users.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .role("ROLE_ADMIN")
                        .full_name("Quản trị viên")
                        .email("admin@admin.com")
                        .provider("LOCAL")
                        .created_at(LocalDate.now())
                        .build();
                userRepository.save(user);
                log.warn("admin user has been created with default password: admin");
            }
        };
    };
}
