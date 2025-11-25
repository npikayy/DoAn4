package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.ToString;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class tour_bookings {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "booking_seq", sequenceName = "booking_seq", allocationSize = 1)
    private Integer booking_id;
    @ManyToOne
    @ToString.Exclude
    private tours tour;
    private String user_id;
    private String user_full_name;
    private String user_email;
    private String user_phone_number;
    private String user_address;
    private String tour_name;
    private String status;
    private LocalDateTime booking_date;
    private LocalDate start_date;
    private LocalDate end_date;
    private Integer total_price;
    private Integer number_of_adults;
    private Integer number_of_children;
    private Integer number_of_infants;
    private Integer voucher_discount;
    private String note;
    private String cancel_reason;
    private String bookingType;
}
