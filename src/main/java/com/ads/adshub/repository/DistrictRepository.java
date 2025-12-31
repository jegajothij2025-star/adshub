package com.ads.adshub.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import com.ads.adshub.entity.District;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long>{

}
