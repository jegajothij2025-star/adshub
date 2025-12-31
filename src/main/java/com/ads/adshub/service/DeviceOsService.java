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

import com.ads.adshub.entity.DeviceOs;
import com.ads.adshub.entity.document.DeviceOsDocument;
import com.ads.adshub.model.PageDTO;
import com.ads.adshub.repository.DeviceOsRepository;
import com.ads.adshub.request.DeviceOsRequestDTO;
import com.ads.adshub.response.DeviceOsResponseDTO;
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
public class DeviceOsService {

    private final DeviceOsRepository deviceosRepository;
    private final ElasticsearchClient elasticClient;
    private final ObjectMapper objectMapper;
    private final ElasticFieldUtil elasticfieldutil;

    private static final String INDEX_NAME = "states";

    public DeviceOsService(DeviceOsRepository deviceosRepository,
                         ElasticsearchClient elasticClient,
                         ObjectMapper objectMapper, ElasticFieldUtil elasticfieldutil) {
        this.deviceosRepository = deviceosRepository;
        this.elasticClient = elasticClient;
        this.objectMapper = objectMapper;
        this.elasticfieldutil = elasticfieldutil;
    }

    /* ================= CREATE ================= */
    @Transactional
	public DeviceOsResponseDTO createDeviceOs(DeviceOsRequestDTO dto) throws IOException {
	    // Save in DB (LocalDateTime assumed UTC)
		//OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

	    DeviceOs deviceos = DeviceOs.builder()
	    		.deviceos(dto.getDeviceos())
	            .deviceosCode(dto.getDeviceosCode())
	            .build();

	    DeviceOs savedDeviceOs = deviceosRepository.save(deviceos);

	    // Fetch view data
		
		/*
		 * Gender gender1 = genderRepository.findById(savedGender.getGenderId())
		 * .orElseThrow(() -> new RuntimeException("Employee view not found for ID: " +
		 * savedGender.getGenderId()));
		 */
		 

	    // Convert to ES document
	    DeviceOsDocument deviceosDoc = objectMapper.convertValue(savedDeviceOs, DeviceOsDocument.class);
	    deviceosDoc.setDeviceosId(String.valueOf(deviceosDoc.getDeviceosId()));

	    // Index into Elasticsearch
	    elasticClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(deviceosDoc.getDeviceosId()).document(deviceosDoc)));

	    // Convert back to response DTO in local time
	    DeviceOsResponseDTO responseDTO = objectMapper.convertValue(deviceosDoc, DeviceOsResponseDTO.class);

	    return responseDTO;
	}

    /* ================= UPDATE ================= */
    @Transactional
	public DeviceOsResponseDTO updateDeviceOs(Long deviceosId, DeviceOsRequestDTO dto) {
		DeviceOs deviceos = deviceosRepository.findById(deviceosId)
				.orElseThrow(() -> new RuntimeException("Device OS not found with ID: " + deviceosId));

		deviceos.setDeviceos(dto.getDeviceos());
		deviceos.setDeviceosCode(dto.getDeviceosCode());

		DeviceOs updatedDeviceOs = deviceosRepository.save(deviceos);

//		EmployeeView viewData = employeeViewRepository.findById(updatedEmployee.getId())
//				.orElseThrow(() -> new RuntimeException("Employee view not found for ID: " + updatedEmployee.getId()));

		DeviceOsDocument deviceosDoc = objectMapper.convertValue(updatedDeviceOs, DeviceOsDocument.class);
		deviceosDoc.setDeviceosId(String.valueOf(updatedDeviceOs.getDeviceosId()));

		// Update ES document
		try {
			elasticClient
					.index(IndexRequest.of(i -> i.index(INDEX_NAME).id(deviceosDoc.getDeviceosId()).document(deviceosDoc)));
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 DeviceOsResponseDTO responseDTO = objectMapper.convertValue(deviceosDoc, DeviceOsResponseDTO.class);
		return responseDTO;
	}
    /* ================= DELETE ================= */
    @Transactional
	public boolean deleteDeviceOs(Long id) throws IOException {
		if (deviceosRepository.existsById(id)) {
			deviceosRepository.deleteById(id);

			elasticClient.delete(DeleteRequest.of(d -> d.index(INDEX_NAME).id(String.valueOf(id))));

			return true;
		}
		return false;
	}

    /* ================= FIND ALL (DB) ================= */
    @Cacheable(value = "deviceosCache", keyGenerator = "customKeyGenerator")
	public List<DeviceOsResponseDTO> findAllDeviceOs(PageDTO pageDTO) {
		Sort sort = pageDTO.getSortDir().equalsIgnoreCase("asc") ?
		        Sort.by(pageDTO.getSortBy()).ascending() :
		        Sort.by(pageDTO.getSortBy()).descending();
		Pageable pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize(), sort);
		Page<DeviceOs> deviceosPage = deviceosRepository.findAll(pageable);

		List<DeviceOsResponseDTO> deviceos = deviceosPage.getContent().stream()
		        .map(dev -> objectMapper.convertValue(dev, DeviceOsResponseDTO.class))
		        .collect(Collectors.toList());

		return deviceos.stream().map(dev -> objectMapper.convertValue(dev, DeviceOsResponseDTO.class))
				.collect(Collectors.toList());
	}
    
    /* ================= FIND ALL (ES) ================= */
    public List<DeviceOsResponseDTO> esFindAllDeviceOs(PageDTO pageDTO)

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

		SearchResponse<DeviceOsDocument> response = elasticClient.search(searchRequest, DeviceOsDocument.class);

		return response.hits().hits().stream()

				.map(hit -> objectMapper.convertValue(hit.source(), DeviceOsResponseDTO.class))

				.collect(Collectors.toList());

	}

public long countAllDeviceOsFromES() throws IOException {

		Query query =  Query.of(q -> q.matchAll(m -> m));

		CountResponse countResponse = elasticClient.count(CountRequest.of(c -> c.index(INDEX_NAME).query(query)));

		return countResponse.count();

	}
	
	public long countAllDeviceOsFromDB() {
		return deviceosRepository.count();
	}

    
    /* ================== FIND BY ID ES ==================== */
    public DeviceOsResponseDTO esFindDeviceOsById(Long deviceosId) throws IOException {
		SearchResponse<DeviceOsDocument> response = elasticClient.search(
				s -> s.index(INDEX_NAME).query(q -> q.term(t -> t.field("deviceosId").value(String.valueOf(deviceosId)))),
				DeviceOsDocument.class);

		if (response.hits().hits().isEmpty())
			return null;
		return objectMapper.convertValue(response.hits().hits().get(0).source(), DeviceOsResponseDTO.class);
	}
    
    /* =================== FIND BY ID DB ====================== */
    @Cacheable(value = "deviceosCache", keyGenerator = "customKeyGenerator")
	public DeviceOsResponseDTO findDeviceOsById(Long deviceosId) throws JsonProcessingException {
		DeviceOs deviceos = deviceosRepository.findById(deviceosId)
				.orElseThrow(() -> new RuntimeException("Device OS not found with ID: " + deviceosId));
		//String jsonString = objectMapper.writeValueAsString(gender);
		//aspect.LogCodeExecution(jsonString);
		return objectMapper.convertValue(deviceos, DeviceOsResponseDTO.class);
	}
}

