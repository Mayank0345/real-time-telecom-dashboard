package com.telecom.dashboard.dto;

public record SubscriberDto(Long id, String name, String phoneNumber, String status, String plan,
                            Long regionId, String regionName) {
}
