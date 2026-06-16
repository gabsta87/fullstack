package com.serv.service;

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
                .map(WorkerMinimalProfileDTO::from)
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

            // 2. Filtrage par caractéristiques physiques blindé contre la casse (cb.lower)
            if (filters.containsKey("eyeColor")) {
                String eye = filters.get("eyeColor").toString().trim().toLowerCase();
                if (!eye.isEmpty()) {
                    predicates.add(cb.equal(cb.lower(root.get("eyeColor")), eye));
                }
            }
            if (filters.containsKey("hairColor")) {
                String hair = filters.get("hairColor").toString().trim().toLowerCase();
                if (!hair.isEmpty()) {
                    predicates.add(cb.equal(cb.lower(root.get("hairColor")), hair));
                }
            }

            // 3. Traitement robuste de l'Enum BodyType
            if (filters.containsKey("bodyType")) {
                Object bodyTypeData = filters.get("bodyType");
                try {
                    if (bodyTypeData instanceof Collection<?> collection) {
                        List<com.serv.common.BodyType> enums = collection.stream()
                                .map(obj -> com.serv.common.BodyType.valueOf(obj.toString().trim().toUpperCase()))
                                .toList();
                        if (!enums.isEmpty()) predicates.add(root.get("bodyType").in(enums));
                    } else {
                        predicates.add(cb.equal(root.get("bodyType"), com.serv.common.BodyType.valueOf(bodyTypeData.toString().trim().toUpperCase())));
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore si la valeur reçue ne mappe pas l'enum
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

        List<Worker> all = workerRepository.findAllById(ids).stream()
                .toList();

        System.out.println("GetGallery all : "+Arrays.toString(all.stream().map(Worker::getUsername).toArray()));

        List<Worker> available = workerRepository.findAllById(ids).stream()
                .filter(worker -> !worker.isDisabled())
                .toList();

        System.out.println("GetGallery filtered : "+Arrays.toString(available.stream().map(Worker::getUsername).toArray()));

        return workerRepository.findAllById(ids).stream().filter(worker -> !worker.isDisabled())
                .map(WorkerMinimalProfileDTO::from)
                .sorted(WorkerMinimalProfileDTO::compareTo)
                .toList();
    }

}