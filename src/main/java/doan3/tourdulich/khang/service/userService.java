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

import java.time.LocalDate;
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

//    public List<users> getAllUsers() {
//        return userRepository.findByRole("ROLE_USER");
//    }
//    public List<users> getAllUploaders() {
//        return userRepository.findByRole("ROLE_UPLOADER");
//    }
//
//    public void deleteById(String userId) {
//        users user = userRepository.findByUserId(userId);
//        if (user != null) {
//            if (!user.getUserPicUrl().equals("/UserProfilePics/UserDefaultAvatar.png")) {
//                String picUrl = user.getUserPicUrl();
//                String newPicUrl = picUrl.replace("/UserProfilePics/", "src/main/resources/static/UserProfilePics/");
//                File oldPicFile = new File(newPicUrl);
//                    oldPicFile.delete();
//            }
//            userRepository.deleteByUserId(userId);
//        }
//    }
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
//
//    public void updatePicFile(MultipartFile coverImage, String userId) throws IOException {
//        File picDir = new File(PIC_DIR);
//        if (!picDir.exists()) {
//            picDir.mkdirs();
//        }
//        users user = userRepository.findByUserId(userId);
//        if (user != null) {
//            String picUrl = user.getUserPicUrl();
//            String newPicUrl = picUrl.replace("/UserProfilePics/", "src/main/resources/static/UserProfilePics/");
//            File oldPicFile = new File(newPicUrl);
//            if (oldPicFile.exists()&&!user.getUserPicUrl().equals("/UserProfilePics/UserDefaultAvatar.png")) {
//                oldPicFile.delete();
//            }
//            String originalName = coverImage.getOriginalFilename();
//            String picName = originalName;
//            String fileExtension = "";
//
//            // Extract file extension if present
//            int lastDotIndex = originalName.lastIndexOf(".");
//            if (lastDotIndex > 0) {
//                fileExtension = originalName.substring(lastDotIndex); // Get file extension
//                picName = originalName.substring(0, lastDotIndex); // File name without extension
//            }
//
//            File picFile = new File(PIC_DIR + originalName);
//            int counter = 1; // Start with 1 for duplicate files
//
//            // Generate a unique name by appending a number if the file exists
//            while (picFile.exists()) {
//                picFile = new File(PIC_DIR + picName + "_" + counter + fileExtension);
//                counter++;
//            }
//
//            // Save the file
//            try (FileOutputStream fos = new FileOutputStream(picFile)) {
//                fos.write(coverImage.getBytes());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            String newPicUrl2 = "/UserProfilePics/" + picFile.getName();
//            user.setUserPicUrl(newPicUrl2);
//            userRepository.save(user);
//        }
//    }
//    public users findByUserId(String userId) {
//        return userRepository.findByUserId(userId);
//    }
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
