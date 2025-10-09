package doan3.tourdulich.khang.dto;
import java.util.Arrays;
import java.util.List;

public class VietnamProvinces {
    public enum Region {
        NORTH,
        CENTRAL,
        SOUTHEAST,  // Đông Nam Bộ
        SOUTHWEST,  // Tây Nam Bộ (Đồng bằng sông Cửu Long)
        OTHER
    }

    public static final List<String> NORTHERN_PROVINCES = Arrays.asList(
            "Thành phố Hà Nội", "Tỉnh Hà Giang", "Tỉnh Cao Bằng", "Tỉnh Bắc Kạn", "Tỉnh Tuyên Quang",
            "Tỉnh Lào Cai", "Tỉnh Điện Biên", "Tỉnh Lai Châu", "Tỉnh Sơn La", "Tỉnh Yên Bái",
            "Tỉnh Hoà Bình", "Tỉnh Thái Nguyên", "Tỉnh Lạng Sơn", "Tỉnh Quảng Ninh",
            "Tỉnh Bắc Giang", "Tỉnh Phú Thọ", "Tỉnh Vĩnh Phúc", "Tỉnh Bắc Ninh", "Tỉnh Hải Dương",
            "Thành phố Hải Phòng", "Tỉnh Hưng Yên", "Tỉnh Thái Bình", "Tỉnh Hà Nam", "Tỉnh Nam Định",
            "Tỉnh Ninh Bình"
    );

    public static final List<String> CENTRAL_PROVINCES = Arrays.asList(
            "Tỉnh Thanh Hóa", "Tỉnh Nghệ An", "Tỉnh Hà Tĩnh", "Tỉnh Quảng Bình", "Tỉnh Quảng Trị",
            "Tỉnh Thừa Thiên Huế", "Thành phố Đà Nẵng", "Tỉnh Quảng Nam", "Tỉnh Quảng Ngãi",
            "Tỉnh Bình Định", "Tỉnh Phú Yên", "Tỉnh Khánh Hòa", "Tỉnh Ninh Thuận", "Tỉnh Bình Thuận"
    );

    public static final List<String> EASTERN_SOUTH_PROVINCES = Arrays.asList(
            "Thành phố Hồ Chí Minh", "Tỉnh Bà Rịa - Vũng Tàu", "Tỉnh Bình Dương", "Tỉnh Bình Phước",
            "Tỉnh Đồng Nai", "Tỉnh Tây Ninh"
    );

    public static final List<String> WESTERN_SOUTH_PROVINCES = Arrays.asList(
            "Tỉnh An Giang", "Tỉnh Bạc Liêu", "Tỉnh Bến Tre", "Tỉnh Cà Mau", "Tỉnh Đồng Tháp",
            "Tỉnh Hậu Giang", "Tỉnh Kiên Giang", "Tỉnh Long An", "Tỉnh Sóc Trăng", "Tỉnh Tiền Giang",
            "Tỉnh Trà Vinh", "Tỉnh Vĩnh Long", "Thành phố Cần Thơ"
    );

    public static Region getRegionByProvince(String province) {
        String normalizedProvince = province;

        if (NORTHERN_PROVINCES.contains(normalizedProvince)) {
            return Region.NORTH;
        } else if (CENTRAL_PROVINCES.contains(normalizedProvince)) {
            return Region.CENTRAL;
        } else if (EASTERN_SOUTH_PROVINCES.contains(normalizedProvince)) {
            return Region.SOUTHEAST;
        } else if (WESTERN_SOUTH_PROVINCES.contains(normalizedProvince)) {
            return Region.SOUTHWEST;
        } else {
            return Region.OTHER;
        }
    }

}
