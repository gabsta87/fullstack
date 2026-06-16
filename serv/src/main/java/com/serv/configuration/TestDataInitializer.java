package com.serv.configuration;

import com.serv.database.entities.*;
import com.serv.common.BodyType;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.repositories.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Inserts test data on startup — only in "dev" profile, only if the
 * workers table is empty. Safe to leave in: it will never overwrite
 * or duplicate existing data.
 *
 * Activate with: spring.profiles.active=dev
 */
@Component
@Profile("dev")
public class TestDataInitializer implements ApplicationRunner {

    @Autowired private ServiceRepository serviceRepository;
    @Autowired private WorkerRepository  workerRepository;
    @Autowired private PhotoRepository   photoRepository;
    @Autowired private GeographicZoneRepository zoneRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    // Public base URL for media — must match application.properties
    @Value("${media.public.base-url}")
    private String mediaBase;

    @Override
    public void run(ApplicationArguments args) {

        // 1. 🌍 INITIALISATION DES ZONES GÉOGRAPHIQUES
        if (zoneRepository.findAll().isEmpty()) {
            System.out.println("[TestDataInitializer] Inserting test geographic zones...");

            // Parents (Villes principales)
            GeographicZone paris = createZone("Paris", null);
            GeographicZone lyon = createZone("Lyon", null);
            createZone("Bordeaux", null); // Sans enfant pour le test
            createZone("Marseille", null);
            createZone("Nice", null);
            createZone("Toulouse", null);

            // Enfants (Arrondissements et Quartiers)
            createZone("Paris 8e", paris);
            createZone("Paris 11e", paris);
            createZone("Lyon 2e", lyon);
            createZone("Vieux Lyon", lyon);
        } else {
            System.out.println("[TestDataInitializer] Data (Zones) already present — skipping.");
        }

        // 2. 🛠️ INITIALISATION DES SERVICES
        if (serviceRepository.findAll().isEmpty()) {
            System.out.println("[TestDataInitializer] Inserting test services...");
            serviceRepository.save(new Service("Inter"));
            serviceRepository.save(new Service("No_Sex"));
            serviceRepository.save(new Service("Fetish"));
            serviceRepository.save(new Service("BDSM"));
        } else {
            System.out.println("[TestDataInitializer] Data (Services) already present — skipping.");
        }

        // 3. 👩‍💼 INITIALISATION DES WORKERS
        if (!workerRepository.findAll().isEmpty()) {
            System.out.println("[TestDataInitializer] Data (Workers) already present — skipping.");
            return;
        }

        System.out.println("[TestDataInitializer] Inserting test workers...");

        Service firstService = serviceRepository.findAll().getFirst();
        Service secondService = serviceRepository.findAll().get(1);

        // Récupération des zones pour les assigner aux workers
        GeographicZone paris8 = zoneRepository.findByName("Paris 8e").orElseThrow();
        GeographicZone paris11 = zoneRepository.findByName("Paris 11e").orElseThrow();
        GeographicZone lyon2 = zoneRepository.findByName("Lyon 2e").orElseThrow();
        GeographicZone bordeaux = zoneRepository.findByName("Bordeaux").orElseThrow();
        GeographicZone marseille = zoneRepository.findByName("Marseille").orElseThrow();
        GeographicZone nice = zoneRepository.findByName("Nice").orElseThrow();
        GeographicZone toulouse = zoneRepository.findByName("Toulouse").orElseThrow();

        createWorker(
                "amelie",   "amelie@test.com",   "Amélie",
                date(1998, 3, 12), paris8,
                BodyType.ATHLETIC, true, Instant.now(),
                "amelie_main.jpg", List.of("amelie_1.jpg", "amelie_2.jpg", "amelie_3.jpg"),
                firstService, secondService
        );

        createWorker(
                "sofia",    "sofia@test.com",    "Sofia",
                date(2001, 7, 4), lyon2,
                BodyType.SLIM, true, Instant.now().minusSeconds(3600),
                "sofia_main.jpg",  List.of("sofia_1.jpg", "sofia_2.jpg"),
                firstService
        );

        createWorker(
                "lea",      "lea@test.com",      "Léa",
                date(1995, 11, 20), paris11,
                BodyType.AVERAGE, true, Instant.now().minusSeconds(7200),
                "lea_main.jpg",    List.of("lea_1.jpg"),
                firstService
        );

        createWorker(
                "camille",  "camille@test.com",  "Camille",
                date(1999, 5, 8), bordeaux,
                BodyType.CURVY, true, Instant.now().minusSeconds(10800),
                "camille_main.jpg",List.of("camille_1.jpg", "camille_2.jpg"),
                firstService
        );

        createWorker(
                "ines",     "ines@test.com",     "Inès",
                date(1993, 9, 15), marseille,
                BodyType.SLIM, true, Instant.now().minusSeconds(14400),
                null, List.of(),
                secondService
        );

        // Unavailable workers (to test the "offline" section)
        createWorker(
                "zoe",      "zoe@test.com",      "Zoé",
                date(2000, 2, 28), nice,
                BodyType.ATHLETIC, false, Instant.now().minusSeconds(86400),
                "zoe_main.jpg",    List.of("zoe_1.jpg"),
                firstService
        );

        createWorker(
                "manon",    "manon@test.com",    "Manon",
                date(1997, 6, 3), toulouse,
                BodyType.AVERAGE, false, Instant.now().minusSeconds(172800),
                null, List.of(),
                firstService
        );

        System.out.println("[TestDataInitializer] Done — " +
                workerRepository.findAll().size() + " workers inserted.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private GeographicZone createZone(String name, GeographicZone parent) {
        GeographicZone zone = new GeographicZone(name, parent);
        return zoneRepository.save(zone);
    }

    private void createWorker(
            String username, String email, String displayName,
            Date birthday, GeographicZone zone,
            BodyType bodyType, boolean available,
            Instant lastRefreshed,
            String mainPhotoFile, List<String> extraPhotoFiles, Service... services) {

        Worker w = new Worker(
                username,
                new Email(email),
                passwordEncoder.encode("Test1234!")
        );

        w.setAvailable(available);
        w.setLastRefreshed(lastRefreshed);
        w.setGeographicZone(zone); // 🎯 Utilisation de l'objet au lieu des Strings
        w.setBodyType(bodyType);
        w.setBirthday(birthday);
        w.setServices(Arrays.asList(services));
        w.setGalleryPositionIndex(0);
        w.setExpired(false);

        workerRepository.save(w);

        // Main photo
        if (mainPhotoFile != null) {
            Photo main = buildPhoto(w, mainPhotoFile, 0);
            photoRepository.save(main);
            w.setMainPhoto(main);
            workerRepository.save(w); // Mise à jour de la relation OneToOne
        }

        // Extra photos
        int order = 1;
        for (String file : extraPhotoFiles) {
            Photo p = buildPhoto(w, file, order++);
            photoRepository.save(p);
        }
    }

    private Photo buildPhoto(Worker w, String filename, int order) {
        Photo p = new Photo();
        p.setWorker(w);
        p.setFileName(filename);
        p.setSortOrder(order);
        p.setOriginalUrl(mediaBase    + "/originals/test/"      + filename);
        p.setMainThumbUrl(mediaBase   + "/thumbs/main/test/"    + filename);
        p.setPreviewThumbUrl(mediaBase+ "/thumbs/preview/test/" + filename);
        return p;
    }

    private Date date(int year, int month, int day) {
        return Date.from(
                LocalDate.of(year, month, day)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
        );
    }
}