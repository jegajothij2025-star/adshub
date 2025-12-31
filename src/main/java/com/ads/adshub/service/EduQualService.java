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

import com.ads.adshub.entity.EduQual;
import com.ads.adshub.entity.document.EduQualDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.EduQualRepository;
import com.ads.adshub.request.EduQualRequestDTO;
import com.ads.adshub.response.EduQualResponseDTO;
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
public class EduQualService {

    private final EduQualRepository eduqualRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "states";

    public EduQualService(EduQualRepository eduqualRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.eduqualRepository = eduqualRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public EduQualResponseDTO createEduQual(EduQualRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    EduQual eduqual = EduQual.builder()
	    		.eduqualName(dto.getEduqualName())
	            .eduqualCode(dto.getEduqualCode())
	            .build();

	    EduQual savedEduQual = eduqualRepository.save(eduqual);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    EduQualDocument eduqualDoc = objectMapper.convertValue(savedEduQual, EduQualDocument.class);
	    eduqualDoc.setEduqualId(String.valueOf(eduqualDoc.getEduqualId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(eduqualDoc.getEduqualId()).document(eduqualDoc)));

	    // Convert back to response DTO in local time
	    EduQualResponseDTO responseDTO = objectMapper.convertValue(eduqualDoc, EduQualResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public EduQualResponseDTO updateEduQual(Long eduqualId, EduQualRequestDTO dto) {
		EduQual eduqual = eduqualRepository.findById(eduqualId)
				.orElseThrow(() -> new RuntimeException("Education Qualification not found with ID: " + eduqualId));

		eduqual.setEduqualName(dto.getEduqualName());
		eduqual.setEduqualCode(dto.getEduqualCode());

		EduQual updatedEduQual = eduqualRepository.save(eduqual);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		EduQualDocument eduqualDoc = objectMapper.convertValue(updatedEduQual, EduQualDocument.class);
		eduqualDoc.setEduqualId(String.valueOf(updatedEduQual.getEduqualId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(eduqualDoc.getEduqualId()).document(eduqualDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 EduQualResponseDTO responseDTO = objectMapper.convertValue(eduqualDoc, EduQualResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteEduQual(Long id) throws IOException {
		if (eduqualRepository.existsById(id)) {
			eduqualRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "eduqualificationCache", keyGenerator = "customKeyGenerator")
	public List<EduQualResponseDTO> findAllEduQual(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<EduQual> eduqualPage = eduqualRepository.findAll(pageable);

		List<EduQualResponseDTO> eduqual = eduqualPage.getContent().stream()
		        .map(cou -> objectMapper.convertValue(cou, EduQualResponseDTO.class))
		        .collect(Collectors.toList());

		return eduqual.stream().map(edu -> objectMapper.convertValue(edu, EduQualResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<EduQualResponseDTO> esFindAllEduQual(PageDTO pageDTO)

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

		SearchResponse<EduQualDocument> response = elasticClient.search(searchRequest, EduQualDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), EduQualResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllEduQualFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllEduQualFromDB() {
		return eduqualRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public EduQualResponseDTO esFindEduQualById(Long eduqualId) throws IOException {
		SearchResponse<EduQualDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("eduqualId").value(String.valueOf(eduqualId)))),
				EduQualDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), EduQualResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "eduqualificationCache", keyGenerator = "customKeyGenerator")
	public EduQualResponseDTO findEduQualById(Long eduqualId) throws JsonProcessingException {
		EduQual eduqual = eduqualRepository.findById(eduqualId)
				.orElseThrow(() -> new RuntimeException("Education Qualification not found with ID: " + eduqualId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(eduqual, EduQualResponseDTO.class);
	}
}

