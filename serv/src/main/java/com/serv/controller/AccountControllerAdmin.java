package com.serv.controller;

import com.serv.database.entities.Admin;
import com.serv.database.repositories.AdminRepository;
import com.serv.dto.AdminDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/account/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountControllerAdmin {

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<AdminDTO> getMe(Admin user) {
        Admin managedAdmin = adminRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin introuvable"));

        return ResponseEntity.ok(AdminDTO.from(managedAdmin));
    }
}
