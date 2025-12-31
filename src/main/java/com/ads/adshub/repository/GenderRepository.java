package com.ads.adshub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ads.adshub.entity.Gender;

@Repository
public interface GenderRepository extends JpaRepository<Gender, Long> {

    /* 🔍 Search by gender name (pagination support) */
    Page<Gender> findByGenderContainingIgnoreCase(String searchKey, Pageable pageable);

    /* 🔍 Find exact gender */
    Optional<Gender> findByGender(String gender);

    /* 📊 Get distinct gender codes */
    @Query("SELECT DISTINCT g.genderCode FROM Gender g")
    List<String> findDistinctGenderCodes();
}
