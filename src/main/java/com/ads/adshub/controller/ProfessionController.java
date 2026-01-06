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
import com.ads.adshub.request.ProfessionRequestDTO;
import com.ads.adshub.response.ProfessionResponseDTO;
import com.ads.adshub.service.ProfessionService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/profession")
public class ProfessionController {

    private final ProfessionService professionService;

	private final LoggingAspect aspect; 
	 
    public ProfessionController(ProfessionService professionService, LoggingAspect aspect) {
        this.professionService = professionService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<ProfessionResponseDTO>> createProfession(
			@Valid @RequestBody ProfessionRequestDTO professionRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		ProfessionResponseDTO professionResponse = professionService.createProfession(professionRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(professionResponse, "Profession created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<ProfessionResponseDTO>> updateProfession(@PathVariable Long id,
			@RequestBody ProfessionRequestDTO professionRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		ProfessionResponseDTO professionResponse = professionService.updateProfession(id, professionRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(professionResponse, "Profession updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteProfession(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = professionService.deleteProfession(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Profession deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Profession not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<ProfessionResponseDTO>> findProfessionById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		ProfessionResponseDTO professionResponse = professionService.findProfessionById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(professionResponse, "Found Profession Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<ProfessionResponseDTO>> esFindProfessionById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		ProfessionResponseDTO professionResponseDto;
		ApiResponse<ProfessionResponseDTO> apiProfessionES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			professionResponseDto = professionService.esFindProfessionById(id);
			apiProfessionES = ApiResponseUtil.success(professionResponseDto,
					"Profession data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			professionResponseDto = professionService.findProfessionById(id);
			
			apiProfessionES = ApiResponseUtil.success(professionResponseDto,
					"Profession data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiProfessionES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<ProfessionResponseDTO>>> esFindAllProfession(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<ProfessionResponseDTO> profession;
		ApiResponse<List<ProfessionResponseDTO>> apiProfessionES;
		try {
			profession = professionService.esFindAllProfession(pageDTO);
			long count = professionService.countAllProfessionFromES();
			apiProfessionES = ApiResponseUtil.success(profession,
					"Profession data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			profession = professionService.findAllProfession(pageDTO);
			long count = professionService.countAllProfessionFromDB();
			apiProfessionES = ApiResponseUtil.success(profession, "Profession data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiProfessionES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<ProfessionResponseDTO>>> findAllProfession(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<ProfessionResponseDTO> professionResponse = professionService.findAllProfession(pageDTO);
		long count = professionService.countAllProfessionFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(professionResponse, "Success", null, "DB", count, correlationId));
	}

}

