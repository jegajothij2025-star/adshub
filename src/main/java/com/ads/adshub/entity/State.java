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
@Table(name = "state")
public class State {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "state_id")
private Long stateId;

@NotBlank
@Column(name = "state_name", nullable = false)
private String stateName;

@NotBlank
@Column(name = "state_code", nullable = false, unique = true)
private String stateCode;

@NotBlank
@Column(name = "country_code", nullable = false, unique = true)
private String countryCode;



}
