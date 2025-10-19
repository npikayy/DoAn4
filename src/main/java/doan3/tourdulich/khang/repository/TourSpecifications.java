package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tours;
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
            String sortBy) {

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

            // Promotion filter (from client)
            if (hasPromotion != null && hasPromotion) {
                predicates.add(criteriaBuilder.isNotNull(root.get("discount_promotion")));
            }

            // Promotion status filter (from admin)
            if (promotionStatus != null && !promotionStatus.isEmpty()) {
                if (promotionStatus.equals("HAS_DISCOUNT")) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("discount_promotion")));
                } else if (promotionStatus.equals("NO_DISCOUNT")) {
                    predicates.add(criteriaBuilder.isNull(root.get("discount_promotion")));
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

            // Sorting logic
            if (sortBy != null && !sortBy.isEmpty()) {
                switch (sortBy) {
                    case "price_asc":
                        query.orderBy(criteriaBuilder.asc(root.get("tour_adult_price")));
                        break;
                    case "price_desc":
                        query.orderBy(criteriaBuilder.desc(root.get("tour_adult_price")));
                        break;
                    case "rating_desc":
                        query.orderBy(criteriaBuilder.desc(root.get("tour_rating")));
                        break;
                    case "name-asc":
                        query.orderBy(criteriaBuilder.asc(root.get("tour_name")));
                        break;
                    default:
                        break;
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
