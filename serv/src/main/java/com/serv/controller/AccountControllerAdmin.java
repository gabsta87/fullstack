package com.serv.controller;

import com.serv.database.entities.Admin;
import com.serv.database.entities.VenusUser;
import com.serv.dto.AdminUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountControllerAdmin {

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Admin user) {
        return ResponseEntity.ok(AdminUserDTO.from(user));
    }
}
