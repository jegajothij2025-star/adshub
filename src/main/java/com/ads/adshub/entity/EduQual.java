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
@Table(name = "eduqualification")
public class EduQual {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "edu_qual_id")
private Long eduqualId;

@NotBlank
@Column(name = "edu_qual_name", nullable = false)
private String eduqualName;

@NotBlank
@Column(name = "edu_qual_code", nullable = false, unique = true)
private String eduqualCode;

}
