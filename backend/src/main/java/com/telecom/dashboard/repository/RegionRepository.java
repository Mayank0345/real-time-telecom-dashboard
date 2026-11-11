package com.telecom.dashboard.repository;

import com.telecom.dashboard.model.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByOrderByNameAsc();
}
