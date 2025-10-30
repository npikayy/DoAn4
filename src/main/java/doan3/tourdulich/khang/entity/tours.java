package doan3.tourdulich.khang.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class tours {
    @Id
    private String tour_id;
    private String tour_name;
    private String tour_description;
    private String tour_type;
    @OneToMany(mappedBy = "tour")
    private List<tour_schedules> tourSchedules;
    @OneToMany(mappedBy = "tour")
    private Set<tour_pictures> tourPictures;
    private String tour_region;
private Boolean is_abroad;
    private String tour_start_location;
    private String tour_end_location;
    private String tour_transportation;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @OneToMany(mappedBy = "tour")
    private List<tour_start_date> tour_start_date;
    @OneToMany(mappedBy = "tour")
    private List<tour_bookings> tour_bookings;

    @ManyToOne()
    private KhuyenMai discount_promotion;

    private String special_offer;
    private Integer tour_duration;
    private Integer tour_adult_price;
    private Integer tour_child_price;
    private Integer tour_infant_price;
    private Integer tour_max_number_of_people;
    private Float tour_rating;
    LocalDate created_at;
}
