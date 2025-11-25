package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.users;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    public static Specification<users> withFilters(String searchQuery, String gender, String ageRange, String registrationDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter for standard users
            predicates.add(criteriaBuilder.equal(root.get("role"), "ROLE_USER"));

            // Keyword filter (search by name, email, or phone number)
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String lowerCaseKeyword = "%" + searchQuery.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("full_name")), lowerCaseKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), lowerCaseKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone_number")), lowerCaseKeyword)
                ));
            }

            // Gender filter
            if (gender != null && !gender.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            // Age range filter
            if (ageRange != null && !ageRange.isEmpty()) {
                try {
                    String[] parts = ageRange.split("-");
                    int minAge = Integer.parseInt(parts[0]);
                    int maxAge = Integer.parseInt(parts[1]);

                    LocalDate today = LocalDate.now();
                    LocalDate maxBirthDate = today.minusYears(minAge);
                    LocalDate minBirthDate = today.minusYears(maxAge + 1).plusDays(1);
                    
                    predicates.add(criteriaBuilder.between(root.get("date_of_birth"), minBirthDate, maxBirthDate));
                } catch (Exception e) {
                    // Ignore invalid age range format
                }
            }
            
            // Registration date filter
            if (registrationDate != null && !registrationDate.isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDate startDate = null;
                LocalDate endDate = today;

                switch (registrationDate) {
                    case "today":
                        startDate = today;
                        break;
                    case "week":
                        startDate = today.with(DayOfWeek.MONDAY);
                        break;
                    case "month":
                        startDate = today.with(TemporalAdjusters.firstDayOfMonth());
                        break;
                    case "year":
                        startDate = today.with(TemporalAdjusters.firstDayOfYear());
                        break;
                }

                if (startDate != null) {
                    predicates.add(criteriaBuilder.between(
                        root.get("created_at"),
                        startDate, 
                        endDate
                    ));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
