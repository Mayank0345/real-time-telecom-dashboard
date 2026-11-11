package com.telecom.dashboard.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.telecom.dashboard.dto.DashboardResponse;
import com.telecom.dashboard.dto.DashboardStats;
import com.telecom.dashboard.model.NetworkOutage;
import com.telecom.dashboard.model.Region;
import com.telecom.dashboard.model.Subscriber;
import com.telecom.dashboard.model.UsageData;
import com.telecom.dashboard.service.DashboardService;
import com.telecom.dashboard.service.SimulationDataService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;

@Import(DashboardService.class)
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:telecom-dashboard;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseIntegrationTest {

    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private SubscriberRepository subscriberRepository;
    @Autowired
    private UsageDataRepository usageDataRepository;
    @Autowired
    private NetworkOutageRepository outageRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DashboardService dashboardService;

    private Region north;
    private Region south;
    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        north = regionRepository.saveAndFlush(new Region("North", "N"));
        south = regionRepository.saveAndFlush(new Region("South", "S"));
        subscriber = subscriberRepository.saveAndFlush(new Subscriber(
                "Test Subscriber", "555-0001", "ACTIVE", "Standard",
                LocalDateTime.now().minusDays(1), north));

        LocalDate today = LocalDate.now();
        LocalDateTime firstUsageTime = today.atTime(12, 0).minusMinutes(2);
        usageDataRepository.saveAndFlush(
                new UsageData(firstUsageTime, 100, 10, 2, subscriber));
        usageDataRepository.saveAndFlush(
                new UsageData(firstUsageTime.plusMinutes(1), 200, 20, 3, subscriber));
        outageRepository.saveAndFlush(new NetworkOutage(
                "Test outage", LocalDateTime.now().minusMinutes(10), null, north));
    }

    @Test
    void persistsEntitiesAndResolvesRepositoryQueries() {
        assertEquals(1, subscriberRepository.countByRegionId(north.getId()));
        assertEquals(1, subscriberRepository.countByRegionIdAndStatusIgnoreCase(
                north.getId(), "active"));
        assertEquals(0, subscriberRepository.countByRegionId(south.getId()));
        assertEquals(1, outageRepository.countByResolvedAtIsNullAndRegionId(north.getId()));

        List<UsageData> recent = usageDataRepository.findRecentUsage(
                LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(),
                north.getId(), PageRequest.of(0, 1));
        assertEquals(1, recent.size());
        assertNotNull(recent.get(0).getSubscriber().getRegion().getName());
        assertEquals(200.0, recent.get(0).getDataUsedMb(), 0.0);
    }

    @Test
    void executesTheDocumentedSqlAnalyticsAgainstPersistedData() throws Exception {
        Path analyticsPath = List.of(
                        Path.of("database", "analytics.sql"),
                        Path.of("..", "database", "analytics.sql"))
                .stream()
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not locate database/analytics.sql from the project or backend directory"));
        String script = Files.readString(analyticsPath);
        String queryText = script.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (all, line) -> all + line + "\n");
        List<String> queries = Arrays.stream(queryText.split(";"))
                .map(String::strip)
                .filter(query -> !query.isBlank())
                .toList();

        assertFalse(queries.isEmpty());
        for (String query : queries) {
            if (!query.isBlank() && !query.stripLeading().toUpperCase().startsWith("USE ")) {
                jdbcTemplate.queryForList(query);
            }
        }

        Double totalUsage = jdbcTemplate.queryForObject(
                "SELECT SUM(data_usage) FROM usage_data", Double.class);
        assertEquals(300.0, totalUsage, 0.0);
    }

    @Test
    void aggregatesPersistedUsageForDashboardAndApiDtos() {
        var today = LocalDateTime.now().toLocalDate();
        DashboardResponse dashboard = dashboardService.getDashboard(today, today, north.getId());
        DashboardStats stats = dashboardService.getStats(today, today, north.getId());

        assertEquals(1, dashboard.totalSubscribers());
        assertEquals(1, dashboard.activeSubscribers());
        assertEquals(300.0, dashboard.totalUsageMb(), 0.0);
        assertEquals(1, dashboard.activeOutages());
        assertEquals("North", dashboard.regionalUsage().get(0).regionName());
        assertEquals(300.0, dashboard.regionalUsage().get(0).usageMb(), 0.0);
        assertEquals(15.0, stats.averageCallDuration(), 0.0);
        assertEquals(5, stats.totalSmsCount());
        assertEquals(2, dashboardService.getRecentUsage(today, today, north.getId()).size());
    }

    @Test
    void scheduledGeneratorPersistsUsageThroughTheJpaRepository() {
        long before = usageDataRepository.count();
        SimulationDataService simulationDataService = new SimulationDataService(
                regionRepository, subscriberRepository, usageDataRepository, outageRepository);

        simulationDataService.generateUsageData();

        assertEquals(before + 1, usageDataRepository.count());
    }
}
