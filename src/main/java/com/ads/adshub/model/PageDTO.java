package com.ads.adshub.model;

import lombok.Data;

@Data
public class PageDTO {
	private int page;
	private int size;
	private String sortBy;
	private String sortDir;
	private String searchKey;
}
