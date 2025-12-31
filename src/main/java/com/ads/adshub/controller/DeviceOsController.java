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
import com.ads.adshub.request.DeviceOsRequestDTO;
import com.ads.adshub.response.DeviceOsResponseDTO;
import com.ads.adshub.service.DeviceOsService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/deviceos")
public class DeviceOsController {

    private final DeviceOsService deviceosService;

	public DeviceOsController(DeviceOsService deviceosService, LoggingAspect aspect) {
        this.deviceosService = deviceosService;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<DeviceOsResponseDTO>> createDeviceOs(
			@Valid @RequestBody DeviceOsRequestDTO deviceosRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		DeviceOsResponseDTO deviceosResponse = deviceosService.createDeviceOs(deviceosRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(deviceosResponse, "Device OS created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<DeviceOsResponseDTO>> updateDeviceOs(@PathVariable Long id,
			@RequestBody DeviceOsRequestDTO deviceosRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		DeviceOsResponseDTO deviceosResponse = deviceosService.updateDeviceOs(id, deviceosRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(deviceosResponse, "Country updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteDeviceOs(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = deviceosService.deleteDeviceOs(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Device OS deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Device OS not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<DeviceOsResponseDTO>> findDeviceOsById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		DeviceOsResponseDTO deviceosResponse = deviceosService.findDeviceOsById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(deviceosResponse, "Found Device OS Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<DeviceOsResponseDTO>> esFindDeviceOsById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		DeviceOsResponseDTO deviceosResponseDto;
		ApiResponse<DeviceOsResponseDTO> apiDeviceOsES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			deviceosResponseDto = deviceosService.esFindDeviceOsById(id);
			apiDeviceOsES = ApiResponseUtil.success(deviceosResponseDto,
					"Device OS data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			deviceosResponseDto = deviceosService.findDeviceOsById(id);
			
			apiDeviceOsES = ApiResponseUtil.success(deviceosResponseDto,
					"Device OS data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiDeviceOsES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<DeviceOsResponseDTO>>> esFindAllDeviceOs(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<DeviceOsResponseDTO> deviceos;
		ApiResponse<List<DeviceOsResponseDTO>> apiDeviceOsES;
		try {
			deviceos = deviceosService.esFindAllDeviceOs(pageDTO);
			long count = deviceosService.countAllDeviceOsFromES();
			apiDeviceOsES = ApiResponseUtil.success(deviceos,
					"Device OS data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			deviceos = deviceosService.findAllDeviceOs(pageDTO);
			long count = deviceosService.countAllDeviceOsFromDB();
			apiDeviceOsES = ApiResponseUtil.success(deviceos, "Device OS data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiDeviceOsES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<DeviceOsResponseDTO>>> findAllDeviceOs(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<DeviceOsResponseDTO> deviceosResponse = deviceosService.findAllDeviceOs(pageDTO);
		long count = deviceosService.countAllDeviceOsFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(deviceosResponse, "Success", null, "DB", count, correlationId));
	}

}

