package com.telecom.dashboard.model;

import lombok.Getter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "regions")
@Getter
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;
    @Column(nullable = false, length = 20, unique = true)
    private String code;

    protected Region() {
    }

    public Region(String name, String code) {
        this.name = name;
        this.code = code;
    }

}
