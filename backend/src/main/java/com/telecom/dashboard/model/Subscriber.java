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
@Table(name = "subscribers")
@Getter
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(nullable = false, length = 50)
    private String plan;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    protected Subscriber() {
    }

    public Subscriber(String name, String phoneNumber, String status, String plan,
                      LocalDateTime createdAt, Region region) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.plan = plan;
        this.createdAt = createdAt;
        this.region = region;
    }

}
