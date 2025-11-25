package doan3.tourdulich.khang.service;


import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.userRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final userRepo userRepository;
    @Autowired
    private RankService rankService; // Changed from GradeService
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(userRepo userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        
        users user = userRepository.findByEmail(email);
        if (user == null) {
            user = new users();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(email));
            user.setRole("ROLE_USER");
            user.setCreated_at(LocalDate.now());
            user.setUsername(email);
            user.setFull_name(name);
            user.setProvider("GOOGLE");
            userRepository.save(user);
            rankService.createDefaultRank(user);
        }
        return oAuth2User;
    }
}