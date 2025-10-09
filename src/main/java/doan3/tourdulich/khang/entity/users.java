package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String user_id;
    String full_name;
    String username;
    String phone_number;
    String address;
    LocalDate date_of_birth;
    String gender;
    LocalDate created_at;
    String password;
    String email;
    String role;
    String provider;
    @OneToOne(mappedBy = "user")
    private forgotPassword forgotPassword;
    @OneToMany(mappedBy = "user")
    private List<userHistory> userHistoryList;
}
