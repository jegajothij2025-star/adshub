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
import com.ads.adshub.request.GenderRequestDTO;
import com.ads.adshub.response.GenderResponseDTO;
import com.ads.adshub.service.GenderService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/genders")
public class GenderController {

    private final GenderService genderService;

	private final LoggingAspect aspect; 
	 
    public GenderController(GenderService genderService, LoggingAspect aspect) {
        this.genderService = genderService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<GenderResponseDTO>> createGender(
			@Valid @RequestBody GenderRequestDTO genderRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		GenderResponseDTO genderResponse = genderService.createGender(genderRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(genderResponse, "Gender created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<GenderResponseDTO>> updateGender(@PathVariable Long id,
			@RequestBody GenderRequestDTO genderRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		GenderResponseDTO genderResponse = genderService.updateGender(id, genderRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(genderResponse, "Gender updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteGender(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = genderService.deleteGender(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Gender deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Gender not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<GenderResponseDTO>> findGenderById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		GenderResponseDTO genderResponse = genderService.findGenderById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(genderResponse, "Found Gender Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<GenderResponseDTO>> esFindGenderById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		GenderResponseDTO genderResponseDto;
		ApiResponse<GenderResponseDTO> apiGenderES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			genderResponseDto = genderService.esFindGenderById(id);
			apiGenderES = ApiResponseUtil.success(genderResponseDto,
					"Gender data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			genderResponseDto = genderService.findGenderById(id);
			
			apiGenderES = ApiResponseUtil.success(genderResponseDto,
					"Gender data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiGenderES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<GenderResponseDTO>>> esFindAllGenders(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<GenderResponseDTO> genders;
		ApiResponse<List<GenderResponseDTO>> apiGenderES;
		try {
			genders = genderService.esFindAllGenders(pageDTO);
			long count = genderService.countAllGendersFromES();
			apiGenderES = ApiResponseUtil.success(genders,
					"Gender data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			genders = genderService.findAllGenders(pageDTO);
			long count = genderService.countAllGendersFromDB();
			apiGenderES = ApiResponseUtil.success(genders, "Gender data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiGenderES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<GenderResponseDTO>>> findAllGenders(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<GenderResponseDTO> gendersResponse = genderService.findAllGenders(pageDTO);
		long count = genderService.countAllGendersFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(gendersResponse, "Success", null, "DB", count, correlationId));
	}

}
