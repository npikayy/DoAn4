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
public class tour_start_date {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "start_date_seq", sequenceName = "start_date_seq", allocationSize = 1)
    private Integer start_date_id;
    private LocalDate start_date;
    @ManyToOne
    private tours tour;
    private Integer guest_number;


}
