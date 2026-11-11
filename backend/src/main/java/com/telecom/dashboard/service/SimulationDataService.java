package com.telecom.dashboard.service;

import com.telecom.dashboard.model.NetworkOutage;
import com.telecom.dashboard.model.Region;
import com.telecom.dashboard.model.Subscriber;
import com.telecom.dashboard.model.UsageData;
import com.telecom.dashboard.repository.NetworkOutageRepository;
import com.telecom.dashboard.repository.RegionRepository;
import com.telecom.dashboard.repository.SubscriberRepository;
import com.telecom.dashboard.repository.UsageDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationDataService implements ApplicationRunner {

    private static final String[] REGION_NAMES = {"North", "South", "East", "West"};
    private static final String[] REGION_CODES = {"N", "S", "E", "W"};
    private static final String[] PLANS = {"Basic", "Standard", "Premium"};
    private static final String[] OUTAGE_DESCRIPTIONS = {
        "Fiber maintenance", "Power fluctuation", "Cell tower maintenance"
    };

    private final RegionRepository regionRepository;
    private final SubscriberRepository subscriberRepository;
    private final UsageDataRepository usageDataRepository;
    private final NetworkOutageRepository outageRepository;
    private final Random random = new Random();

    public SimulationDataService(RegionRepository regionRepository,
                                 SubscriberRepository subscriberRepository,
                                 UsageDataRepository usageDataRepository,
                                 NetworkOutageRepository outageRepository) {
        this.regionRepository = regionRepository;
        this.subscriberRepository = subscriberRepository;
        this.usageDataRepository = usageDataRepository;
        this.outageRepository = outageRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (regionRepository.count() == 0) {
            for (int i = 0; i < REGION_NAMES.length; i++) {
                regionRepository.save(new Region(REGION_NAMES[i], REGION_CODES[i]));
            }
        }

        List<Region> regions = regionRepository.findAllByOrderByNameAsc();
        if (subscriberRepository.count() == 0) {
            int subscriberNumber = 1001;
            for (int i = 0; i < 32; i++) {
                Region region = regions.get(i % regions.size());
                subscriberRepository.save(new Subscriber(
                        "Sample Subscriber " + subscriberNumber,
                        "555-" + subscriberNumber++, i % 8 == 0 ? "INACTIVE" : "ACTIVE",
                        PLANS[i % PLANS.length],
                        LocalDateTime.now().minusDays(random.nextInt(180)), region));
            }
        }

        if (usageDataRepository.count() == 0) {
            List<Subscriber> subscribers = subscriberRepository.findAll().stream()
                    .filter(subscriber -> "ACTIVE".equalsIgnoreCase(subscriber.getStatus()))
                    .toList();
            LocalDateTime now = LocalDateTime.now();
            for (int day = 6; day >= 0; day--) {
                for (Subscriber subscriber : subscribers) {
                    LocalDateTime recordedAt = now.minusDays(day)
                            .withHour(random.nextInt(24))
                            .withMinute(random.nextInt(60))
                            .withSecond(0)
                            .withNano(0);
                    usageDataRepository.save(createUsage(recordedAt, subscriber));
                }
            }
        }

        if (outageRepository.count() == 0 && !regions.isEmpty()) {
            outageRepository.save(new NetworkOutage(
                    "Example active outage - simulated tower maintenance",
                    LocalDateTime.now().minusMinutes(18), null, regions.get(0)));
            outageRepository.save(new NetworkOutage(
                    "Resolved fiber maintenance",
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusMinutes(35),
                    regions.get(Math.min(1, regions.size() - 1))));
        }
    }

    @Scheduled(fixedRate = 10_000)
    @Transactional
    public void generateUsageData() {
        List<Subscriber> subscribers = subscriberRepository.findAll().stream()
                .filter(subscriber -> "ACTIVE".equalsIgnoreCase(subscriber.getStatus()))
                .toList();
        if (subscribers.isEmpty()) {
            return;
        }
        Subscriber subscriber = subscribers.get(random.nextInt(subscribers.size()));
        usageDataRepository.save(createUsage(LocalDateTime.now(), subscriber));

        if (random.nextInt(300) == 0) {
            Region region = subscriber.getRegion();
            outageRepository.save(new NetworkOutage(
                    OUTAGE_DESCRIPTIONS[random.nextInt(OUTAGE_DESCRIPTIONS.length)],
                    LocalDateTime.now(), null, region));
        }

        outageRepository.findAll().stream()
                .filter(outage -> outage.getResolvedAt() == null)
                .filter(outage -> outage.getStartedAt().isBefore(LocalDateTime.now().minusMinutes(30)))
                .forEach(outage -> outage.setResolvedAt(LocalDateTime.now()));
    }

    private UsageData createUsage(LocalDateTime recordedAt, Subscriber subscriber) {
        double dataUsedMb = Math.round((50 + random.nextDouble() * 950) * 100.0) / 100.0;
        return new UsageData(recordedAt, dataUsedMb, random.nextInt(61), random.nextInt(21), subscriber);
    }
}
