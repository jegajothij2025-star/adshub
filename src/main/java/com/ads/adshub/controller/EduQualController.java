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
import com.ads.adshub.request.EduQualRequestDTO;
import com.ads.adshub.response.EduQualResponseDTO;
import com.ads.adshub.service.EduQualService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/eduqual")
public class EduQualController {

    private final EduQualService eduqualService;

	private final LoggingAspect aspect; 
	 
    public EduQualController(EduQualService eduqualService, LoggingAspect aspect) {
        this.eduqualService = eduqualService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<EduQualResponseDTO>> createEduQual(
			@Valid @RequestBody EduQualRequestDTO eduqualRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		EduQualResponseDTO eduqualResponse = eduqualService.createEduQual(eduqualRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(eduqualResponse, "Education Qualification created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<EduQualResponseDTO>> updateEduQual(@PathVariable Long id,
			@RequestBody EduQualRequestDTO eduqualRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		EduQualResponseDTO eduqualResponse = eduqualService.updateEduQual(id, eduqualRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(eduqualResponse, "Education Qualification updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteEduQual(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = eduqualService.deleteEduQual(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Education Qualification deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Education Qualification not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<EduQualResponseDTO>> findEduQualById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		EduQualResponseDTO eduqualResponse = eduqualService.findEduQualById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(eduqualResponse, "Found Education Qualification Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<EduQualResponseDTO>> esFindEduQualById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		EduQualResponseDTO eduqualResponseDto;
		ApiResponse<EduQualResponseDTO> apiEduQualES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			eduqualResponseDto = eduqualService.esFindEduQualById(id);
			apiEduQualES = ApiResponseUtil.success(eduqualResponseDto,
					"Education Qualification data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			eduqualResponseDto = eduqualService.findEduQualById(id);
			
			apiEduQualES = ApiResponseUtil.success(eduqualResponseDto,
					"Education Qualification data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiEduQualES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<EduQualResponseDTO>>> esFindAllEduQual(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<EduQualResponseDTO> eduqual;
		ApiResponse<List<EduQualResponseDTO>> apiEduQualES;
		try {
			eduqual = eduqualService.esFindAllEduQual(pageDTO);
			long count = eduqualService.countAllEduQualFromES();
			apiEduQualES = ApiResponseUtil.success(eduqual,
					"Education Qualification data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			eduqual = eduqualService.findAllEduQual(pageDTO);
			long count = eduqualService.countAllEduQualFromDB();
			apiEduQualES = ApiResponseUtil.success(eduqual, "Education Qualification data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiEduQualES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<EduQualResponseDTO>>> findAllEduQual(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<EduQualResponseDTO> eduqualResponse = eduqualService.findAllEduQual(pageDTO);
		long count = eduqualService.countAllEduQualFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(eduqualResponse, "Success", null, "DB", count, correlationId));
	}

}

