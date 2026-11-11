package com.telecom.dashboard.service;

import com.telecom.dashboard.dto.DashboardResponse;
import com.telecom.dashboard.dto.DashboardStats;
import com.telecom.dashboard.dto.MonthlyUsage;
import com.telecom.dashboard.dto.OutageDto;
import com.telecom.dashboard.dto.RegionDto;
import com.telecom.dashboard.dto.SubscriberDto;
import com.telecom.dashboard.dto.UsageDto;
import com.telecom.dashboard.model.NetworkOutage;
import com.telecom.dashboard.model.Region;
import com.telecom.dashboard.model.Subscriber;
import com.telecom.dashboard.model.UsageData;
import com.telecom.dashboard.repository.NetworkOutageRepository;
import com.telecom.dashboard.repository.RegionRepository;
import com.telecom.dashboard.repository.SubscriberRepository;
import com.telecom.dashboard.repository.UsageDataRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final RegionRepository regionRepository;
    private final SubscriberRepository subscriberRepository;
    private final UsageDataRepository usageDataRepository;
    private final NetworkOutageRepository outageRepository;

    public DashboardService(RegionRepository regionRepository,
                            SubscriberRepository subscriberRepository,
                            UsageDataRepository usageDataRepository,
                            NetworkOutageRepository outageRepository) {
        this.regionRepository = regionRepository;
        this.subscriberRepository = subscriberRepository;
        this.usageDataRepository = usageDataRepository;
        this.outageRepository = outageRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate from, LocalDate to, Long regionId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        List<UsageData> usageRecords =
                usageDataRepository.findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                        start, endExclusive);
        Map<LocalDate, Double> dailyTotals = new LinkedHashMap<>();
        Map<Long, Double> regionalTotals = new LinkedHashMap<>();
        Map<Long, String> regionNames = new LinkedHashMap<>();
        regionRepository.findAllByOrderByNameAsc().stream()
                .filter(region -> regionId == null || regionId.equals(region.getId()))
                .forEach(region -> {
                    regionalTotals.put(region.getId(), 0.0);
                    regionNames.put(region.getId(), region.getName());
                });
        double totalUsage = 0;

        for (UsageData record : usageRecords) {
            Region region = record.getSubscriber().getRegion();
            if (regionId != null && !regionId.equals(region.getId())) {
                continue;
            }

            double usage = record.getDataUsedMb();
            totalUsage += usage;
            dailyTotals.merge(record.getRecordedAt().toLocalDate(), usage, Double::sum);
            regionalTotals.merge(region.getId(), usage, Double::sum);
            regionNames.put(region.getId(), region.getName());
        }

        List<DashboardResponse.DailyUsage> trend = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            trend.add(new DashboardResponse.DailyUsage(day, dailyTotals.getOrDefault(day, 0.0)));
        }

        Map<Long, Long> activeOutagesByRegion = getActiveOutagesByRegion();
        List<DashboardResponse.RegionalUsage> regionalUsage = regionalTotals.entrySet().stream()
                .map(entry -> {
                    long regionActiveOutages = activeOutagesByRegion.getOrDefault(
                            entry.getKey(), 0L);
                    return new DashboardResponse.RegionalUsage(
                            entry.getKey(), regionNames.get(entry.getKey()), entry.getValue(),
                            regionActiveOutages,
                            regionActiveOutages == 0 ? "NORMAL" : "OUTAGE");
                })
                .sorted(Comparator.comparing(DashboardResponse.RegionalUsage::usageMb).reversed())
                .toList();

        List<NetworkOutage> outageRecords =
                outageRepository.findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        start, endExclusive);
        List<DashboardResponse.OutageSummary> outages = outageRecords.stream()
                .filter(outage -> regionId == null || regionId.equals(outage.getRegion().getId()))
                .limit(10)
                .map(outage -> new DashboardResponse.OutageSummary(
                        outage.getId(), outage.getRegion().getName(), outage.getDescription(),
                        outage.getStartedAt(), outage.getResolvedAt()))
                .toList();

        long subscribers = regionId == null
                ? subscriberRepository.count()
                : subscriberRepository.countByRegionId(regionId);
        long activeOutages = regionId == null
                ? outageRepository.countByResolvedAtIsNull()
                : outageRepository.countByResolvedAtIsNullAndRegionId(regionId);

        long activeSubscribers = regionId == null
                ? subscriberRepository.countByStatusIgnoreCase("ACTIVE")
                : subscriberRepository.countByRegionIdAndStatusIgnoreCase(regionId, "ACTIVE");

        return new DashboardResponse(subscribers, activeSubscribers, totalUsage, activeOutages,
                trend, regionalUsage, outages);
    }

    @Transactional(readOnly = true)
    public List<RegionDto> getRegions() {
        return regionRepository.findAllByOrderByNameAsc().stream()
                .map(region -> new RegionDto(region.getId(), region.getName(), region.getCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriberDto> getSubscribers(Long regionId) {
        List<Subscriber> subscribers = regionId == null
                ? subscriberRepository.findAll()
                : subscriberRepository.findAllByRegionIdOrderByIdAsc(regionId);
        return subscribers.stream()
                .map(subscriber -> new SubscriberDto(
                        subscriber.getId(), subscriber.getName(), subscriber.getPhoneNumber(),
                        subscriber.getStatus(), subscriber.getPlan(),
                        subscriber.getRegion().getId(), subscriber.getRegion().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsageDto> getUsage(LocalDate from, LocalDate to, Long regionId) {
        return getUsageRecords(from, to, regionId).stream()
                .map(this::toUsageDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsageDto> getRecentUsage(LocalDate from, LocalDate to, Long regionId) {
        return usageDataRepository.findRecentUsage(
                        from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                        regionId, PageRequest.of(0, 20))
                .stream()
                .map(this::toUsageDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyUsage> getMonthlyUsage(Long regionId) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(11);
        LocalDateTime from = firstMonth.atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<YearMonth, Double> monthlyTotals =
                usageDataRepository.findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(from, to)
                        .stream()
                        .filter(record -> regionId == null || regionId.equals(
                                record.getSubscriber().getRegion().getId()))
                        .collect(Collectors.groupingBy(
                                record -> YearMonth.from(record.getRecordedAt()),
                                Collectors.summingDouble(UsageData::getDataUsedMb)));

        List<MonthlyUsage> trend = new ArrayList<>();
        for (YearMonth month = firstMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            trend.add(new MonthlyUsage(month.toString(), monthlyTotals.getOrDefault(month, 0.0)));
        }
        return trend;
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse.RegionalUsage> getRegionalUsage(
            LocalDate from, LocalDate to, Long regionId) {
        return getDashboard(from, to, regionId).regionalUsage();
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse.DailyUsage> getDailyUsage(
            LocalDate from, LocalDate to, Long regionId) {
        return getDashboard(from, to, regionId).usageTrend();
    }

    @Transactional(readOnly = true)
    public List<OutageDto> getOutages(LocalDate from, LocalDate to, Long regionId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        return outageRepository
                .findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        start, endExclusive)
                .stream()
                .filter(outage -> regionId == null || regionId.equals(outage.getRegion().getId()))
                .map(outage -> new OutageDto(
                        outage.getId(), outage.getRegion().getId(), outage.getRegion().getName(),
                        outage.getDurationMinutes(), outage.getDescription(),
                        outage.getStartedAt(), outage.getResolvedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats(LocalDate from, LocalDate to, Long regionId) {
        List<UsageData> usageRecords = getUsageRecords(from, to, regionId);
        double totalDataUsage = usageRecords.stream()
                .mapToDouble(UsageData::getDataUsedMb)
                .sum();
        double averageCallDuration = usageRecords.stream()
                .mapToInt(UsageData::getCallMinutes)
                .average()
                .orElse(0.0);
        long totalSmsCount = usageRecords.stream()
                .mapToLong(UsageData::getSmsCount)
                .sum();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        long totalOutages = outageRepository
                .findByStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        start, endExclusive)
                .stream()
                .filter(outage -> regionId == null || regionId.equals(outage.getRegion().getId()))
                .count();
        long totalSubscribers = regionId == null
                ? subscriberRepository.count()
                : subscriberRepository.countByRegionId(regionId);
        long activeSubscribers = regionId == null
                ? subscriberRepository.countByStatusIgnoreCase("ACTIVE")
                : subscriberRepository.countByRegionIdAndStatusIgnoreCase(regionId, "ACTIVE");
        long activeOutages = regionId == null
                ? outageRepository.countByResolvedAtIsNull()
                : outageRepository.countByResolvedAtIsNullAndRegionId(regionId);
        return new DashboardStats(totalSubscribers, activeSubscribers,
                totalDataUsage, averageCallDuration, totalSmsCount, totalOutages, activeOutages);
    }

    private UsageDto toUsageDto(UsageData record) {
        return new UsageDto(
                record.getId(), record.getSubscriber().getId(),
                record.getSubscriber().getName(),
                record.getSubscriber().getRegion().getId(),
                record.getSubscriber().getRegion().getName(),
                record.getDataUsedMb(), record.getCallMinutes(), record.getSmsCount(),
                record.getRecordedAt());
    }

    private List<UsageData> getUsageRecords(LocalDate from, LocalDate to, Long regionId) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        return usageDataRepository
                .findByRecordedAtGreaterThanEqualAndRecordedAtLessThan(start, endExclusive)
                .stream()
                .filter(record -> regionId == null
                        || regionId.equals(record.getSubscriber().getRegion().getId()))
                .toList();
    }

    private Map<Long, Long> getActiveOutagesByRegion() {
        return outageRepository.findByResolvedAtIsNull().stream()
                .collect(Collectors.groupingBy(
                        outage -> outage.getRegion().getId(), Collectors.counting()));
    }
}
