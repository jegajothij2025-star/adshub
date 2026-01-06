package com.ads.adshub.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "profession")
public class Profession {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "profession_id")
private Long professionId;

@NotBlank
@Column(name = "profession_name", nullable = false)
private String professionName;

}
