package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_bookings;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class BookingSpecifications {

    public static Specification<tour_bookings> withStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<tour_bookings> withUserId(String userId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(userId) || "all".equalsIgnoreCase(userId)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user_id"), userId);
        };
    }

    public static Specification<tour_bookings> withSearchQuery(String searchQuery) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(searchQuery)) {
                return criteriaBuilder.conjunction();
            }

            String likePattern = "%" + searchQuery.toLowerCase() + "%";

            // Predicate for text fields (always active)
            Predicate textSearch = criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("user_full_name")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("tour_name")), likePattern)
            );

            // Predicate for ID search (only if searchQuery is a valid integer)
            try {
                Integer searchId = Integer.parseInt(searchQuery);
                Predicate idSearch = criteriaBuilder.equal(root.get("booking_id"), searchId);
                // Combine text search and ID search
                return criteriaBuilder.or(textSearch, idSearch);
            } catch (NumberFormatException e) {
                // If not a number, only search by text fields
                return textSearch;
            }
        };
    }
}
