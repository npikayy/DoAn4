package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class tour_ratings {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "rating_seq", sequenceName = "rating_seq", allocationSize = 1)
    private Integer rating_id;
    @ManyToOne
    private tours tour;
    @ManyToOne
    private users user;
    private Integer rating;
    private LocalDate rating_date;
    private String comment;
}
