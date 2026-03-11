package com.serv.configuration;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Worker;
import com.serv.common.BodyType;
import com.serv.common.Service;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.entities.Email;
import com.serv.database.entities.PhysicalAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    @Autowired private WorkerRepository  workerRepository;
    @Autowired private PhotoRepository   photoRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    // Public base URL for media — must match application.properties
    @Value("${media.public.base-url}")
    private String mediaBase;

    @Override
//    @Transactional
    public void run(ApplicationArguments args) {

        // Guard — do nothing if data already exists
        if (!workerRepository.findAll().isEmpty()) {
            System.out.println("[TestDataInitializer] Data already present — skipping.");
            return;
        }

        System.out.println("[TestDataInitializer] Inserting test workers...");

        createWorker(
                "amelie",   "amelie@test.com",   "Amélie",   26,
                date(1998, 3, 12), "Paris 8e",     "Paris",
                BodyType.ATHLETIC, 168, 56,
                List.of(Service.INTER), true,
                Instant.now(),
                "amelie_main.jpg", List.of("amelie_1.jpg", "amelie_2.jpg", "amelie_3.jpg")
        );

        createWorker(
                "sofia",    "sofia@test.com",    "Sofia",    23,
                date(2001, 7, 4),  "Lyon 2e",      "Lyon",
                BodyType.SLIM,     172, 53,
                List.of(Service.NO_S, Service.INTER), true,
                Instant.now().minusSeconds(3600),
                "sofia_main.jpg",  List.of("sofia_1.jpg", "sofia_2.jpg")
        );

        createWorker(
                "lea",      "lea@test.com",      "Léa",      29,
                date(1995, 11, 20),"Paris 11e",   "Paris",
                BodyType.AVERAGE,  165, 60,
                List.of(Service.NO_S), true,
                Instant.now().minusSeconds(7200),
                "lea_main.jpg",    List.of("lea_1.jpg")
        );

        createWorker(
                "camille",  "camille@test.com",  "Camille",  25,
                date(1999, 5, 8),  "Bordeaux",    "Bordeaux",
                BodyType.CURVY,    162, 65,
                List.of(Service.INTER), true,
                Instant.now().minusSeconds(10800),
                "camille_main.jpg",List.of("camille_1.jpg", "camille_2.jpg")
        );

        createWorker(
                "ines",     "ines@test.com",     "Inès",     31,
                date(1993, 9, 15), "Marseille",   "Marseille",
                BodyType.SLIM,     170, 54,
                List.of(Service.NO_S, Service.INTER), true,
                Instant.now().minusSeconds(14400),
                null, List.of()   // no photos yet — tests placeholder rendering
        );

        // Unavailable workers (to test the "offline" section)
        createWorker(
                "zoe",      "zoe@test.com",      "Zoé",      24,
                date(2000, 2, 28), "Nice",        "Nice",
                BodyType.ATHLETIC, 169, 57,
                List.of(Service.NO_S), false,
                Instant.now().minusSeconds(86400),
                "zoe_main.jpg",    List.of("zoe_1.jpg")
        );

        createWorker(
                "manon",    "manon@test.com",    "Manon",    27,
                date(1997, 6, 3),  "Toulouse",    "Toulouse",
                BodyType.AVERAGE,  164, 59,
                List.of(Service.INTER), false,
                Instant.now().minusSeconds(172800),
                null, List.of()
        );

        System.out.println("[TestDataInitializer] Done — " +
                workerRepository.findAll().size() + " workers inserted.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void createWorker(
            String username, String email, String displayName, int age,
            Date birthday, String location, String region,
            BodyType bodyType, int height, int weight,
            List<Service> services, boolean available,
            Instant lastRefreshed,
            String mainPhotoFile, List<String> extraPhotoFiles) {

        Worker w = new Worker(
                username,
                new Email(email),
                passwordEncoder.encode("Test1234!")
        );

        w.setAvailable(available);
        w.setLastRefreshed(lastRefreshed);
        w.setLocation(location);
        w.setRegion(region);
        w.setBodyType(bodyType);
        w.setHeight(height);
        w.setWeight(weight);
        w.setBirthday(birthday);
        w.setServices(services);
        w.setPriority(0);
        w.setExpired(false);

        // Physical address (minimal)
        PhysicalAddress addr = new PhysicalAddress();
        addr.setCity(location);
        w.setAddress(addr);

        workerRepository.save(w);

        // Main photo
        if (mainPhotoFile != null) {
            Photo main = buildPhoto(w, mainPhotoFile, 0);
            photoRepository.save(main);
            w.setMainPhoto(main);
            workerRepository.save(w);
        }

        // Extra photos (for the hover carousel)
        int order = 1;
        for (String file : extraPhotoFiles) {
            Photo p = buildPhoto(w, file, order++);
            photoRepository.save(p);
        }
    }

    /**
     * Builds a Photo entity whose URLs point to placeholder images.
     *
     * In dev, put some real JPEGs inside your media folder at:
     *   ${media.upload.base}/originals/test/
     *   ${media.upload.base}/thumbs/main/test/
     *   ${media.upload.base}/thumbs/preview/test/
     *
     * Or just leave them as-is — the frontend will show the placeholder
     * SVG for any URL that 404s, so the UI is still testable.
     */
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