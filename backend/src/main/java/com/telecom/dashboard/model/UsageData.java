package com.telecom.dashboard.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "usage_data")
@Getter
public class UsageData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`timestamp`", nullable = false)
    private LocalDateTime recordedAt;
    @Column(name = "data_usage", nullable = false)
    private double dataUsedMb;
    @Column(name = "call_duration", nullable = false)
    private int callMinutes;
    @Column(name = "sms_count", nullable = false)
    private int smsCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    protected UsageData() {
    }

    public UsageData(LocalDateTime recordedAt, double dataUsedMb, int callMinutes,
                     int smsCount, Subscriber subscriber) {
        this.recordedAt = recordedAt;
        this.dataUsedMb = dataUsedMb;
        this.callMinutes = callMinutes;
        this.smsCount = smsCount;
        this.subscriber = subscriber;
    }
}
