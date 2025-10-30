package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer>, JpaSpecificationExecutor<KhuyenMai> {
    @Query("SELECT km FROM KhuyenMai km WHERE LOWER(km.tenKhuyenMai) LIKE %:keyword% OR LOWER(km.moTa) LIKE %:keyword%")
    List<KhuyenMai> findByKeywordInNameOrDescription(@Param("keyword") String keyword);
}
