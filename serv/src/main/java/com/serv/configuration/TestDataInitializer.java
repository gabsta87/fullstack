package com.serv.configuration;

import com.serv.common.*;
import com.serv.database.entities.*;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.database.repositories.WorkerRepository;
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

    @Value("${media.public.base-url}")
    private String mediaBase;

    @Override
    public void run(ApplicationArguments args) {

        // 1. 🌍 INITIALISATION DES ZONES GEOGRAPHIQUES
        if (zoneRepository.findAll().isEmpty()) {
            System.out.println("[TestDataInitializer] Inserting test geographic zones...");
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
            System.out.println("[TestDataInitializer] Data (GeographicZones) already present — skipping.");
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

        System.out.println("[TestDataInitializer] Generating 2500 random workers...");

        List<Service> allServices = serviceRepository.findAll();
        List<GeographicZone> allZones = zoneRepository.findAll();

        // 📝 Banques de données de 50 prénoms et 50 noms
        List<String> firstNames = Arrays.asList(
                "Amélie", "Sofia", "Léa", "Camille", "Inès", "Zoé", "Manon", "Chloé", "Emma", "Jade",
                "Sarah", "Eva", "Clara", "Anna", "Alice", "Lucie", "Mila", "Elena", "Maya", "Nina",
                "Lina", "Mia", "Lola", "Julia", "Romane", "Louise", "Juliette", "Agathe", "Ines", "Clemence",
                "Maxime", "Lucas", "Thomas", "Hugo", "Enzo", "Nathan", "Leo", "Louis", "Arthur", "Gabriel",
                "Alexandre", "Antoine", "Julien", "Rayan", "Florian", "Clement", "Mathéo", "Paul", "Alexis", "Quentin"
        );

        List<String> lastNames = Arrays.asList(
                "Martin", "Bernard", "Thomas", "Petit", "Robert", "Richard", "Durand", "Dubois", "Moreau", "Laurent",
                "Simon", "Michel", "Lefebvre", "Legrand", "Garcia", "Rousseau", "Fournier", "Bonner", "Dupont", "Fontaine",
                "Lopez", "Gomez", "Muller", "Schmitt", "Masson", "Sanchez", "Clerc", "Denis", "Hubert", "Gautier",
                "Perrin", "Roussel", "Mathieu", "Chevalier", "Francois", "Duval", "Joly", "Guerin", "Lemaire", "Roux",
                "Roy", "Aubert", "Giraud", "Henry", "Barbier", "Brun", "Dumas", "Brunet", "Mercier", "Roger"
        );

        // Récupération des valeurs d'Enums sous forme de tableaux pour le tirage aléatoire
        BodyType[] bodyTypes = BodyType.values();
        Gender[] genders = Gender.values();
        EyeColor[] eyeColors = EyeColor.values();
        HairColor[] hairColors = HairColor.values();

        Random random = new Random();
        String encodedPassword = passwordEncoder.encode("asdfasdf!");

        int totalGenerated = 0;

        // Boucles imbriquées pour faire chaque combinaison unique
        for (String firstName : firstNames) {
            for (String lastName : lastNames) {

                // Génération de l'identité et de l'adresse email uniques
                String displayName = firstName + " " + lastName;
                String username = (firstName + "." + lastName).toLowerCase()
                        .replaceAll("[éèêëàâäîïôöûüç]", "e"); // Normalisation basique
                String email = username + "@test.com";

                // Calcul d'une date de naissance aléatoire entre 1960 et 2009 (entre 18 et 66 ans)
                int birthYear = ThreadLocalRandom.current().nextInt(1960, 2009);
                int birthMonth = ThreadLocalRandom.current().nextInt(1, 13);
                int birthDay = ThreadLocalRandom.current().nextInt(1, 29);
                Date birthday = date(birthYear, birthMonth, birthDay);

                // Tirage aléatoire des critères physiques et de la localisation
                GeographicZone randomZone = allZones.get(random.nextInt(allZones.size()));
                BodyType randomBody = bodyTypes[random.nextInt(bodyTypes.length)];
                Gender randomGender = Gender.FEMALE;
                EyeColor randomEye = eyeColors[random.nextInt(eyeColors.length)];
                HairColor randomHair = hairColors[random.nextInt(hairColors.length)];

                // Configuration de l'état du profil (70% de chances d'être disponible)
                boolean available = random.nextDouble() < 0.70;
                Instant lastRefreshed = Instant.now().minusSeconds(random.nextInt(259200)); // refresh dans les 3 derniers jours

                // Détermination des services (1 à 3 services aléatoires)
                Collections.shuffle(allServices);
                int servicesCount = random.nextInt(3) + 1;
                List<Service> selectedServices = new ArrayList<>(allServices.subList(0, servicesCount));

                // Attribution de crédit de jours aléatoire (de 0 à 30 jours) pour simuler l'état de la plateforme
                int remainingDays = random.nextInt(31);

                // Création et sauvegarde de l'entité de test
                createGeneratedWorker(
                        username, email, encodedPassword, displayName, birthday, randomZone,
                        randomBody, randomGender, randomEye, randomHair, available,
                        lastRefreshed, remainingDays, selectedServices
                );

                totalGenerated++;
            }
        }

        System.out.println("[TestDataInitializer] Done — " + totalGenerated + " massive workers successfully inserted.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private GeographicZone createZone(String name, GeographicZone parent) {
        return zoneRepository.save(new GeographicZone(name, parent));
    }

    private void createGeneratedWorker(
            String username, String email, String encodedPassword, String displayName,
            Date birthday, GeographicZone zone,
            BodyType bodyType, Gender gender, EyeColor eyeColor, HairColor hairColor,
            boolean available, Instant lastRefreshed, int remainingDays, List<Service> services) {

        Worker w = new Worker(username, new Email(email), encodedPassword);

        // Paramètres standards
        w.setUsername(displayName); // Utilise le set de nom approprié selon ton modèle
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

        // 🎯 Intégration de tes nouvelles propriétés d'abonnement
        w.setRemainingDaysCredit(remainingDays);

        // Si le crédit attribué est supérieur à 0 et qu'il est disponible, active-le
        // (La méthode setActive déclenchera automatiquement le hasBeenActiveToday interne !)
        if (remainingDays > 0 && available) {
            w.setActive(true);
        } else {
            w.setActive(false);
        }

        workerRepository.save(w);

        // Simulation d'une photo par défaut pour ne pas ralentir l'initialisation de masse
        // en créant trop d'entités photos secondaires pour chaque combinaison.
        String placeholderPhoto = "test_profile_placeholder.jpg";
        Photo main = new Photo();
        main.setWorker(w);
        main.setFileName(placeholderPhoto);
        main.setSortOrder(0);
        main.setOriginalUrl(mediaBase + "/originals/test/" + placeholderPhoto);
        main.setMainThumbUrl(mediaBase + "/thumbs/main/test/" + placeholderPhoto);
        main.setPreviewThumbUrl(mediaBase + "/thumbs/preview/test/" + placeholderPhoto);

        photoRepository.save(main);
        w.setMainPhoto(main);
        workerRepository.save(w);
    }

    private Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}