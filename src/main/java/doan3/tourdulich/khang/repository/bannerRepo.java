package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.carouselBanner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface bannerRepo extends JpaRepository<carouselBanner, Integer> {
}
