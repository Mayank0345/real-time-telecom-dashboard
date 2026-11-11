package com.telecom.dashboard.repository;

import com.telecom.dashboard.model.UsageData;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageDataRepository extends JpaRepository<UsageData, Long> {
    List<UsageData> findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(
            LocalDateTime from, LocalDateTime to);

    @Query("""
            select u
            from UsageData u
            join fetch u.subscriber s
            join fetch s.region r
            where u.recordedAt >= :from
              and u.recordedAt < :to
              and (:regionId is null or r.id = :regionId)
            order by u.recordedAt desc
            """)
    List<UsageData> findRecentUsage(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("regionId") Long regionId,
            Pageable pageable);
}
