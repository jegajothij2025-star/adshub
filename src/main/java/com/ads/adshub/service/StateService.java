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

import com.ads.adshub.entity.State;
import com.ads.adshub.entity.document.StateDocument;
import com.ads.adshub.model.FilterCriteria;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.StateRepository;
import com.ads.adshub.request.StateRequestDTO;
import com.ads.adshub.response.StateResponseDTO;
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
public class StateService {

    private final StateRepository stateRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "states";

    public StateService(StateRepository stateRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.stateRepository = stateRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public StateResponseDTO createState(StateRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    State state = State.builder()
	    		.stateName(dto.getStateName())
	            .stateCode(dto.getStateCode())
	            .countryCode(dto.getCountryCode())
	            .build();

	    State savedState = stateRepository.save(state);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    StateDocument stateDoc = objectMapper.convertValue(savedState, StateDocument.class);
	    stateDoc.setStateId(String.valueOf(stateDoc.getStateId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(stateDoc.getStateId()).document(stateDoc)));

	    // Convert back to response DTO in local time
	    StateResponseDTO responseDTO = objectMapper.convertValue(stateDoc, StateResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public StateResponseDTO updateState(Long stateId, StateRequestDTO dto) {
		State state = stateRepository.findById(stateId)
				.orElseThrow(() -> new RuntimeException("State not found with ID: " + stateId));

		state.setStateName(dto.getStateName());
		state.setStateCode(dto.getStateCode());
		state.setCountryCode(dto.getCountryCode());

		State updatedState = stateRepository.save(state);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		StateDocument stateDoc = objectMapper.convertValue(updatedState, StateDocument.class);
		stateDoc.setStateId(String.valueOf(updatedState.getStateId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(stateDoc.getStateId()).document(stateDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 StateResponseDTO responseDTO = objectMapper.convertValue(stateDoc, StateResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteState(Long id) throws IOException {
		if (stateRepository.existsById(id)) {
			stateRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "statesCache", keyGenerator = "customKeyGenerator")
	public List<StateResponseDTO> findAllStates(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<State> statePage = stateRepository.findAll(pageable);

		List<StateResponseDTO> states = statePage.getContent().stream()
		        .map(sta -> objectMapper.convertValue(sta, StateResponseDTO.class))
		        .collect(Collectors.toList());

		return states.stream().map(sta -> objectMapper.convertValue(sta, StateResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<StateResponseDTO> esFindAllStates(PageDTO pageDTO)

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

		SearchResponse<StateDocument> response = elasticClient.search(searchRequest, StateDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), StateResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllStatesFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllStatesFromDB() {
		return stateRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public StateResponseDTO esFindStateById(Long stateId) throws IOException {
		SearchResponse<StateDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("stateId").value(String.valueOf(stateId)))),
				StateDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), StateResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "statesCache", keyGenerator = "customKeyGenerator")
	public StateResponseDTO findStateById(Long stateId) throws JsonProcessingException {
		State state = stateRepository.findById(stateId)
				.orElseThrow(() -> new RuntimeException("State not found with ID: " + stateId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(state, StateResponseDTO.class);
	}
}

