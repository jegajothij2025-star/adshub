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

import com.ads.adshub.entity.Profession;
import com.ads.adshub.entity.document.ProfessionDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.ProfessionRepository;
import com.ads.adshub.request.ProfessionRequestDTO;
import com.ads.adshub.response.ProfessionResponseDTO;
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
public class ProfessionService {

    private final ProfessionRepository professionRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "professions";

    public ProfessionService(ProfessionRepository professionRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.professionRepository = professionRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public ProfessionResponseDTO createProfession(ProfessionRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    Profession profession = Profession.builder()
	    		.professionName(dto.getProfessionName())
	            .build();

	    Profession savedProfession = professionRepository.save(profession);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    ProfessionDocument professionDoc = objectMapper.convertValue(savedProfession, ProfessionDocument.class);
	    professionDoc.setProfessionId(String.valueOf(professionDoc.getProfessionId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(professionDoc.getProfessionId()).document(professionDoc)));

	    // Convert back to response DTO in local time
	    ProfessionResponseDTO responseDTO = objectMapper.convertValue(professionDoc, ProfessionResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public ProfessionResponseDTO updateProfession(Long professionId, ProfessionRequestDTO dto) {
		Profession profession = professionRepository.findById(professionId)
				.orElseThrow(() -> new RuntimeException("Profession not found with ID: " + professionId));

		profession.setProfessionName(dto.getProfessionName());

		Profession updatedProfession = professionRepository.save(profession);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		ProfessionDocument professionDoc = objectMapper.convertValue(updatedProfession, ProfessionDocument.class);
		professionDoc.setProfessionId(String.valueOf(updatedProfession.getProfessionId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(professionDoc.getProfessionId()).document(professionDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 ProfessionResponseDTO responseDTO = objectMapper.convertValue(professionDoc, ProfessionResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteProfession(Long id) throws IOException {
		if (professionRepository.existsById(id)) {
			professionRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "professionCache", keyGenerator = "customKeyGenerator")
	public List<ProfessionResponseDTO> findAllProfession(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<Profession> professionPage = professionRepository.findAll(pageable);

		List<ProfessionResponseDTO> profession = professionPage.getContent().stream()
		        .map(pro -> objectMapper.convertValue(pro, ProfessionResponseDTO.class))
		        .collect(Collectors.toList());

		return profession.stream().map(pro -> objectMapper.convertValue(pro, ProfessionResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<ProfessionResponseDTO> esFindAllProfession(PageDTO pageDTO)

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

		SearchResponse<ProfessionDocument> response = elasticClient.search(searchRequest, ProfessionDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), ProfessionResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllProfessionFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllProfessionFromDB() {
		return professionRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public ProfessionResponseDTO esFindProfessionById(Long professionId) throws IOException {
		SearchResponse<ProfessionDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("professionId").value(String.valueOf(professionId)))),
				ProfessionDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), ProfessionResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "professionCache", keyGenerator = "customKeyGenerator")
	public ProfessionResponseDTO findProfessionById(Long professionId) throws JsonProcessingException {
		Profession profession = professionRepository.findById(professionId)
				.orElseThrow(() -> new RuntimeException("Profession not found with ID: " + professionId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(profession, ProfessionResponseDTO.class);
	}
}

