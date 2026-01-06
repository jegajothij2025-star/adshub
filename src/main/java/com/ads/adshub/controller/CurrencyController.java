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
import com.ads.adshub.request.CurrencyRequestDTO;
import com.ads.adshub.response.CurrencyResponseDTO;
import com.ads.adshub.service.CurrencyService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/currency")
public class CurrencyController {

    private final CurrencyService currencyService;

	private final LoggingAspect aspect; 
	 
    public CurrencyController(CurrencyService currencyService, LoggingAspect aspect) {
        this.currencyService = currencyService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CurrencyResponseDTO>> createCurrency(
			@Valid @RequestBody CurrencyRequestDTO currencyRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		CurrencyResponseDTO currencyResponse = currencyService.createCurrency(currencyRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(currencyResponse, "Currency created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponseDTO>> updateCurrency(@PathVariable Long id,
			@RequestBody CurrencyRequestDTO currencyRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		CurrencyResponseDTO currencyResponse = currencyService.updateCurrency(id, currencyRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(currencyResponse, "Currency updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteCurrency(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = currencyService.deleteCurrency(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Currency deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Currency not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponseDTO>> findCurrencyById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		CurrencyResponseDTO currencyResponse = currencyService.findCurrencyById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(currencyResponse, "Found Currency Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponseDTO>> esFindCurrencyById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		CurrencyResponseDTO currencyResponseDto;
		ApiResponse<CurrencyResponseDTO> apiCurrencyES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			currencyResponseDto = currencyService.esFindCurrencyById(id);
			apiCurrencyES = ApiResponseUtil.success(currencyResponseDto,
					"Currency data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			currencyResponseDto = currencyService.findCurrencyById(id);
			
			apiCurrencyES = ApiResponseUtil.success(currencyResponseDto,
					"Currency data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiCurrencyES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<CurrencyResponseDTO>>> esFindAllCurrency(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<CurrencyResponseDTO> currency;
		ApiResponse<List<CurrencyResponseDTO>> apiCurrencyES;
		try {
			currency = currencyService.esFindAllCurrency(pageDTO);
			long count = currencyService.countAllCurrencyFromES();
			apiCurrencyES = ApiResponseUtil.success(currency,
					"Currency data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			currency = currencyService.findAllCurrency(pageDTO);
			long count = currencyService.countAllCurrencyFromDB();
			apiCurrencyES = ApiResponseUtil.success(currency, "Currency data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiCurrencyES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<CurrencyResponseDTO>>> findAllCurrency(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<CurrencyResponseDTO> currencyResponse = currencyService.findAllCurrency(pageDTO);
		long count = currencyService.countAllCurrencyFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(currencyResponse, "Success", null, "DB", count, correlationId));
	}

}

