package com.telecom.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        long totalSubscribers,
        long activeSubscribers,
        double totalUsageMb,
        long activeOutages,
        List<DailyUsage> usageTrend,
        List<RegionalUsage> regionalUsage,
        List<OutageSummary> outages) {

    public record DailyUsage(LocalDate date, double usageMb) {
    }

    public record RegionalUsage(Long regionId, String regionName, double usageMb,
                                long activeOutages, String networkStatus) {
    }

    public record OutageSummary(Long id, String regionName, String description,
                                LocalDateTime startedAt, LocalDateTime resolvedAt) {
    }
}
