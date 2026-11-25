package com.serv.controllers;

import com.serv.common.InputLimits;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.WorkerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.serv.dto.WorkerMinimalProfileDTO;
import com.serv.dto.WorkerPublicFullProfileDTO;
import com.serv.requests.WorkersFilters;
import com.serv.services.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Worker-facing read endpoints.
 * GET /workers          — paginated gallery (lightweight DTOs, main thumb only)
 * GET /workers/{id}     — full profile (all photos, videos, reviews…)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workers")
@Transactional(readOnly = true)
public class WorkerController {
    private final WorkerRepository workerRepository;
    private final WorkerService    galleryService;

    @GetMapping
    public ResponseEntity<List<WorkerMinimalProfileDTO>> getGallery(WorkersFilters req) {
        Map<String, Object> allFilters = new HashMap<>();

        // 1. Zone ID
        if (req.zoneId() != null && !req.zoneId().isBlank() && !req.zoneId().equalsIgnoreCase("undefined")) {
            try { allFilters.put("zoneId", Integer.parseInt(req.zoneId())); } catch (NumberFormatException ignored) {}
        }

        // 2. Assignations explicites et robustes
        if (req.username() != null && req.username().length() > InputLimits.USERNAME_MAX) {
            return ResponseEntity.badRequest().build();
        }
        if (req.username()  != null && !req.username().isBlank()) allFilters.put("username", req.username().trim());
        if (req.bodyType()  != null && !req.bodyType().isBlank()) allFilters.put("bodyType", req.bodyType().trim());
        if (req.eyeColor()  != null && !req.eyeColor().isBlank()) allFilters.put("eyeColor", req.eyeColor().trim());
        if (req.hairColor() != null && !req.hairColor().isBlank()) allFilters.put("hairColor", req.hairColor().trim());
        if (req.gender()    != null && !req.gender().isBlank()) allFilters.put("gender", req.gender().trim());

        // Age (accepted as any textual or numeric form)
        if (req.minAge() != null && !req.minAge().toString().isBlank()) allFilters.put("minAge", req.minAge().toString().trim());
        if (req.maxAge() != null && !req.maxAge().toString().isBlank()) allFilters.put("maxAge", req.maxAge().toString().trim());

        // Collections
        if ((req.services() != null && req.services().size() > InputLimits.LIST_MAX)
                || (req.languages() != null && req.languages().size() > InputLimits.LIST_MAX)) {
            return ResponseEntity.badRequest().build();
        }
        if (req.services()  != null && !req.services().isEmpty()) allFilters.put("services", req.services());
        if (req.languages() != null && !req.languages().isEmpty()) allFilters.put("languages", req.languages());

        return ResponseEntity.ok(galleryService.getGalleryPage(req.getPageOrZero(), allFilters));
    }

    /**
     * Public profile. Profiles that are not publicly visible (banned, locked, hidden, incomplete,
     * expired or minor) answer 404, except for admins and for the owner of the profile.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkerPublicFullProfileDTO> getProfile(@PathVariable UUID id, Authentication authentication) {
        return workerRepository.findByIdWithPhotos(id)
                .filter(w -> w.isValid() || isAdmin(authentication) || isOwner(authentication, w))
                .map(w -> ResponseEntity.ok(WorkerPublicFullProfileDTO.from(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static boolean isOwner(Authentication authentication, Worker worker) {
        return authentication instanceof JwtAuthenticationToken jwt
                && worker.getId().toString().equals(jwt.getToken().getClaimAsString(JwtProvider.CLAIM_USER_ID));
    }

}