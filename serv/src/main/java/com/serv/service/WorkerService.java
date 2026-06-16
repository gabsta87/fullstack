package com.serv.service;

import com.serv.common.BodyType;
import com.serv.common.EyeColor;
import com.serv.common.HairColor;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.repositories.filters.WorkerSpecifications;
import com.serv.dto.WorkerMinimalProfileDTO;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkerService {

    @Autowired private WorkerRepository workerRepository;
    @Autowired private PhotoRepository  photoRepository;

    public static final int MAX_PREVIEW_THUMBS = 5;
    public static final int PAGE_SIZE          = 24;

    public List<WorkerMinimalProfileDTO> getGalleryPage(int page, Map<String, Object> filters) {

        System.out.println("applying "+filters.size()+" filters : "+filters);

        // 1. On prépare la base : Uniquement les profils non désactivés + les filtres dynamiques
        Specification<Worker> spec = Specification
                .where(WorkerSpecifications.isNotDisabled())
                .and(buildDynamicFilters(filters));

        // 2. LA MAGIE DU MULTI-TRI :
        // D'abord 'available' (true avant false), puis 'galleryPositionIndex' par ordre décroissant
        Sort doubleSort = Sort.by(Sort.Direction.DESC, "available")
                .and(Sort.by(Sort.Direction.DESC, "galleryPositionIndex"));

        // 3. Une seule et unique requête propre, paginée au niveau SQL
        List<Worker> workers = workerRepository.findAll(
                spec,
                PageRequest.of(page, PAGE_SIZE, doubleSort)
        ).getContent();

        System.out.println("found workers : "+workers.size());

        if (workers.isEmpty()) return List.of();

        // 4. Récupération des images principales (sans requête supplémentaire grâce à ton modèle)
        Map<UUID, String> mainThumbs = workers.stream()
                .filter(w -> w.getMainPhoto() != null)
                .collect(Collectors.toMap(
                        Worker::getId,
                        w -> w.getMainPhoto().getMainThumbUrl()
                ));

        // Batch fetch des miniatures de preview, groupées par workerId
        List<UUID> workerIds = workers.stream().map(Worker::getId).toList();
        Map<UUID, List<String>> previewThumbs = new HashMap<>();
        photoRepository
                .findByWorkerIdInOrderBySortOrderAsc(workerIds)
                .forEach(p -> previewThumbs
                        .computeIfAbsent(p.getWorker().getId(), k -> new ArrayList<>())
                        .add(p.getPreviewThumbUrl()));

        // 5. Assemblage des DTOs (Tri conservé fidèlement depuis la base de données)
        return workers.stream()
                .map(w -> {
                    // On récupère les previews associées à ce worker précis depuis notre Map optimisée
                    List<String> previews = previewThumbs.getOrDefault(w.getId(), List.of());
                    // On limite à MAX_PREVIEW_THUMBS si ce n'est pas déjà fait dans la requête
                    if (previews.size() > MAX_PREVIEW_THUMBS) {
                        previews = previews.subList(0, MAX_PREVIEW_THUMBS);
                    }
                    return WorkerMinimalProfileDTO.from(w, previews);
                })
                .toList();
    }

    private Specification<Worker> buildDynamicFilters(Map<String, Object> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtrage Géographique par ID
            if (filters.containsKey("zoneId")) {
                Integer zoneId = (Integer) filters.get("zoneId");

                // 1. Jointure explicite en LEFT JOIN pour ne pas perdre les zones sans parent
                var zoneJoin = root.join("geographicZone", JoinType.LEFT);
                var parentJoin = zoneJoin.join("parent", JoinType.LEFT);

                // 2. Condition : Soit la zone est la bonne, soit le parent est la bonne
                Predicate isDirectlyInZone = cb.equal(zoneJoin.get("id"), zoneId);
                Predicate isInSubZone = cb.equal(parentJoin.get("id"), zoneId);

                predicates.add(cb.or(isDirectlyInZone, isInSubZone));
            }

            // --- FILTRE SILHOUETTE ---
            if (filters.containsKey("bodyType")) {
                String bodyVal = filters.get("bodyType").toString().trim();
                if (!bodyVal.isEmpty()) {
                    try {
                        // 🎯 Pas de cb.lower() ! On compare l'enum directement.
                        BodyType enumValue = BodyType.valueOf(bodyVal.toUpperCase());
                        predicates.add(cb.equal(root.get("bodyType"), enumValue));
                    } catch (IllegalArgumentException e) {
                        // Si la chaîne reçue ne correspond à aucun Enum (ex: chaîne vide ou invalide)
                    }
                }
            }

            // --- FILTRE YEUX ---
            if (filters.containsKey("eyeColor")) {
                String eyeVal = filters.get("eyeColor").toString().trim();
                if (!eyeVal.isEmpty()) {
                    try {
                        EyeColor enumValue = EyeColor.valueOf(eyeVal.toUpperCase());
                        predicates.add(cb.equal(root.get("eyeColor"), enumValue));
                    } catch (IllegalArgumentException e) {
                        // Ignoré si invalide
                    }
                }
            }

            // --- FILTRE CHEVEUX ---
            if (filters.containsKey("hairColor")) {
                String hairVal = filters.get("hairColor").toString().trim();
                if (!hairVal.isEmpty()) {
                    try {
                        HairColor enumValue = HairColor.valueOf(hairVal.toUpperCase());
                        predicates.add(cb.equal(root.get("hairColor"), enumValue));
                    } catch (IllegalArgumentException e) {
                        // Ignoré si invalide
                    }
                }
            }

            // 4. Filtre Username & Services (Inchangés mais sécurisés)
            if (filters.containsKey("username")) {
                String searchName = filters.get("username").toString().toLowerCase().trim();
                if (!searchName.isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("username")), "%" + searchName + "%"));
                }
            }
            if (filters.containsKey("services")) {
                List<?> services = (List<?>) filters.get("services");
                if (!services.isEmpty()) {
                    query.distinct(true);
                    predicates.add(root.join("services").get("name").in(services));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static int calculateAge(java.util.Date birthday) {
        if (birthday == null) return 0;
        java.time.LocalDate birthDate = (birthday instanceof java.sql.Date sqlDate)
                ? sqlDate.toLocalDate()
                : birthday.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    /**
     * Returns gallery DTOs for a specific list of worker IDs.
     * Used by GET /account/favorites to return a client's saved workers.
     */
    @Transactional(readOnly = true)
    public List<WorkerMinimalProfileDTO> getGalleryByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // 1. Un seul appel SQL pour récupérer tous les workers concernés
        List<Worker> workersFromDb = workerRepository.findAllById(ids);

        System.out.println("GetGallery all : " + Arrays.toString(workersFromDb.stream().map(Worker::getUsername).toArray()));

        // 2. Filtrage, mapping et tri en une seule passe propre
        return workersFromDb.stream()
                .filter(worker -> !worker.isDisabled()) // Exclure les désactivés
                .map(WorkerMinimalProfileDTO::from)     // Utilise la surcharge à 1 paramètre automatiquement
                .sorted()                               // Utilise la méthode compareTo implémentée dans le Record
                .toList();
    }

}