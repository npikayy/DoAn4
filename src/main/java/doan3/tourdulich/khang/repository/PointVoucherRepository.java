package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.PointVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointVoucherRepository extends JpaRepository<PointVoucher, Integer> {
}
