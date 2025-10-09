package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.forgotPassword;
import jakarta.transaction.Transactional;
import doan3.tourdulich.khang.entity.users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FPRepository extends JpaRepository<forgotPassword, Integer> {
    @Query("SELECT fp FROM forgotPassword fp WHERE fp.otp = :otp AND fp.user = :user")
    Optional<forgotPassword> findByOtpAndUser(Integer otp, users user);
    @Transactional
    void deleteByFpId(Integer fpId);

    @Query("SELECT fp FROM forgotPassword fp WHERE fp.user = :user")
    forgotPassword findByUser(users user);
}
