package com.telecom.dashboard.repository;

import com.telecom.dashboard.model.NetworkOutage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkOutageRepository extends JpaRepository<NetworkOutage, Long> {
    long countByResolvedAtIsNull();

    long countByResolvedAtIsNullAndRegionId(Long regionId);

    List<NetworkOutage> findByResolvedAtIsNull();

    List<NetworkOutage> findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            LocalDateTime from, LocalDateTime to);
}
