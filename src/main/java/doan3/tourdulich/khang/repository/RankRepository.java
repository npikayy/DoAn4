package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankRepository extends JpaRepository<Rank, Integer> {
    @Query("SELECT r FROM Rank r WHERE r.user.user_id = ?1")
    Rank findByUser_id(String userId);
}