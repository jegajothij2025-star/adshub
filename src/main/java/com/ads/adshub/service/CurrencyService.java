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
import com.ads.adshub.entity.Currency;
import com.ads.adshub.entity.document.CurrencyDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.CountryRepository;
import com.ads.adshub.repository.CurrencyRepository;
import com.ads.adshub.request.CurrencyRequestDTO;
import com.ads.adshub.response.CurrencyResponseDTO;
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
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CountryRepository countryRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "currencies";

    public CurrencyService(CurrencyRepository currencyRepository,
    					 CountryRepository countryRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.currencyRepository = currencyRepository;
        this.countryRepository = countryRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public CurrencyResponseDTO createCurrency(CurrencyRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    	
    	 // ✅ Load Country entity
    	Country country = countryRepository.findById(dto.getCountryId())
    	        .orElseThrow(() ->
    	            new RuntimeException("Invalid country ID: " + dto.getCountryId())
    	        );

	    Currency currency = Currency.builder()
	    		.currencyName(dto.getCurrencyName())
	            .currencyCode(dto.getCurrencyCode())
	            .country(country)
	            .build();

	    Currency savedCurrency = currencyRepository.save(currency);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    CurrencyDocument currencyDoc = objectMapper.convertValue(savedCurrency, CurrencyDocument.class);
	    currencyDoc.setCurrencyId(String.valueOf(currencyDoc.getCurrencyId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(currencyDoc.getCurrencyId()).document(currencyDoc)));

	    // Convert back to response DTO in local time
	    CurrencyResponseDTO responseDTO = objectMapper.convertValue(currencyDoc, CurrencyResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public CurrencyResponseDTO updateCurrency(Long currencyId, CurrencyRequestDTO dto) {
		Currency currency = currencyRepository.findById(currencyId)
				.orElseThrow(() -> new RuntimeException("Currencyy not found with ID: " + currencyId));
		
		//Load countryId
		Country country = countryRepository.findById(dto.getCountryId())
		        .orElseThrow(() ->
		            new RuntimeException("Invalid country ID: " + dto.getCountryId())
		        );

		currency.setCurrencyName(dto.getCurrencyName());
		currency.setCurrencyCode(dto.getCurrencyCode());
		currency.setCountry(country);

		Currency updatedCurrency = currencyRepository.save(currency);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		CurrencyDocument currencyDoc = objectMapper.convertValue(updatedCurrency, CurrencyDocument.class);
		currencyDoc.setCurrencyId(String.valueOf(updatedCurrency.getCurrencyId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(currencyDoc.getCurrencyId()).document(currencyDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 CurrencyResponseDTO responseDTO = objectMapper.convertValue(currencyDoc, CurrencyResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteCurrency(Long id) throws IOException {
		if (currencyRepository.existsById(id)) {
			currencyRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "currencyCache", keyGenerator = "customKeyGenerator")
	public List<CurrencyResponseDTO> findAllCurrency(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<Currency> currencyPage = currencyRepository.findAll(pageable);

		List<CurrencyResponseDTO> currencies = currencyPage.getContent().stream()
		        .map(cur -> objectMapper.convertValue(cur, CurrencyResponseDTO.class))
		        .collect(Collectors.toList());

		return currencies.stream().map(cur -> objectMapper.convertValue(cur, CurrencyResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<CurrencyResponseDTO> esFindAllCurrency(PageDTO pageDTO)

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

		SearchResponse<CurrencyDocument> response = elasticClient.search(searchRequest, CurrencyDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), CurrencyResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllCurrencyFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllCurrencyFromDB() {
		return currencyRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public CurrencyResponseDTO esFindCurrencyById(Long currencyId) throws IOException {
		SearchResponse<CurrencyDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("currencyId").value(String.valueOf(currencyId)))),
				CurrencyDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), CurrencyResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "currencyCache", keyGenerator = "customKeyGenerator")
	public CurrencyResponseDTO findCurrencyById(Long currencyId) throws JsonProcessingException {
		Currency currency = currencyRepository.findById(currencyId)
				.orElseThrow(() -> new RuntimeException("Currency not found with ID: " + currencyId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(currency, CurrencyResponseDTO.class);
	}
}

