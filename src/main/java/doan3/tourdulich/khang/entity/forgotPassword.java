package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class forgotPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "fp_seq", sequenceName = "fp_seq", allocationSize = 1)
    public Integer fpId;
    @Column(nullable = false)
    private Integer otp;
    @Column(nullable = false)
    private Date expiryDate;
    @OneToOne
    private users user;
}
