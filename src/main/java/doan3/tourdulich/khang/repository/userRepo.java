package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface userRepo extends JpaRepository<users, String> {

    users findByUsername(String username);

    users findByEmail(String email);



    @Query("SELECT u FROM users u WHERE u.role = 'ROLE_USER' AND u.email <> 'admin@gmail.com'")
    Page<users> findAllUsersExceptAdmin(Pageable pageable);

    @Query("SELECT u FROM users u WHERE u.role = 'ROLE_USER'")
    List<users> findAllUsers();

    @Query(value = "SELECT EXTRACT(YEAR FROM created_at) as year, " +
            "EXTRACT(MONTH FROM created_at) as month, " +
            "COUNT(*) as user_count " +
            "FROM users " +
            "WHERE role = 'ROLE_USER' " +
            "GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at) " +
            "ORDER BY year, month",
            nativeQuery = true)
    List<Object[]> getUserGrowthByMonth();
}
