package com.serv.dto;

public record ServiceDTO(Integer id, String name, String description) {

    public static ServiceDTO from(com.serv.database.entities.Service s) {
        return new ServiceDTO(s.getId(), s.getName(), s.getDescription());
    }
}
