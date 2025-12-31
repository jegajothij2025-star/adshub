package com.ads.adshub.model;

import java.util.List;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCriteria {

	private String field;
    private String operation;
    private List<Object> values;
}
