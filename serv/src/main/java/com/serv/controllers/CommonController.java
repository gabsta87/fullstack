package com.serv.controllers;

import com.serv.common.InputLimits;
import com.serv.database.entities.LegalText;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.LegalTextRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.dto.GeographicZoneWithChildrenDTO;
import com.serv.dto.ServiceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/common")
public class CommonController {

    private final ServiceRepository serviceRepository;
    private final GeographicZoneRepository zoneRepository;
    private final LegalTextRepository legalTextRepository;

    /** Current version of a legal text, shown to visitors (plain text). */
    public record LegalTextDTO(String key, String language, int version, String content) {}

    @Transactional(readOnly = true)
    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getWorkerServices() {
        return ResponseEntity.ok(serviceRepository.findAll().stream()
                .map(ServiceDTO::from)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/regions")
    public ResponseEntity<List<GeographicZoneWithChildrenDTO>> getLocationsTree() {
        List<GeographicZoneWithChildrenDTO> roots = zoneRepository.findAll().stream()
                .filter(z -> z.getParent() == null)
                .map(GeographicZoneWithChildrenDTO::from)
                .toList();
        return ResponseEntity.ok(roots);
    }

    /**
     * Current version of a public legal text (e.g. ADULT_NOTICE) in the site language,
     * English when it has no translation.
     */
    @Transactional(readOnly = true)
    @GetMapping("/legal/{key}")
    public ResponseEntity<LegalTextDTO> getLegalText(@PathVariable String key,
                                                     @RequestParam(defaultValue = "en") String lang) {
        if (!key.matches(InputLimits.LEGAL_KEY_PATTERN) || !lang.matches(InputLimits.SITE_LANGUAGE_PATTERN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid legal text key or language.");
        }
        LegalText text = legalTextRepository.findFirstByNameAndLanguageOrderByVersionDesc(key, lang)
                .or(() -> legalTextRepository.findFirstByNameAndLanguageOrderByVersionDesc(key, "en"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal text not found"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(new LegalTextDTO(text.getName(), text.getLanguage(), text.getVersion(), text.getContent()));
    }
}
