package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_ratings;
import org.springframework.data.jpa.domain.Specification;

public class RatingSpecifications {

    public static Specification<tour_ratings> withUserId(String userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("user_id"), userId);
    }

    public static Specification<tour_ratings> withRating(String ratingFilter) {
        return (root, query, criteriaBuilder) -> {
            if ("all".equalsIgnoreCase(ratingFilter)) {
                return criteriaBuilder.conjunction();
            } else if ("1-3".equals(ratingFilter)) {
                return criteriaBuilder.between(root.get("rating"), 1, 3);
            } else {
                return criteriaBuilder.equal(root.get("rating"), Integer.parseInt(ratingFilter));
            }
        };
    }

    public static Specification<tour_ratings> withSearchQuery(String searchQuery) {
        return (root, query, criteriaBuilder) -> {
            String likePattern = "%" + searchQuery.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("tour").get("tour_name")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("comment")), likePattern)
            );
        };
    }
}
