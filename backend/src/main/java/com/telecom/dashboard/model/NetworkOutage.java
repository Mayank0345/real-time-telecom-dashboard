package com.telecom.dashboard.model;

import lombok.Getter;
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

@Entity
@Table(name = "network_outages")
@Getter
public class NetworkOutage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reason", nullable = false)
    private String description;
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "end_time")
    private LocalDateTime resolvedAt;
    @Column(name = "duration_minutes")
    private Long durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    protected NetworkOutage() {
    }

    public NetworkOutage(String description, LocalDateTime startedAt, LocalDateTime resolvedAt,
                         Region region) {
        this.description = description;
        this.startedAt = startedAt;
        this.resolvedAt = resolvedAt;
        this.durationMinutes = resolvedAt == null
                ? null
                : java.time.Duration.between(startedAt, resolvedAt).toMinutes();
        this.region = region;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
        this.durationMinutes = resolvedAt == null
                ? null
                : java.time.Duration.between(startedAt, resolvedAt).toMinutes();
    }
}
