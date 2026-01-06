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
import com.ads.adshub.request.LanguageRequestDTO;
import com.ads.adshub.response.LanguageResponseDTO;
import com.ads.adshub.service.LanguageService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/language")
public class LanguageController {

    private final LanguageService languageService;

	private final LoggingAspect aspect; 
	 
    public LanguageController(LanguageService languageService, LoggingAspect aspect) {
        this.languageService = languageService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<LanguageResponseDTO>> createLanguage(
			@Valid @RequestBody LanguageRequestDTO languageRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		LanguageResponseDTO languageResponse = languageService.createlanguage(languageRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(languageResponse, "Language created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<LanguageResponseDTO>> updateLanguage(@PathVariable Long id,
			@RequestBody LanguageRequestDTO languageRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		LanguageResponseDTO languageResponse = languageService.updateLanguage(id, languageRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(languageResponse, "Language updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteLanguage(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = languageService.deleteLanguage(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Language deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Language not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<LanguageResponseDTO>> findLanguageById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		LanguageResponseDTO languageResponse = languageService.findLanguageById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(languageResponse, "Found Language Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<LanguageResponseDTO>> esFindLanguageById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		LanguageResponseDTO languageResponseDto;
		ApiResponse<LanguageResponseDTO> apiLanguageES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			languageResponseDto = languageService.esFindLanguageById(id);
			apiLanguageES = ApiResponseUtil.success(languageResponseDto,
					"Language data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			languageResponseDto = languageService.findLanguageById(id);
			
			apiLanguageES = ApiResponseUtil.success(languageResponseDto,
					"Language data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiLanguageES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<LanguageResponseDTO>>> esFindAllLanguage(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<LanguageResponseDTO> language;
		ApiResponse<List<LanguageResponseDTO>> apiLanguageES;
		try {
			language = languageService.esFindAllLanguage(pageDTO);
			long count = languageService.countAllLanguageFromES();
			apiLanguageES = ApiResponseUtil.success(language,
					"Language data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			language = languageService.findAllLanguage(pageDTO);
			long count = languageService.countAllLanguageFromDB();
			apiLanguageES = ApiResponseUtil.success(language, "Language data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiLanguageES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<LanguageResponseDTO>>> findAllLanguage(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<LanguageResponseDTO> languageResponse = languageService.findAllLanguage(pageDTO);
		long count = languageService.countAllLanguageFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(languageResponse, "Success", null, "DB", count, correlationId));
	}

}

