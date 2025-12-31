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
import com.ads.adshub.request.StateRequestDTO;
import com.ads.adshub.response.StateResponseDTO;
import com.ads.adshub.service.StateService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/states")
public class StateController {

    private final StateService stateService;

	private final LoggingAspect aspect; 
	 
    public StateController(StateService stateService, LoggingAspect aspect) {
        this.stateService = stateService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<StateResponseDTO>> createState(
			@Valid @RequestBody StateRequestDTO stateRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		StateResponseDTO stateResponse = stateService.createState(stateRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(stateResponse, "State created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<StateResponseDTO>> updateState(@PathVariable Long id,
			@RequestBody StateRequestDTO stateRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		StateResponseDTO stateResponse = stateService.updateState(id, stateRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(stateResponse, "State updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteState(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = stateService.deleteState(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "State deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("State not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<StateResponseDTO>> findStateById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		StateResponseDTO stateResponse = stateService.findStateById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(stateResponse, "Found State Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<StateResponseDTO>> esFindStateById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		StateResponseDTO stateResponseDto;
		ApiResponse<StateResponseDTO> apiStateES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			stateResponseDto = stateService.esFindStateById(id);
			apiStateES = ApiResponseUtil.success(stateResponseDto,
					"State data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			stateResponseDto = stateService.findStateById(id);
			
			apiStateES = ApiResponseUtil.success(stateResponseDto,
					"State data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiStateES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<StateResponseDTO>>> esFindAllStates(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<StateResponseDTO> states;
		ApiResponse<List<StateResponseDTO>> apiStateES;
		try {
			states = stateService.esFindAllStates(pageDTO);
			long count = stateService.countAllStatesFromES();
			apiStateES = ApiResponseUtil.success(states,
					"State data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			states = stateService.findAllStates(pageDTO);
			long count = stateService.countAllStatesFromDB();
			apiStateES = ApiResponseUtil.success(states, "State data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiStateES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<StateResponseDTO>>> findAllStates(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<StateResponseDTO> statesResponse = stateService.findAllStates(pageDTO);
		long count = stateService.countAllStatesFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(statesResponse, "Success", null, "DB", count, correlationId));
	}

}

