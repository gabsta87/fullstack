package com.serv.service;

import com.serv.common.*;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.repositories.filters.WorkerSpecifications;
import com.serv.dto.WorkerMinimalProfileDTO;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class WorkerService {

    @Autowired private WorkerRepository workerRepository;
    @Autowired private PhotoRepository  photoRepository;

    public static final int MAX_PREVIEW_THUMBS = 5;
    public static final int PAGE_SIZE          = 24;

    public List<WorkerMinimalProfileDTO> getGalleryPage(int page, Map<String, Object> filters) {

        System.out.println("Applying " + filters.size() + " filters : " + filters);

        if(page < 0){
            page = 0;
            System.out.println("Page " + page+" corrected to 0");
        }

        // 1. On prépare la base : Uniquement les profils non désactivés + les filtres dynamiques
        Specification<Worker> spec = Specification
                .where(WorkerSpecifications.isNotDisabled())
                .and(buildDynamicFilters(filters));

        // 2. LA MAGIE DU MULTI-TRI :
        // D'abord 'available' (true avant false), puis 'galleryPositionIndex' par ordre décroissant
        Sort doubleSort = Sort.by(Sort.Direction.DESC, "available")
                .and(Sort.by(Sort.Direction.DESC, "galleryPositionIndex"));
//                .and(Sort.by(Sort.Direction.ASC, "id"));

        // 3. Une seule et unique requête propre, paginée au niveau SQL
        List<Worker> workers = workerRepository.findAll(
                spec,
                PageRequest.of(page, PAGE_SIZE, doubleSort)
        ).getContent();

        System.out.println("found workers : "+workers.size());

        if (workers.isEmpty()) return List.of();

        // Batch fetch des miniatures de preview, groupées par workerId
        List<UUID> workerIds = workers.stream().map(Worker::getId).toList();
        Map<UUID, List<String>> previewThumbs = new HashMap<>();
        photoRepository
                .findByWorkerIdInOrderBySortOrderAsc(workerIds)
                .forEach(p -> previewThumbs
                        .computeIfAbsent(p.getWorker().getId(), k -> new ArrayList<>())
                        .add(p.getPreviewThumbUrl()));

        // 4. Assemblage des DTOs (Tri conservé fidèlement depuis la base de données)
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

            // 1. Cas particulier complexe : Filtrage Géographique (On garde ta logique de Jointure)
            if (filters.containsKey("zoneId") && filters.get("zoneId") != null) {
                try {
                    Integer zoneId = Integer.parseInt(filters.get("zoneId").toString().trim());
                    var zoneJoin = root.join("geographicZone", JoinType.LEFT);
                    var parentJoin = zoneJoin.join("parent", JoinType.LEFT);
                    Predicate isDirectlyInZone = cb.equal(zoneJoin.get("id"), zoneId);
                    Predicate isInSubZone = cb.equal(parentJoin.get("id"), zoneId);
                    predicates.add(cb.or(isDirectlyInZone, isInSubZone));
                } catch (NumberFormatException ignored) {}
            }

            // 2. 🎯 UTILISATION DE LA METHODE GENERIQUE POUR LES ENUMS
            addEqualPredicate(filters, "bodyType", "bodyType", predicates, root, cb, v -> BodyType.valueOf(v.toUpperCase()));
            addEqualPredicate(filters, "eyeColor", "eyeColor", predicates, root, cb, v -> EyeColor.valueOf(v.toUpperCase()));
            addEqualPredicate(filters, "hairColor", "hairColor", predicates, root, cb, v -> HairColor.valueOf(v.toUpperCase()));
            addEqualPredicate(filters, "gender", "gender", predicates, root, cb, v -> Gender.valueOf(v.toUpperCase()));

            addAgePredicate(filters, "minAge", predicates, root, cb, true);
            addAgePredicate(filters, "maxAge", predicates, root, cb, false);

            if (filters.containsKey("username") && filters.get("username") != null) {
                String searchName = filters.get("username").toString().toLowerCase().trim();
                if (!searchName.isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("username")), "%" + searchName + "%"));
                }
            }

            // 5. Cas des Collections : Services et Langues
            // Filtre Services
            if (filters.containsKey("services")) {
                List<?> services = (List<?>) filters.get("services");
                if (services != null && !services.isEmpty()) {
                    query.distinct(true);
                    predicates.add(root.join("services").get("name").in(services));
                }
            }

            // 🎯 NOUVEAU : Filtre Langues (Intégration de ta nouvelle demande !)
            if (filters.containsKey("languages")) {
                List<?> languages = (List<?>) filters.get("languages");
                if (languages != null && !languages.isEmpty()) {
                    query.distinct(true);

                    // 1. On convertit d'abord les Strings reçues en Enums "Language" valides
                    List<Language> enumLanguages = languages.stream()
                            .map(obj -> Language.fromRepositoryOrString(obj.toString()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

                    if (!enumLanguages.isEmpty()) {
                        // 2. On fait un JOIN sur la collection embedded 'spokenLanguages'
                        var spokenLangJoin = root.join("spokenLanguages");

                        // 3. On cible le champ 'language' à l'intérieur de l'objet WorkerLanguage
                        predicates.add(spokenLangJoin.get("language").in(enumLanguages));
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private <T> void addEqualPredicate(Map<String, Object> filters, String paramName, String dbAttributeName,
                                       List<Predicate> predicates, Root<Worker> root,
                                       CriteriaBuilder cb, java.util.function.Function<String, T> converter) {
        if (filters.containsKey(paramName) && filters.get(paramName) != null) {
            String rawValue = filters.get(paramName).toString().trim();
            if (!rawValue.isEmpty()) {
                try {
                    T convertedValue = converter.apply(rawValue);
                    predicates.add(cb.equal(root.get(dbAttributeName), convertedValue));
                } catch (Exception ignored) {}
            }
        }
    }

    private void addAgePredicate(Map<String, Object> filters, String paramName, List<Predicate> predicates,
                                 jakarta.persistence.criteria.Root<Worker> root, jakarta.persistence.criteria.CriteriaBuilder cb, boolean isMin) {
        if (filters.containsKey(paramName) && filters.get(paramName) != null) {
            try {
                String rawValue = filters.get(paramName).toString().trim();
                if (rawValue.isEmpty()) return;

                if (rawValue.contains(".")) {
                    rawValue = rawValue.split("\\.")[0];
                }

                int ageVal = Integer.parseInt(rawValue);
                LocalDate today = LocalDate.now();

                // 1. 🎯 On type explicitement en Path<Date> pour aider le CriteriaBuilder
                Path<Date> birthDateAttribute = root.get("birthdate");

                if (isMin) {
                    LocalDate maxBirthDateForMinAge = today.minusYears(ageVal);

                    // 2. 🎯 Conversion de LocalDate vers java.util.Date
                    Date targetDate = java.sql.Date.valueOf(maxBirthDateForMinAge);

                    predicates.add(cb.lessThanOrEqualTo(birthDateAttribute, targetDate));
                } else {
                    LocalDate minBirthDateForMaxAge = today.minusYears(ageVal + 1);

                    // 2. 🎯 Conversion de LocalDate vers java.util.Date
                    Date targetDate = java.sql.Date.valueOf(minBirthDateForMaxAge);

                    predicates.add(cb.greaterThan(birthDateAttribute, targetDate));
                }

                System.out.println("Filtre Age [" + paramName + "=" + ageVal + "] appliqué avec succès.");

            } catch (Exception e) {
                System.err.println("Échec de l'application du filtre d'âge pour " + paramName + ": " + e.getMessage());
                e.printStackTrace(); // Utile pour voir les détails de l'erreur au cas où
            }
        }
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