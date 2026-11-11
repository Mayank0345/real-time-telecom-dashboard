package com.telecom.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.telecom.dashboard.dto.DashboardStats;
import com.telecom.dashboard.repository.NetworkOutageRepository;
import com.telecom.dashboard.repository.RegionRepository;
import com.telecom.dashboard.repository.SubscriberRepository;
import com.telecom.dashboard.repository.UsageDataRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RegionRepository regionRepository;
    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private UsageDataRepository usageDataRepository;
    @Mock
    private NetworkOutageRepository outageRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                regionRepository, subscriberRepository, usageDataRepository, outageRepository);
    }

    @Test
    void returnsZeroUsageMetricsWhenThereAreNoUsageRecords() {
        LocalDate today = LocalDate.now();
        when(usageDataRepository.findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(outageRepository
                .findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(subscriberRepository.count()).thenReturn(0L);
        when(subscriberRepository.countByStatusIgnoreCase("ACTIVE")).thenReturn(0L);
        when(outageRepository.countByResolvedAtIsNull()).thenReturn(0L);

        DashboardStats stats = dashboardService.getStats(today, today, null);

        assertEquals(0, stats.totalSubscribers());
        assertEquals(0, stats.activeSubscribers());
        assertEquals(0.0, stats.totalDataUsage(), 0.0);
        assertEquals(0.0, stats.averageCallDuration(), 0.0);
        assertEquals(0, stats.totalSmsCount());
        assertEquals(0, stats.totalOutages());
        assertEquals(0, stats.activeOutages());
    }

    @Test
    void returnsEmptyMetricsForARegionWithNoSubscribersOrUsage() {
        LocalDate today = LocalDate.now();
        Long unknownRegionId = 999L;
        when(usageDataRepository.findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(outageRepository
                .findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(subscriberRepository.countByRegionId(unknownRegionId)).thenReturn(0L);
        when(subscriberRepository.countByRegionIdAndStatusIgnoreCase(unknownRegionId, "ACTIVE"))
                .thenReturn(0L);
        when(outageRepository.countByResolvedAtIsNullAndRegionId(unknownRegionId)).thenReturn(0L);

        DashboardStats stats = dashboardService.getStats(today, today, unknownRegionId);

        assertEquals(0, stats.totalSubscribers());
        assertEquals(0, stats.activeSubscribers());
        assertEquals(0.0, stats.totalDataUsage(), 0.0);
        assertEquals(0, stats.totalOutages());
        assertEquals(0, stats.activeOutages());
    }
}
