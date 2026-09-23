package com.serv.dto;

import com.serv.database.entities.Admin;

public record AdminDTO (String id, String username, String email, String role) {

    public static AdminDTO from(Admin admin){
        return new AdminDTO(
                admin.getId().toString(),
                admin.getUsername(),
                admin.getEmail().toString(),
                admin.getRole().toString()
        );
    }
}
