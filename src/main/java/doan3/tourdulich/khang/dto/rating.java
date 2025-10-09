package doan3.tourdulich.khang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class rating {
    private Integer booking_id;
    private String tour_id;
    private String user_id;
    private Integer rating;
    private String comment;
}
