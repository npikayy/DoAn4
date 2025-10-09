package doan3.tourdulich.khang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInfoRequest {
    String user_id;
    String full_name;
    String phone_number;
    String address;
    String gender;
    String date_of_birth;
}
