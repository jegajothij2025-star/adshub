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

import com.ads.adshub.entity.Gender;
import com.ads.adshub.entity.document.GenderDocument;
import com.ads.adshub.model.FilterCriteria;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.GenderRepository;
import com.ads.adshub.request.GenderRequestDTO;
import com.ads.adshub.response.GenderResponseDTO;
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
public class GenderService {

    private final GenderRepository genderRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;

 
    private static final String INDEX_NAME = "genders";

    public GenderService(GenderRepository genderRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper) {
        this.genderRepository = genderRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
    }

    /* ================= CREATE ================= */
    @Transactional
	public GenderResponseDTO createGender(GenderRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    Gender gender = Gender.builder()
	            .gender(dto.getGender())
	            .genderCode(dto.getGenderCode())
	            .build();

	    Gender savedGender = genderRepository.save(gender);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    GenderDocument genderDoc = objectMapper.convertValue(savedGender, GenderDocument.class);
	    genderDoc.setGenderId(String.valueOf(genderDoc.getGenderId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(genderDoc.getGenderId()).document(genderDoc)));

	    // Convert back to response DTO in local time
	    GenderResponseDTO responseDTO = objectMapper.convertValue(genderDoc, GenderResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public GenderResponseDTO updateGender(Long id, GenderRequestDTO dto) {
		Gender gender = genderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));

		gender.setGender(dto.getGender());
		gender.setGenderCode(dto.getGenderCode());

		Gender updatedGender = genderRepository.save(gender);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		GenderDocument genderDoc = objectMapper.convertValue(updatedGender, GenderDocument.class);
		genderDoc.setGenderId(String.valueOf(updatedGender.getGenderId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(genderDoc.getGenderId()).document(genderDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 GenderResponseDTO responseDTO = objectMapper.convertValue(genderDoc, GenderResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteGender(Long id) throws IOException {
		if (genderRepository.existsById(id)) {
			genderRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "gendersCache", keyGenerator = "customKeyGenerator")
	public List<GenderResponseDTO> findAllGenders(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<Gender> genderPage = genderRepository.findAll(pageable);

		List<GenderResponseDTO> genders = genderPage.getContent().stream()
		        .map(gen -> objectMapper.convertValue(gen, GenderResponseDTO.class))
		        .collect(Collectors.toList());

		return genders.stream().map(gen -> objectMapper.convertValue(gen, GenderResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<GenderResponseDTO> esFindAllGenders(PageDTO pageDTO)

			throws IOException {



		Query query =  Query.of(q -> q.matchAll(m -> m));

		SearchRequest searchRequest = SearchRequest.of(s -> s.index(INDEX_NAME).query(query)

				.from(pageDTO.getPage() * pageDTO.getSize()).size(pageDTO.getSize())

				.sort(srt -> srt.field(f -> f.field(pageDTO.getSortBy())

						.order(pageDTO.getSortDir().equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc))));

		SearchResponse<GenderDocument> response = elasticClient.search(searchRequest, GenderDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), GenderResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllGendersFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllGendersFromDB() { //no need to pass parameter
		return genderRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public GenderResponseDTO esFindGenderById(Long genderId) throws IOException {
		SearchResponse<GenderDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("genderId").value(String.valueOf(genderId)))),
				GenderDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), GenderResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "gendersCache", keyGenerator = "customKeyGenerator")
	public GenderResponseDTO findGenderById(Long id) throws JsonProcessingException {
		Gender gender = genderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Gender not found with ID: " + id));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(gender, GenderResponseDTO.class);
	}
}
