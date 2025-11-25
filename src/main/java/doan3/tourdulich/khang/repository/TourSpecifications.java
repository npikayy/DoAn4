package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.KhuyenMai;
import doan3.tourdulich.khang.entity.tours;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class TourSpecifications {

    public static Specification<tours> withFilters(
            String keyword,
            Boolean isAbroad,
            String tourType,
            String duration,
            String transportation,
            String priceRange,
            String location,
            String region,
            Boolean hasPromotion,
            String promotionStatus,
            String startDate,
            String departureStatus,
            String rating,
            Boolean redeemableWithPoints) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword filter (search by name or location)
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tour_name")), "%" + keyword.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tour_end_location")), "%" + keyword.toLowerCase() + "%")
                ));
            }

            // isAbroad filter (for client)
            if (isAbroad != null) {
                predicates.add(criteriaBuilder.equal(root.get("is_abroad"), isAbroad));
            }

            // tourType filter (for admin)
            if (tourType != null && !tourType.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("tour_type"), tourType));
            }

            // Region filter
            if (region != null && !region.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("tour_region"), region));
            }

            // Duration filter
            if (duration != null && !duration.isEmpty()) {
                String[] parts = duration.split("-");
                if (parts.length == 2) {
                    try {
                        int minDuration = Integer.parseInt(parts[0]);
                        int maxDuration = Integer.parseInt(parts[1]);
                        predicates.add(criteriaBuilder.between(root.get("tour_duration"), minDuration, maxDuration));
                    } catch (NumberFormatException e) {
                        // Ignore invalid duration format
                    }
                }
            }

            // Transportation filter
            if (transportation != null && !transportation.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("tour_transportation"), transportation));
            }

            // Price range filter
            if (priceRange != null && !priceRange.isEmpty()) {
                String[] parts = priceRange.split("-");
                if (parts.length == 2) {
                    try {
                        int minPrice = Integer.parseInt(parts[0]);
                        int maxPrice = Integer.parseInt(parts[1]);
                        predicates.add(criteriaBuilder.between(root.get("tour_adult_price"), minPrice, maxPrice));
                    } catch (NumberFormatException e) {
                        // Ignore invalid price range format
                    }
                }
            }

            // Location filter
            if (location != null && !location.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("tour_end_location"), location));
            }

            // Rating filter
            if (rating != null && !rating.isEmpty()) {
                try {
                    float minRating = Float.parseFloat(rating);
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("tour_rating"), minRating));
                } catch (NumberFormatException e) {
                    // Ignore invalid rating format
                }
            }


            // Promotion filter (from client)
            if (hasPromotion != null && hasPromotion) {
                Join<tours, KhuyenMai> promotionJoin = root.join("discount_promotion");
                predicates.add(criteriaBuilder.isNotNull(promotionJoin));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(promotionJoin.get("ngayKetThuc"), new java.util.Date()));
            }

            // Promotion status filter (from admin)
            if (promotionStatus != null && !promotionStatus.isEmpty()) {
                Join<tours, KhuyenMai> promotionJoin = root.join("discount_promotion", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Expression<java.util.Date> ngayBatDau = promotionJoin.get("ngayBatDau");
                jakarta.persistence.criteria.Expression<java.util.Date> ngayKetThuc = promotionJoin.get("ngayKetThuc");
                java.util.Date today = new java.util.Date(); // Use java.util.Date for comparison

                switch (promotionStatus) {
                    case "ACTIVE_PROMOTION":
                        // For active promotions, ensure a promotion exists and its dates are valid
                        predicates.add(criteriaBuilder.isNotNull(promotionJoin));
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(ngayBatDau, today));
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(ngayKetThuc, today));
                        break;
                    case "UPCOMING_PROMOTION":
                        // For upcoming promotions, ensure a promotion exists and its start date is in the future
                        predicates.add(criteriaBuilder.isNotNull(promotionJoin));
                        predicates.add(criteriaBuilder.greaterThan(ngayBatDau, today));
                        break;
                    case "EXPIRED_PROMOTION":
                        // For expired promotions, ensure a promotion exists and its end date is in the past
                        predicates.add(criteriaBuilder.isNotNull(promotionJoin));
                        predicates.add(criteriaBuilder.lessThan(ngayKetThuc, today));
                        break;
                    case "NO_PROMOTION":
                        // For no promotion, ensure the discount_promotion is null
                        predicates.add(criteriaBuilder.isNull(promotionJoin));
                        break;
                }
            }

            // Start date filter
            if (startDate != null && !startDate.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.join("tour_start_date").get("start_date"), java.time.LocalDate.parse(startDate)));
            }

            // Departure status filter (from admin)
            if (departureStatus != null && !departureStatus.isEmpty()) {
                if (departureStatus.equals("HAS_DEPARTURE")) {
                    predicates.add(criteriaBuilder.isNotEmpty(root.get("tour_start_date")));
                } else if (departureStatus.equals("NO_DEPARTURE")) {
                    predicates.add(criteriaBuilder.isEmpty(root.get("tour_start_date")));
                }
            }

            // Redeemable with Points filter
            if (redeemableWithPoints != null) {
                predicates.add(criteriaBuilder.equal(root.get("redeemableWithPoints"), redeemableWithPoints));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
