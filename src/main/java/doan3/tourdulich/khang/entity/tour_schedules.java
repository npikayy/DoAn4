package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class tour_schedules {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "schedule_seq", sequenceName = "schedule_seq", allocationSize = 1)
    private Integer schedule_id;
    @ManyToOne
    private tours tour;
    private Integer day;
    private String title;
    private String body;
    private String image_url;
}
