package com.ads.adshub.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "country")
public class Country {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "country_id")
private Long countryId;

@NotBlank
@Column(name = "country_name", nullable = false)
private String countryName;

@NotBlank
@Column(name = "country_code", nullable = false, unique = true)
private String countryCode;

}
