package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.userRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
public class userService implements UserDetailsService {
    static final String PIC_DIR = "src/main/resources/static/UserProfilePics/";
    @Autowired
    private userRepo userRepository;
    public void saveUser(String fullName, String username, String password, String email) {
        users user = users.builder()
                .full_name(fullName)
                .username(username)
                .password(password)
                .email(email)
                .role("ROLE_USER")
                .provider("LOCAL")
                .created_at(java.time.LocalDate.now())
                .build();
        userRepository.save(user);
    }

    public Page<users> getAllUsersExceptAdmin(Pageable pageable) {
        return (Page<users>) userRepository.findAllUsersExceptAdmin(pageable);
    }

    public List<users> getAllUsersExceptAdmin() {
        List<users> users = userRepository.findAllUsers();
        return users.stream()
                .filter(user -> !"admin@gmail.com".equals(user.getEmail()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<users> getAllUsers() {
        return userRepository.findAll();
    }
    public void updateUserInfo(String user_id,String full_name,String phone_number,String address,String gender,String date_of_birth) {
        Optional<users> user = userRepository.findById(user_id);
        if (user.isPresent()) {
            users updatedUser = user.get();
            updatedUser.setFull_name(full_name);
            updatedUser.setPhone_number(phone_number);
            updatedUser.setAddress(address);
            updatedUser.setGender(gender);
            if (!date_of_birth.isEmpty()){
                updatedUser.setDate_of_birth(LocalDate.parse(date_of_birth));
            }
            userRepository.save(updatedUser);
        }
    }

    public Optional<users> findByUserId(String userId) {
        return userRepository.findById(userId);
    }

    public users findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        users user = userRepository.findByUsername(username);
        if (user == null) {
            log.error("Login failed - User not found: {}", username);
            throw new UsernameNotFoundException("Invalid username or password");
        }

        log.info("User logged in successfully: {}", username);
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
