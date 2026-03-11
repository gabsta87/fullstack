package com.serv.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class ServiceListConverter implements AttributeConverter<List<com.serv.common.Service>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<com.serv.common.Service> services) {
        if (services == null || services.isEmpty()) return "";
        return services.stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<com.serv.common.Service> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split(DELIMITER))
                .map(String::trim)
                .map(com.serv.common.Service::valueOf)
                .collect(Collectors.toList());
    }
}