package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.vouchers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<vouchers, Integer> {
    @Query("SELECT v FROM vouchers v WHERE v.user.user_id = ?1")
    List<vouchers> findByUser(String userId);
    Optional<vouchers> findByMaVoucher(String maVoucher);
}
