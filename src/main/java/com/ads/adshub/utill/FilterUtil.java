package com.ads.adshub.utill;

import java.util.ArrayList;

import java.util.List;

import com.ads.adshub.model.FilterCriteria;

public class FilterUtil {

public static List<FilterCriteria> getUpdatedFilterCriteria(List<FilterCriteria> listFilterCriteria, String fieldName, String value, boolean isSearchFilter){
		
		
		List<FilterCriteria> updatedFilterCriteria = (listFilterCriteria == null) ? new ArrayList<>() : new ArrayList<>(listFilterCriteria);
		
		if(isSearchFilter) {
			updatedFilterCriteria.add(FilterCriteria.builder()
		            .field(fieldName)
		            .operation("startsWith")
		            .values(List.of(value))
		            .build());
		}
		else {
			updatedFilterCriteria.add(0,FilterCriteria.builder()
		            .field(fieldName)
		            .operation("startsWith")
		            .values(List.of(value))
		            .build());
		}
		
		
		return updatedFilterCriteria;
	}
}
