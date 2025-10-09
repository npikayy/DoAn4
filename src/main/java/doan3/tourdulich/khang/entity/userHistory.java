package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class userHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "history_seq", sequenceName = "history_seq", allocationSize = 1)
    private Integer history_id;
    private String region;
    @ManyToOne
    private users user;
    private LocalDateTime timeStamp;
}
