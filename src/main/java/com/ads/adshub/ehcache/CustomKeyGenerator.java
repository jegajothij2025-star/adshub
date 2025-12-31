package com.ads.adshub.ehcache;

import java.lang.reflect.Method;


import java.util.List;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import com.ads.adshub.config.DataContext;
import com.ads.adshub.model.FilterCriteria;
import com.ads.adshub.model.PageDTO;

@Component("customKeyGenerator")
public class CustomKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        Long appId = DataContext.getAppId();
        //Integer userId = DataContext.getUserId();
        Object idParam = null;
        PageDTO pageDto = null;
        List<FilterCriteria> filterCriteriaList = null;
        
//        if (params.length == 0) {
//            return appId + ":" + userId + ":default";
//        }
        for (Object param : params) {
            if (param instanceof PageDTO) {
                pageDto = (PageDTO) param;
            } else if (param instanceof List) {
                List<?> list = (List<?>) param;
                if (list.stream().allMatch(o -> o instanceof FilterCriteria)) {
                    filterCriteriaList = list.stream()
                                             .map(o -> (FilterCriteria) o)
                                             .toList(); // Java 16+, or collect(Collectors.toList()) for older
                }
            }
            else if (param instanceof Long || param instanceof Integer || param instanceof String) {
                idParam = param;
            }
        }

        if (pageDto != null) {
        	 String filterPart = (filterCriteriaList != null && !filterCriteriaList.isEmpty())
                     ? filterCriteriaList.stream()
                           .map(fc -> fc.getField() + "-" + fc.getOperation() + "-" + String.join("_", fc.getValues().stream().map(String::valueOf).toList()))
                           .reduce((a, b) -> a + "|" + b)
                           .orElse("no-filters")
                     : "no-filters";
            return String.format("%d:%d:%d:%s:%s:%s:%s",
                appId,
                pageDto.getPage(),
                pageDto.getSize(),
                pageDto.getSortBy(),
                pageDto.getSortDir(),
                pageDto.getSearchKey() != null ? pageDto.getSearchKey() : "default",
            		filterPart);
        }
       return String.format("%d:%s", appId, idParam != null ? idParam.toString() : "no-id");
    }
}
