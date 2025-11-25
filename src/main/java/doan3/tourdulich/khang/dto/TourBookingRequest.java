package doan3.tourdulich.khang.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourBookingRequest {
    private String tour_id;
    private String user_id;
    private String user_full_name;
    private String user_email;
    private String user_phone_number;
    private String user_address;
    private String tour_name;
    private LocalDate start_date;
    private Integer total_price;
    private Integer number_of_adults;
    private Integer number_of_children;
    private Integer number_of_infants;
    private String voucherCode;
    private Integer voucher_discount;
    private String note;
    private String bookingType;
}
