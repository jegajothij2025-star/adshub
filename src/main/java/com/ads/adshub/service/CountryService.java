package com.ads.adshub.service;

import java.io.IOException;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ads.adshub.entity.Country;
import com.ads.adshub.entity.document.CountryDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.CountryRepository;
import com.ads.adshub.request.CountryRequestDTO;
import com.ads.adshub.response.CountryResponseDTO;
import com.ads.adshub.utill.ElasticFieldUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

@Service
public class CountryService {

    private final CountryRepository countryRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "countries";

    public CountryService(CountryRepository countryRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.countryRepository = countryRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public CountryResponseDTO createCountry(CountryRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    Country country = Country.builder()
	    		.countryName(dto.getCountryName())
	            .countryCode(dto.getCountryCode())
	            .build();

	    Country savedCountry = countryRepository.save(country);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    CountryDocument countryDoc = objectMapper.convertValue(savedCountry, CountryDocument.class);
	    countryDoc.setCountryId(String.valueOf(countryDoc.getCountryId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(countryDoc.getCountryId()).document(countryDoc)));

	    // Convert back to response DTO in local time
	    CountryResponseDTO responseDTO = objectMapper.convertValue(countryDoc, CountryResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public CountryResponseDTO updateCountry(Long countryId, CountryRequestDTO dto) {
		Country country = countryRepository.findById(countryId)
				.orElseThrow(() -> new RuntimeException("Country not found with ID: " + countryId));

		country.setCountryName(dto.getCountryName());
		country.setCountryCode(dto.getCountryCode());

		Country updatedCountry = countryRepository.save(country);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		CountryDocument countryDoc = objectMapper.convertValue(updatedCountry, CountryDocument.class);
		countryDoc.setCountryId(String.valueOf(updatedCountry.getCountryId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(countryDoc.getCountryId()).document(countryDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 CountryResponseDTO responseDTO = objectMapper.convertValue(countryDoc, CountryResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteCountry(Long id) throws IOException {
		if (countryRepository.existsById(id)) {
			countryRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "countriesCache", keyGenerator = "customKeyGenerator")
	public List<CountryResponseDTO> findAllCountries(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<Country> countryPage = countryRepository.findAll(pageable);

		List<CountryResponseDTO> countries = countryPage.getContent().stream()
		        .map(cou -> objectMapper.convertValue(cou, CountryResponseDTO.class))
		        .collect(Collectors.toList());

		return countries.stream().map(cou -> objectMapper.convertValue(cou, CountryResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<CountryResponseDTO> esFindAllCountries(PageDTO pageDTO)

			throws IOException {

    	// 🔹 Resolve ES field for sorting
        String resolvedSortField = elasticfieldutil.resolveField(
                INDEX_NAME,
                pageDTO.getSortBy(),
                "SORT"
        );

		Query query =  Query.of(q -> q.matchAll(m -> m));

		SearchRequest searchRequest = SearchRequest.of(s -> s.index(INDEX_NAME).query(query)

				.from(pageDTO.getPage() * pageDTO.getSize()).size(pageDTO.getSize())

				.sort(srt -> srt.field(f -> f.field(resolvedSortField)

						.order(pageDTO.getSortDir().equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc))));

		SearchResponse<CountryDocument> response = elasticClient.search(searchRequest, CountryDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), CountryResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllCountriesFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllCountriesFromDB() {
		return countryRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public CountryResponseDTO esFindCountryById(Long countryId) throws IOException {
		SearchResponse<CountryDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("countryId").value(String.valueOf(countryId)))),
				CountryDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), CountryResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "countriesCache", keyGenerator = "customKeyGenerator")
	public CountryResponseDTO findCountryById(Long countryId) throws JsonProcessingException {
		Country country = countryRepository.findById(countryId)
				.orElseThrow(() -> new RuntimeException("Country not found with ID: " + countryId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(country, CountryResponseDTO.class);
	}
}

