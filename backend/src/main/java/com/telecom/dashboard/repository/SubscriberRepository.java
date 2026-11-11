package com.telecom.dashboard.repository;

import com.telecom.dashboard.model.Subscriber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    long countByRegionId(Long regionId);

    long countByStatusIgnoreCase(String status);

    long countByRegionIdAndStatusIgnoreCase(Long regionId, String status);

    List<Subscriber> findAllByRegionIdOrderByIdAsc(Long regionId);
}
