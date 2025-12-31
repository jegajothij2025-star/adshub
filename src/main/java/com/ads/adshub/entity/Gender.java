package com.ads.adshub.entity;

import com.ads.adshub.entity.Gender;

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
@Table(name = "gender")
public class Gender {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "gender_id")
private Long genderId;

@NotBlank
@Column(name = "gender", nullable = false)
private String gender;

@NotBlank
@Column(name = "gender_code", nullable = false, unique = true)
private String genderCode;



}
