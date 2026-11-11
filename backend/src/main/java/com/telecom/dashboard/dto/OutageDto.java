package com.telecom.dashboard.dto;

import java.time.LocalDateTime;

public record OutageDto(Long id, Long regionId, String regionName, Long durationMinutes,
                        String reason, LocalDateTime startTime, LocalDateTime endTime) {
}
