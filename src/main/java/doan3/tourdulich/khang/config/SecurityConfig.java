package doan3.tourdulich.khang.config;

import doan3.tourdulich.khang.service.CustomOAuth2UserService;
import doan3.tourdulich.khang.service.userService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private final userService userService;
    @Autowired
    private CustomOAuth2UserService oauth2UserService;

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public DefaultSecurityFilterChain SecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(successHandler())
                        .authorizationEndpoint(authorization -> authorization
                            .baseUri("/oauth2/authorization")
                        )
                        .redirectionEndpoint(redirection -> redirection
                            .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                            .userService(oauth2UserService)
                        )
                )
                .formLogin(httpForm -> {
                    httpForm
                        .loginPage("/login").permitAll()
                            .successHandler(successHandler());
                }).logout(httpLogout -> httpLogout
                        .logoutUrl("/logout") // URL để xử lý đăng xuất
                        .logoutSuccessUrl("/Client/Home") // Chuyển hướng sau khi đăng xuất
                        .permitAll()
                )
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/Client/**",
                            "/login",
                            "/oauth2/authorization/code/google",
                            "/oauth2/**",
                            "/register",
                            "/forgotPassword/**",
                            "/css/**",
                            "/js/**",
                            "/img/**",
                            "/uploads/**",
                            "/submitOrder",
                            "/payment",
                            "/vnpay-payment",
                            "/api/chatbot/**"
                    ).permitAll();
                    registry.requestMatchers("/admin/**").hasRole("ADMIN");
                    registry.anyRequest().authenticated();
                })
                .build();
    }
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new CustomAuthenticationSuccessHandler();
    }
}
