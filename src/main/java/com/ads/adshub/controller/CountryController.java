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
import com.ads.adshub.request.CountryRequestDTO;
import com.ads.adshub.response.CountryResponseDTO;
import com.ads.adshub.service.CountryService;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/country")
public class CountryController {

    private final CountryService countryService;

	private final LoggingAspect aspect; 
	 
    public CountryController(CountryService countryService, LoggingAspect aspect) {
        this.countryService = countryService;
        this.aspect = aspect;
    }

    /* ---------------- CREATE ---------------- */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CountryResponseDTO>> createCountry(
			@Valid @RequestBody CountryRequestDTO countryRequestDto, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		CountryResponseDTO countryResponse = countryService.createCountry(countryRequestDto);
			//aspect.LogCodeExecution("Fetching data");
		return new ResponseEntity<>(
				ApiResponseUtil.success(countryResponse, "Country created successfully", null, "DB and ES",1, correlationId),
				HttpStatus.CREATED);

	}

    /* ---------------- UPDATE ---------------- */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CountryResponseDTO>> updateCountry(@PathVariable Long id,
			@RequestBody CountryRequestDTO countryRequestDto, HttpServletRequest request) {
		String correlationId = (String) request.getAttribute("correlationId");
		CountryResponseDTO countryResponse = countryService.updateCountry(id, countryRequestDto);
		return new ResponseEntity<>(
				ApiResponseUtil.success(countryResponse, "Country updated successfully", null, "DB and ES",1, correlationId),
				HttpStatus.ACCEPTED);
	}

    /* ---------------- DELETE ---------------- */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteCountry(@PathVariable Long id, HttpServletRequest request) throws IOException {
		boolean isDeleted = countryService.deleteCountry(id);
		String correlationId = (String) request.getAttribute("correlationId");
		if (isDeleted) {
			return ResponseEntity.status(HttpStatus.OK)
					.body(ApiResponseUtil.success(null, "Country deleted successfully", null, "DB and ES",1, correlationId));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponseUtil.failure("Country not found or could not be deleted", "DB and ES"));
		}
	}

    /* ---------------- FIND BY ID (DB) ---------------- */
    @GetMapping("/find-by-id/{id}")
    public ResponseEntity<ApiResponse<CountryResponseDTO>> findCountryById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		//aspect.LogCodeExecution("Fetching data");
		CountryResponseDTO countryResponse = countryService.findCountryById(id);
		
		return ResponseEntity.ok(ApiResponseUtil.success(countryResponse, "Found Country Successfully", null, "DB",1, correlationId));
	}
    
    /* ---------------- FIND BY ID (ES with fallback) ---------------- */
    @GetMapping("/es/find-by-id/{id}")
    public ResponseEntity<ApiResponse<CountryResponseDTO>> esFindCountryById(@PathVariable Long id, HttpServletRequest request) throws JsonProcessingException {
		String correlationId = (String) request.getAttribute("correlationId");
		CountryResponseDTO countryResponseDto;
		ApiResponse<CountryResponseDTO> apiCountryES;
		try {
			//aspect.LogCodeExecution("Fetching data");
			countryResponseDto = countryService.esFindCountryById(id);
			apiCountryES = ApiResponseUtil.success(countryResponseDto,
					"Country data retrieved successfully from Elasticsearch", null, "ES",1, correlationId);
		} catch (Exception e) {
			countryResponseDto = countryService.findCountryById(id);
			
			apiCountryES = ApiResponseUtil.success(countryResponseDto,
					"Country data retrieved successfully from Database", e.getMessage(), "DB",1, correlationId);
		}
		
		return ResponseEntity.ok(apiCountryES);
	}

    /* ---------------- FIND ALL (ES with fallback) ---------------- */
    @PostMapping("/es/find-all")
    public ResponseEntity<ApiResponse<List<CountryResponseDTO>>> esFindAllCountries(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<CountryResponseDTO> countries;
		ApiResponse<List<CountryResponseDTO>> apiCountryES;
		try {
			countries = countryService.esFindAllCountries(pageDTO);
			long count = countryService.countAllCountriesFromES();
			apiCountryES = ApiResponseUtil.success(countries,
					"Country data retrieved successfully from Elasticsearch", null, "ES", count, correlationId);
		} catch (Exception e) {
			countries = countryService.findAllCountries(pageDTO);
			long count = countryService.countAllCountriesFromDB();
			apiCountryES = ApiResponseUtil.success(countries, "Country data retrieved successfully from Database",
					e.getMessage(), "DB", count, correlationId);
		}
		return ResponseEntity.ok(apiCountryES);
	}

    /* ---------------- FIND ALL (DB) ---------------- */
    @PostMapping("/find-all")
	public ResponseEntity<ApiResponse<List<CountryResponseDTO>>> findAllCountries(@RequestBody PageDTO pageDTO, HttpServletRequest request) throws IOException {
		String correlationId = (String) request.getAttribute("correlationId");
		List<CountryResponseDTO> countiresResponse = countryService.findAllCountries(pageDTO);
		long count = countryService.countAllCountriesFromDB();
		return ResponseEntity.ok(ApiResponseUtil.success(countiresResponse, "Success", null, "DB", count, correlationId));
	}

}

