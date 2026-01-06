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

import com.ads.adshub.entity.Language;
import com.ads.adshub.entity.document.LanguageDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.LanguageRepository;
import com.ads.adshub.request.LanguageRequestDTO;
import com.ads.adshub.response.LanguageResponseDTO;
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
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "languages";

    public LanguageService(LanguageRepository languageRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.languageRepository = languageRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public LanguageResponseDTO createlanguage(LanguageRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    Language language = Language.builder()
	    		.languageName(dto.getLanguageName())
	            .languageCode(dto.getLanguageCode())
	            .build();

	    Language savedLanguage = languageRepository.save(language);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    LanguageDocument languageDoc = objectMapper.convertValue(savedLanguage, LanguageDocument.class);
	    languageDoc.setLanguageId(String.valueOf(languageDoc.getLanguageId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(languageDoc.getLanguageId()).document(languageDoc)));

	    // Convert back to response DTO in local time
	    LanguageResponseDTO responseDTO = objectMapper.convertValue(languageDoc, LanguageResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public LanguageResponseDTO updateLanguage(Long languageId, LanguageRequestDTO dto) {
		Language language = languageRepository.findById(languageId)
				.orElseThrow(() -> new RuntimeException("Language not found with ID: " + languageId));

		language.setLanguageName(dto.getLanguageName());
		language.setLanguageCode(dto.getLanguageCode());

		Language updatedLanguage = languageRepository.save(language);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		LanguageDocument languageDoc = objectMapper.convertValue(updatedLanguage, LanguageDocument.class);
		languageDoc.setLanguageId(String.valueOf(updatedLanguage.getLanguageId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(languageDoc.getLanguageId()).document(languageDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 LanguageResponseDTO responseDTO = objectMapper.convertValue(languageDoc, LanguageResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteLanguage(Long id) throws IOException {
		if (languageRepository.existsById(id)) {
			languageRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "languageCache", keyGenerator = "customKeyGenerator")
	public List<LanguageResponseDTO> findAllLanguage(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<Language> languagePage = languageRepository.findAll(pageable);

		List<LanguageResponseDTO> language = languagePage.getContent().stream()
		        .map(cou -> objectMapper.convertValue(cou, LanguageResponseDTO.class))
		        .collect(Collectors.toList());

		return language.stream().map(lan -> objectMapper.convertValue(lan, LanguageResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<LanguageResponseDTO> esFindAllLanguage(PageDTO pageDTO)

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

		SearchResponse<LanguageDocument> response = elasticClient.search(searchRequest, LanguageDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), LanguageResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllLanguageFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllLanguageFromDB() {
		return languageRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public LanguageResponseDTO esFindLanguageById(Long languageId) throws IOException {
		SearchResponse<LanguageDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("languageId").value(String.valueOf(languageId)))),
				LanguageDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), LanguageResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "languageCache", keyGenerator = "customKeyGenerator")
	public LanguageResponseDTO findLanguageById(Long languageId) throws JsonProcessingException {
		Language language = languageRepository.findById(languageId)
				.orElseThrow(() -> new RuntimeException("Language not found with ID: " + languageId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(language, LanguageResponseDTO.class);
	}
}

