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
@Table(name = "deviceos")
public class DeviceOs {


@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "device_os_id")
private Long deviceosId;

@NotBlank
@Column(name = "device_os", nullable = false)
private String deviceos;

@NotBlank
@Column(name = "device_os_code", nullable = false, unique = true)
private String deviceosCode;

}
