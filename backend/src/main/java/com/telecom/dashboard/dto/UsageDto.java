package com.telecom.dashboard.dto;

import java.time.LocalDateTime;

public record UsageDto(Long id, Long subscriberId, String subscriberName, Long regionId,
                       String regionName, double dataUsage, int callDuration, int smsCount,
                       LocalDateTime timestamp) {
}
