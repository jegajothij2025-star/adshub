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
@Table(name = "language")
public class Language {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "language_id")
private Long languageId;

@NotBlank
@Column(name = "language_name", nullable = false)
private String languageName;

@NotBlank
@Column(name = "language_code", nullable = false, unique = true)
private String languageCode;

}
