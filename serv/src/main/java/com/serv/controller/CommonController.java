package com.serv.controller;

import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.dto.GeographicZoneDTO;
import com.serv.dto.ServiceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/common")
public class CommonController {

    private final ServiceRepository serviceRepository;
    private final GeographicZoneRepository zoneRepository;

    @Transactional(readOnly = true)
    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getWorkerServices() {
        return ResponseEntity.ok(serviceRepository.findAll().stream()
                .map(ServiceDTO::from)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/locations")
    public ResponseEntity<List<GeographicZoneDTO>> getLocationsTree() {
        List<GeographicZoneDTO> roots = zoneRepository.findAll().stream()
                .filter(z -> z.getParent() == null)
                .map(GeographicZoneDTO::from)
                .toList();
        return ResponseEntity.ok(roots);
    }

}
