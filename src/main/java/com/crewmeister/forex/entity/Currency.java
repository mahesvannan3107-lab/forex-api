package com.crewmeister.forex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "currencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 3)
    private String code;

    @Column(name = "numeric_code", length = 3)
    private String numericCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 10)
    private String symbol;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(columnDefinition = "INT DEFAULT 2")
    private Integer decimals;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer rounding;

    @Column(name = "symbol_position", length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'before'")
    private String symbolPosition;

    @Column(name = "decimal_separator", length = 5, columnDefinition = "VARCHAR(5) DEFAULT '.'")
    private String decimalSeparator;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}