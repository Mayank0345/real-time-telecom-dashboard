package com.telecom.dashboard.dto;

public record DashboardStats(long totalSubscribers, long activeSubscribers,
                             double totalDataUsage, double averageCallDuration, long totalSmsCount,
                             long totalOutages, long activeOutages) {
}
