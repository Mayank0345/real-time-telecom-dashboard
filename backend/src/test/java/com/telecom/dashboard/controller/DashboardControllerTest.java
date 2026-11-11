package com.telecom.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telecom.dashboard.dto.DashboardStats;
import com.telecom.dashboard.dto.UsageDto;
import com.telecom.dashboard.service.DashboardService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@org.springframework.test.context.TestPropertySource(properties =
        "app.cors.allowed-origin-patterns=https://dashboard.example.vercel.app")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void returnsDashboardStatsAsJson() throws Exception {
        when(dashboardService.getStats(any(), any(), isNull()))
                .thenReturn(new DashboardStats(12, 9, 450.5, 12.5, 17, 2, 1));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscribers").value(12))
                .andExpect(jsonPath("$.activeSubscribers").value(9))
                .andExpect(jsonPath("$.totalDataUsage").value(450.5))
                .andExpect(jsonPath("$.averageCallDuration").value(12.5))
                .andExpect(jsonPath("$.totalSmsCount").value(17))
                .andExpect(jsonPath("$.totalOutages").value(2))
                .andExpect(jsonPath("$.activeOutages").value(1));
    }

    @Test
    void returnsAnEmptyJsonArrayWhenThereAreNoRecentUsageRecords() throws Exception {
        when(dashboardService.getRecentUsage(any(), any(), isNull())).thenReturn(List.<UsageDto>of());

        mockMvc.perform(get("/api/usage/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void allowsConfiguredFrontendOriginsToCallTheApi() throws Exception {
        mockMvc.perform(options("/api/dashboard/stats")
                        .header("Origin", "https://dashboard.example.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://dashboard.example.vercel.app"));
    }

    @Test
    void returnsBadRequestForInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/usage/daily")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("'from' must be on or before 'to'"));
    }

    @Test
    void returnsServiceUnavailableWhenDatabaseAccessFails() throws Exception {
        when(dashboardService.getStats(any(), any(), isNull()))
                .thenThrow(new DataAccessResourceFailureException("database offline"));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Database is temporarily unavailable"));
    }
}
