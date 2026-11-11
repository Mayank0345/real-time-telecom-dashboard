package com.telecom.dashboard.controller;

import com.telecom.dashboard.dto.DashboardResponse;
import com.telecom.dashboard.dto.DashboardStats;
import com.telecom.dashboard.dto.MonthlyUsage;
import com.telecom.dashboard.dto.OutageDto;
import com.telecom.dashboard.dto.RegionDto;
import com.telecom.dashboard.dto.SubscriberDto;
import com.telecom.dashboard.dto.UsageDto;
import com.telecom.dashboard.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        LocalDate start = range.from();
        LocalDate end = range.to();
        return dashboardService.getDashboard(start, end, regionId);
    }

    @GetMapping("/subscribers")
    public List<SubscriberDto> getSubscribers(
            @RequestParam(required = false) Long regionId) {
        return dashboardService.getSubscribers(regionId);
    }

    @GetMapping("/usage")
    public List<UsageDto> getUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getUsage(range.from(), range.to(), regionId);
    }

    @GetMapping("/usage/recent")
    public List<UsageDto> getRecentUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getRecentUsage(range.from(), range.to(), regionId);
    }

    @GetMapping("/usage/monthly")
    public List<MonthlyUsage> getMonthlyUsage(
            @RequestParam(required = false) Long regionId) {
        return dashboardService.getMonthlyUsage(regionId);
    }

    @GetMapping("/usage/region")
    public List<DashboardResponse.RegionalUsage> getRegionalUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getRegionalUsage(range.from(), range.to(), regionId);
    }

    @GetMapping("/usage/daily")
    public List<DashboardResponse.DailyUsage> getDailyUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getDailyUsage(range.from(), range.to(), regionId);
    }

    @GetMapping("/outages")
    public List<OutageDto> getOutages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getOutages(range.from(), range.to(), regionId);
    }

    @GetMapping("/dashboard/stats")
    public DashboardStats getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long regionId) {
        DateRange range = resolveDateRange(from, to);
        return dashboardService.getStats(range.from(), range.to(), regionId);
    }

    @GetMapping("/regions")
    public List<RegionDto> getRegions() {
        return dashboardService.getRegions();
    }

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
        if (start.isBefore(end.minusYears(1))) {
            throw new IllegalArgumentException("Date range cannot be longer than one year");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
