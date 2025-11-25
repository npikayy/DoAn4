package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "point_history")
public class pointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private users user;

    private int pointsChange; // Renamed from pointsAdded

    private String description; // New field

    @Enumerated(EnumType.STRING) // New field
    private PointHistoryType type;

    private LocalDateTime creationDate;
}
