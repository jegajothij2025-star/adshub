package com.ads.adshub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "currency")
public class Currency {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "currency_id")
private Long currencyId;

@NotBlank
@Column(name = "currency_name", nullable = false)
private String currencyName;

@NotBlank
@Column(name = "currency_code", nullable = false, unique = true)
private String currencyCode;

// ✅ FOREIGN KEY MAPPING
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(
    name = "country_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_currency_country")
)
private Country country;

}
