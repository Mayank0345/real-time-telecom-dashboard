package com.telecom.dashboard.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telecom.dashboard.model.Region;
import com.telecom.dashboard.model.Subscriber;
import com.telecom.dashboard.repository.NetworkOutageRepository;
import com.telecom.dashboard.repository.RegionRepository;
import com.telecom.dashboard.repository.SubscriberRepository;
import com.telecom.dashboard.repository.UsageDataRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulationDataServiceTest {

    @Mock
    private RegionRepository regionRepository;
    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private UsageDataRepository usageDataRepository;
    @Mock
    private NetworkOutageRepository outageRepository;

    private SimulationDataService simulationDataService;

    @BeforeEach
    void setUp() {
        simulationDataService = new SimulationDataService(
                regionRepository, subscriberRepository, usageDataRepository, outageRepository);
    }

    @Test
    void savesUsageForAnActiveSubscriber() {
        Region region = new Region("North", "N");
        Subscriber activeSubscriber = new Subscriber(
                "Active subscriber", "555-0101", "ACTIVE", "Basic",
                java.time.LocalDateTime.now(), region);
        when(subscriberRepository.findAll()).thenReturn(List.of(activeSubscriber));
        when(outageRepository.findAll()).thenReturn(List.of());

        simulationDataService.generateUsageData();

        verify(usageDataRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotSaveUsageWhenThereAreNoActiveSubscribers() {
        Subscriber inactiveSubscriber = new Subscriber(
                "Inactive subscriber", "555-0102", "INACTIVE", "Basic",
                java.time.LocalDateTime.now(), new Region("South", "S"));
        when(subscriberRepository.findAll()).thenReturn(List.of(inactiveSubscriber));

        simulationDataService.generateUsageData();

        verify(usageDataRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outageRepository, never()).findAll();
    }

    @Test
    void doesNotSaveUsageWhenNoSubscribersExist() {
        when(subscriberRepository.findAll()).thenReturn(List.of());

        simulationDataService.generateUsageData();

        verify(usageDataRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outageRepository, never()).findAll();
    }
}
