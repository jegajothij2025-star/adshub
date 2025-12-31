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

import com.ads.adshub.entity.District;
import com.ads.adshub.entity.document.DistrictDocument;
import com.ads.adshub.model.FilterCriteria;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.DistrictRepository;
import com.ads.adshub.request.DistrictRequestDTO;
import com.ads.adshub.response.DistrictResponseDTO;
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
public class DistrictService {

    private final DistrictRepository districtRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "districts";

    public DistrictService(DistrictRepository districtRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.districtRepository = districtRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public DistrictResponseDTO createDistrict(DistrictRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    District district = District.builder()
	    		.districtName(dto.getDistrictName())
	            .districtCode(dto.getDistrictCode())
	            .stateId(dto.getStateId())
	            .build();

	    District savedDistrict = districtRepository.save(district);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    DistrictDocument districtDoc = objectMapper.convertValue(savedDistrict, DistrictDocument.class);
	    districtDoc.setDistrictId(String.valueOf(districtDoc.getDistrictId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(districtDoc.getDistrictId()).document(districtDoc)));

	    // Convert back to response DTO in local time
	    DistrictResponseDTO responseDTO = objectMapper.convertValue(districtDoc, DistrictResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public DistrictResponseDTO updateDistrict(Long districtId, DistrictRequestDTO dto) {
		District district = districtRepository.findById(districtId)
				.orElseThrow(() -> new RuntimeException("District not found with ID: " + districtId));

		district.setDistrictName(dto.getDistrictName());
		district.setDistrictCode(dto.getDistrictCode());
		district.setStateId(dto.getStateId());

		District updatedDistrict = districtRepository.save(district);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		DistrictDocument districtDoc = objectMapper.convertValue(updatedDistrict, DistrictDocument.class);
		districtDoc.setDistrictId(String.valueOf(updatedDistrict.getDistrictId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(districtDoc.getDistrictId()).document(districtDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 DistrictResponseDTO responseDTO = objectMapper.convertValue(districtDoc, DistrictResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteDistrict(Long id) throws IOException {
		if (districtRepository.existsById(id)) {
			districtRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "districtsCache", keyGenerator = "customKeyGenerator")
	public List<DistrictResponseDTO> findAllDistricts(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<District> districtPage = districtRepository.findAll(pageable);

		List<DistrictResponseDTO> districts = districtPage.getContent().stream()
		        .map(dis -> objectMapper.convertValue(dis, DistrictResponseDTO.class))
		        .collect(Collectors.toList());

		return districts.stream().map(dis -> objectMapper.convertValue(dis, DistrictResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<DistrictResponseDTO> esFindAllDistricts(PageDTO pageDTO)

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

		SearchResponse<DistrictDocument> response = elasticClient.search(searchRequest, DistrictDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), DistrictResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllDistrictsFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllDistrictsFromDB() {
		return districtRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public DistrictResponseDTO esFindDistrictById(Long districtId) throws IOException {
		SearchResponse<DistrictDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("districtId").value(String.valueOf(districtId)))),
				DistrictDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), DistrictResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "districtsCache", keyGenerator = "customKeyGenerator")
	public DistrictResponseDTO findDistrictById(Long districtId) throws JsonProcessingException {
		District district = districtRepository.findById(districtId)
				.orElseThrow(() -> new RuntimeException("District not found with ID: " + districtId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(district, DistrictResponseDTO.class);
	}
}
