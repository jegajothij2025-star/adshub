package com.ads.adshub.utill;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import com.ads.adshub.model.FilterCriteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;



public class FilterSpecification<T> implements Specification<T> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8686562036363078095L;
	private final List<FilterCriteria> criteriaList;

    public FilterSpecification(List<FilterCriteria> criteriaList) {
        this.criteriaList = criteriaList;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
    	List<Predicate> predicates = new ArrayList<>();

        for (FilterCriteria filter : criteriaList) {
            String field = filter.getField();
            String operation = filter.getOperation();
            List<Object> values = filter.getValues();
            
            if (!operation.equals("isTrue") && !operation.equals("isFalse") && (values == null || values.isEmpty())) {
                continue;
            }

            switch (operation) {
                // String Operations
                case "equals":
                    // Use case-insensitive search for text fields
                    List<Predicate> equalsPredicates = values.stream()
                            .map(val -> criteriaBuilder.equal(criteriaBuilder.lower(root.get(field)), val.toString().toLowerCase()))
                            .collect(Collectors.toList());
                    predicates.add(criteriaBuilder.or(equalsPredicates.toArray(new Predicate[0])));
                    break;
                case "notEquals":
                    // Use case-insensitive search for text fields
                    List<Predicate> notEqualPredicates = values.stream()
                            .map(val -> criteriaBuilder.notEqual(criteriaBuilder.lower(root.get(field)), val.toString().toLowerCase()))
                            .collect(Collectors.toList());
                    predicates.add(criteriaBuilder.and(notEqualPredicates.toArray(new Predicate[0])));
                    break;
                case "contains":
                    // Use case-insensitive search for text fields
                    List<Predicate> containsPredicates = values.stream()
                            .map(val -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), "%" + val.toString().toLowerCase() + "%"))
                            .collect(Collectors.toList());
                    predicates.add(criteriaBuilder.and(containsPredicates.toArray(new Predicate[0])));
                    break;
                case "startsWith":
                    // Use case-insensitive search for text fields
                    List<Predicate> startsWithPredicates = values.stream()
                            .map(val -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), val.toString().toLowerCase() + "%"))
                            .collect(Collectors.toList());
                    predicates.add(criteriaBuilder.and(startsWithPredicates.toArray(new Predicate[0])));
                    break;
                case "endsWith":
                    // Use case-insensitive search for text fields
                    List<Predicate> endsWithPredicates = values.stream()
                            .map(val -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), "%" + val.toString().toLowerCase()))
                            .collect(Collectors.toList());
                    predicates.add(criteriaBuilder.and(endsWithPredicates.toArray(new Predicate[0])));
                    break;

                // Numeric Operations
                case "greaterThan":
                case "lessThan":
                case "greaterThanOrEqualTo":
                case "lessThanOrEqualTo":
                case "between":
                    for (Object value : values) {
                        if (value instanceof Integer) {
                            Integer numberValue = (Integer) value;
                            switch (operation) {
                                case "greaterThan":
                                    predicates.add(criteriaBuilder.greaterThan(root.get(field), numberValue));
                                    break;
                                case "lessThan":
                                    predicates.add(criteriaBuilder.lessThan(root.get(field), numberValue));
                                    break;
                                case "greaterThanOrEqualTo":
                                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(field), numberValue));
                                    break;
                                case "lessThanOrEqualTo":
                                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(field), numberValue));
                                    break;
                                case "between":
                                    if (values.size() == 2 && values.get(0) instanceof Integer && values.get(1) instanceof Integer) {
                                        predicates.add(criteriaBuilder.between(root.get(field), (Integer) values.get(0), (Integer) values.get(1)));
                                    }
                                    break;
                            }
                        } else if (value instanceof Double) {
                            Double numberValue = (Double) value;
                            switch (operation) {
                                case "greaterThan":
                                    predicates.add(criteriaBuilder.greaterThan(root.get(field), numberValue));
                                    break;
                                case "lessThan":
                                    predicates.add(criteriaBuilder.lessThan(root.get(field), numberValue));
                                    break;
                                case "greaterThanOrEqualTo":
                                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(field), numberValue));
                                    break;
                                case "lessThanOrEqualTo":
                                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(field), numberValue));
                                    break;
                                case "between":
                                    if (values.size() == 2 && values.get(0) instanceof Double && values.get(1) instanceof Double) {
                                        predicates.add(criteriaBuilder.between(root.get(field), (Double) values.get(0), (Double) values.get(1)));
                                    }
                                    break;
                            }
                        }
                    }
                    break;

                // Date Operations
                case "before":
                case "after":
                	for (Object value : values) {
                        LocalDate dateValue = null;
                        if (value instanceof String) {
                            try {
                                dateValue = LocalDate.parse((String) value);
                            } catch (DateTimeParseException e) {
                                throw new IllegalArgumentException("Invalid date format: " + value);
                            }
                        } else if (value instanceof LocalDate) {
                            dateValue = (LocalDate) value;
                        }

                        if (dateValue != null) {
                            if (operation.equals("before")) {
                                predicates.add(criteriaBuilder.lessThan(root.get(field), dateValue));
                            } else {
                                predicates.add(criteriaBuilder.greaterThan(root.get(field), dateValue));
                            }
                        }
                    }
                    break; 
                case "betweenDates":
                    if (values.size() == 2) {
                        OffsetDateTime startDateTime = parseDateOrDateTimeDB(values.get(0));
                        OffsetDateTime endDateTime = parseDateOrDateTimeDB(values.get(1));

                        if (startDateTime != null && endDateTime != null) {
                            // ✅ Use OffsetDateTime directly to match DB column type
                            predicates.add(criteriaBuilder.between(root.get(field), startDateTime, endDateTime));
                        }
                    }
                    break;

                case "equalsDate":
                    List<Predicate> dateEqualsPredicates = values.stream()
                            .map(val -> criteriaBuilder.equal(root.get(field), parseDateOrDateTimeDB(val)))
                            .collect(Collectors.toList());

                    if (!dateEqualsPredicates.isEmpty()) {
                        // ✅ Combine with OR, equivalent to ES `should` query
                        predicates.add(criteriaBuilder.or(dateEqualsPredicates.toArray(new Predicate[0])));
                    }
                    break;

                // Boolean Operations
                case "isTrue":
                    predicates.add(criteriaBuilder.isTrue(root.get(field)));
                    break;

                case "isFalse":
                    predicates.add(criteriaBuilder.isFalse(root.get(field)));
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported operation: " + operation);

            }
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }
    
    private static OffsetDateTime parseDateOrDateTimeDB(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Date value cannot be null");
        }

        if (value instanceof String) {
            String strVal = ((String) value).trim();
            if (strVal.isEmpty()) {
                throw new IllegalArgumentException("Date string cannot be empty");
            }

            // 1️⃣ Try LocalDate (yyyy-MM-dd)
            try {
                LocalDate localDate = LocalDate.parse(strVal);
                return localDate.atStartOfDay().atOffset(ZoneOffset.UTC); // start of day UTC
            } catch (DateTimeParseException ignored) {}

            // 2️⃣ Try LocalDateTime (yyyy-MM-dd HH:mm:ss)
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(strVal, formatter);
                return localDateTime.atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}

            // 3️⃣ Try OffsetDateTime directly (ISO-8601 format)
            try {
                return OffsetDateTime.parse(strVal);
            } catch (DateTimeParseException ignored) {}

            throw new IllegalArgumentException("Invalid date/datetime format: " + strVal);
        }

        // Direct Java date types
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atOffset(ZoneOffset.UTC);
        }
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }

        throw new IllegalArgumentException("Unsupported date type: " + value.getClass().getName());
    }


}
