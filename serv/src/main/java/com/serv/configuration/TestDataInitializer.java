package com.serv.configuration;

import com.serv.common.*;
import com.serv.database.entities.*;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.database.repositories.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inserts test data on startup — only in "dev" profile, only if the
 * workers table is empty. Safe to leave in: it will never overwrite
 * or duplicate existing data.
 * Activate with: spring.profiles.active=dev
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class TestDataInitializer implements ApplicationRunner {

    private final ServiceRepository serviceRepository;
    private final WorkerRepository  workerRepository;
    private final GeographicZoneRepository zoneRepository;
    private final PasswordEncoder   passwordEncoder;

    @Value("${media.public.base-url}")
    private String mediaBase;

    @Override
    public void run(ApplicationArguments args) {

        // 1. GEOGRAPHIC ZONES
        if (zoneRepository.findAll().isEmpty()) {
            log.info("Inserting test geographic zones...");
            GeographicZone paris = createZone("Paris", null);
            GeographicZone lyon = createZone("Lyon", null);
            createZone("Bordeaux", null);
            createZone("Marseille", null);
            createZone("Nice", null);
            createZone("Toulouse", null);

            createZone("Paris 8e", paris);
            createZone("Paris 11e", paris);
            createZone("Lyon 2e", lyon);
            createZone("Vieux Lyon", lyon);
        } else {
            log.info("Data (GeographicZones) already present — skipping.");
        }

        // 2. 🛠️ INITIALISATION DES SERVICES
        if (serviceRepository.findAll().isEmpty()) {
            log.info("Inserting test services...");
            serviceRepository.save(new Service("Inter"));
            serviceRepository.save(new Service("No_Sex"));
            serviceRepository.save(new Service("Fetish"));
            serviceRepository.save(new Service("BDSM"));
        } else {
            log.info("Data (Services) already present — skipping.");
        }

        // 3. 👩‍💼 INITIALISATION DES WORKERS
        if (!workerRepository.findAll().isEmpty()) {
            log.info("Data (Workers) already present — skipping.");
            return;
        }

        log.info("Generating 2500 random workers...");

        List<Service> allServices = serviceRepository.findAll();
        List<GeographicZone> allZones = zoneRepository.findAll();

        // Pools of 50 first names and 50 last names
        List<String> firstNames = Arrays.asList(
                "Amelie", "Sofia", "Lea", "Camille", "Ines", "Zoe", "Manon", "Chloe", "Emma", "Jade",
                "Sarah", "Eva", "Clara", "Anna", "Alice", "Lucie", "Mila", "Elena", "Maya", "Nina",
                "Lina", "Mia", "Lola", "Julia", "Romane", "Louise", "Juliette", "Agathe", "Clemence", "Elodie",
                "Maxime", "Lucas", "Thomas", "Hugo", "Enzo", "Nathan", "Leo", "Louis", "Arthur", "Gabriel",
                "Alexandre", "Antoine", "Julien", "Rayan", "Florian", "Clement", "Matheo", "Paul", "Alexis", "Quentin"
        );

        List<String> lastNames = Arrays.asList(
                "Martin", "Bernard", "Petit", "Robert", "Richard", "Durand", "Dubois", "Moreau", "Laurent", "Simon",
                "Michel", "Lefebvre", "Legrand", "Garcia", "Rousseau", "Fournier", "Bonner", "Dupont", "Fontaine", "Lopez",
                "Gomez", "Muller", "Schmitt", "Masson", "Sanchez", "Clerc", "Denis", "Hubert", "Gautier", "Perrin",
                "Roussel", "Mathieu", "Chevalier", "Francois", "Duval", "Joly", "Guerin", "Lemaire", "Roux", "Roy",
                "Aubert", "Giraud", "Henry", "Barbier", "Brun", "Dumas", "Brunet", "Mercier", "Roger", "Vidal"
        );

        // Enum values as arrays for random picks
        BodyType[] bodyTypes = BodyType.values();
        EyeColor[] eyeColors = EyeColor.values();
        HairColor[] hairColors = HairColor.values();

        Random random = new Random();
        String encodedPassword = passwordEncoder.encode("asdfasdf!");

        int totalGenerated = 0;
        Set<String> generatedEmails = new HashSet<>();

        // Nested loops: every combination is unique
        for (String firstName : firstNames) {
            for (String lastName : lastNames) {

                // Unique identity and email address
                String displayName = firstName + " " + lastName;
                String username = (firstName + "." + lastName).toLowerCase()
                        .replaceAll("[éèêë]", "e")
                        .replaceAll("[àâä]", "a")
                        .replaceAll("[îï]", "i")
                        .replaceAll("[ôö]", "o")
                        .replaceAll("[ûü]", "u")
                        .replaceAll("[ç]", "c");
                String email = username + "@test.com";

                if (generatedEmails.contains(email)) {
                    continue;
                }
                generatedEmails.add(email);

                // Random birthdate between 1960 and 2009 (18 to 66 years old)
                int birthYear = ThreadLocalRandom.current().nextInt(1960, 2009);
                int birthMonth = ThreadLocalRandom.current().nextInt(1, 13);
                int birthDay = ThreadLocalRandom.current().nextInt(1, 29);
                LocalDate birthday = date(birthYear, birthMonth, birthDay);

                // Random physical criteria and location
                GeographicZone randomZone = allZones.get(random.nextInt(allZones.size()));
                BodyType randomBody = bodyTypes[random.nextInt(bodyTypes.length)];
                Gender randomGender = Gender.FEMALE;
                EyeColor randomEye = eyeColors[random.nextInt(eyeColors.length)];
                HairColor randomHair = hairColors[random.nextInt(hairColors.length)];

                // Profile state (70% chance of being available)
                boolean available = random.nextDouble() < 0.70;
                Instant lastRefreshed = Instant.now().minusSeconds(random.nextInt(259200)); // refreshed within the last 3 days

                // 1 to 3 random services
                Collections.shuffle(allServices);
                int servicesCount = random.nextInt(3) + 1;
                List<Service> selectedServices = new ArrayList<>(allServices.subList(0, servicesCount));

                // Random day credit (0 to 30 days) to simulate a live platform
                int remainingDays = random.nextInt(31);

                // Create and save the test entity
                createGeneratedWorker(
                        username, email, encodedPassword, displayName, birthday, randomZone,
                        randomBody, randomGender, randomEye, randomHair, available,
                        lastRefreshed, remainingDays, selectedServices
                );

                totalGenerated++;
            }
        }

        log.info("Done — {} test workers inserted.", totalGenerated);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private GeographicZone createZone(String name, GeographicZone parent) {
        return zoneRepository.save(new GeographicZone(name, parent));
    }

    private void createGeneratedWorker(
            String username, String email, String encodedPassword, String displayName,
            LocalDate birthday, GeographicZone zone,
            BodyType bodyType, Gender gender, EyeColor eyeColor, HairColor hairColor,
            boolean available, Instant lastRefreshed, int remainingDays, List<Service> services) {

        Worker w = new Worker(username, new Email(email), encodedPassword);

        w.setUsername(displayName);
        w.setAvailable(available);
        w.setLastRefreshed(lastRefreshed);
        w.setGeographicZone(zone);
        w.setBodyType(bodyType);
        w.setGender(gender);
        w.setEyeColor(eyeColor);
        w.setHairColor(hairColor);
        w.setBirthdate(birthday);
        w.setServices(services);
        w.setGalleryPositionPriority(0);
        w.addSpokenLanguage(new WorkerLanguage(Language.EN, 3));
        w.setExpired(false);
        w.setRemainingDaysCredit(remainingDays);
        w.setActive(remainingDays > 0 && available);

        workerRepository.save(w);
    }

    private LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }
}