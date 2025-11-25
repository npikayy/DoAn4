package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "point_vouchers")
@Getter
@Setter
public class PointVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Integer pointsCost;
    private Integer quantity; // New field

    @Enumerated(EnumType.STRING)
    private RedeemableVoucherType redeemableVoucherType; // FREE_TOUR or DISCOUNT

    private String voucherType; // PERCENTAGE or AMOUNT, for discount vouchers
    private Integer giaTriGiam; // Discount value, for discount vouchers
}