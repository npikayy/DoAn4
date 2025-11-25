package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "vouchers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class vouchers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "ma_voucher", unique = true, nullable = false)
    private String maVoucher;

    @Column(name = "voucher_type")
    private String voucherType; // "PERCENTAGE" or "AMOUNT"

    @Column(name = "gia_tri_giam")
    private int giaTriGiam;

    @Column(name = "ngay_het_han")
    @Temporal(TemporalType.DATE)
    private Date ngayHetHan;

    @Column(name = "trang_thai")
    private String trangThai; // e.g., "ACTIVE", "USED", "EXPIRED"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private tours tour; // New field to associate voucher with a specific tour
}