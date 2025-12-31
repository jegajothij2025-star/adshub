package com.ads.adshub.controller;

import java.io.IOException;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ads.adshub.config.LoggingAspect;
import com.ads.adshub.model.ApiResponse;
import com.ads.adshub.model.ApiResponseUtil;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.request.DistrictRequestDTO;
import com.ads.adshub.response.DistrictResponseDTO;
import com.ads.adshub.service.DistrictService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/districts")
public class DistrictController {

    private final DistrictService districtService;

	private final LoggingAspect aspect; 
	 
    public DistrictController(DistrictService districtService, LoggingAspect aspect) {
        this.districtService = districtService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<DistrictResponseDTO>> createDistrict(
			@Valid @RequestBody DistrictRequestDTO districtRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		DistrictResponseDTO districtResponse = districtService.createDistrict(districtRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(districtResponse, "District created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<DistrictResponseDTO>> updateDistrict(@PathVariable Long id,
			@RequestBody DistrictRequestDTO districtRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		DistrictResponseDTO districtResponse = districtService.updateDistrict(id, districtRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(districtResponse, "District updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteDistrict(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = districtService.deleteDistrict(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "District deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("District not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<DistrictResponseDTO>> findDistrictById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		DistrictResponseDTO districtResponse = districtService.findDistrictById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(districtResponse, "Found District Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<DistrictResponseDTO>> esFindDistrictById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		DistrictResponseDTO districtResponseDto;
		ApiResponse<DistrictResponseDTO> apiDistrictES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			districtResponseDto = districtService.esFindDistrictById(id);
			apiDistrictES = ApiResponseUtil.success(districtResponseDto,
					"District data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			districtResponseDto = districtService.findDistrictById(id);
			
			apiDistrictES = ApiResponseUtil.success(districtResponseDto,
					"District data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiDistrictES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<DistrictResponseDTO>>> esFindAllDistricts(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<DistrictResponseDTO> districts;
		ApiResponse<List<DistrictResponseDTO>> apiDistrictES;
		try {
			districts = districtService.esFindAllDistricts(pageDTO);
			long count = districtService.countAllDistrictsFromES();
			apiDistrictES = ApiResponseUtil.success(districts,
					"District data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			districts = districtService.findAllDistricts(pageDTO);
			long count = districtService.countAllDistrictsFromDB();
			apiDistrictES = ApiResponseUtil.success(districts, "District data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiDistrictES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<DistrictResponseDTO>>> findAllDistricts(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<DistrictResponseDTO> districtsResponse = districtService.findAllDistricts(pageDTO);
		long count = districtService.countAllDistrictsFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(districtsResponse, "Success", null, "DB", count, correlationId));
	}

}
