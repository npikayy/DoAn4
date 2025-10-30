package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.ToString;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class tour_pictures {
    @Id
    private String picture_id;
    @ManyToOne
    @ToString.Exclude
    private tours tour;
    private String picture_url;
}
