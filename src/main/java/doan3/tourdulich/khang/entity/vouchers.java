package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class vouchers {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "voucher_seq", sequenceName = "voucher_seq", allocationSize = 1)
    private Integer voucher_id;
    private String voucher_code;
    private String voucher_description;
    private LocalDate voucher_end_date;
    private Integer voucher_discount;
    private String voucher_type;
}
