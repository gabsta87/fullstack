package com.serv.support;

import tools.jackson.databind.ObjectMapper;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.main.ServApplication;
import com.serv.services.AbuseProtectionService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * Base class for end-to-end tests: real Spring context, real security filter chain, real JWTs,
 * in-memory H2 database (wiped before each test) and a temporary media directory.
 * <p>
 * Tests deliberately do NOT run inside a transaction, so lazy-loading and transaction
 * boundaries behave exactly as in production.
 */
@SpringBootTest(classes = ServApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    public static final String PASSWORD = "Correct-Horse-42";

    protected static final Path MEDIA_DIR;
    protected static final Path PRIVATE_MEDIA_DIR;

    static {
        try {
            MEDIA_DIR = Files.createTempDirectory("venus-test-media");
            PRIVATE_MEDIA_DIR = Files.createTempDirectory("venus-test-private-media");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void mediaProperties(DynamicPropertyRegistry registry) {
        registry.add("media.upload.base", MEDIA_DIR::toString);
        registry.add("media.public.base-url", () -> "/media");
        registry.add("media.private.base", PRIVATE_MEDIA_DIR::toString);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JwtProvider jwtProvider;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected TransactionTemplate tx;
    @Autowired protected AbuseProtectionService abuseProtection;

    @Autowired protected UserRepository userRepository;
    @Autowired protected WorkerRepository workerRepository;
    @Autowired protected ClientRepository clientRepository;
    @Autowired protected AdminRepository adminRepository;
    @Autowired protected ServiceRepository serviceRepository;
    @Autowired protected GeographicZoneRepository zoneRepository;
    @Autowired protected PhotoRepository photoRepository;
    @Autowired protected CommentRepository commentRepository;

    @MockitoBean protected JavaMailSender mailSender;
    /** ffmpeg is not needed by the tests: each video test says what the "transcoder" does. */
    @MockitoBean protected com.serv.services.video.VideoTranscoder videoTranscoder;

    @BeforeEach
    void resetState() throws IOException {
        wipeDatabase();
        wipeMedia();
        abuseProtection.clearAll();
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    private void wipeDatabase() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE LOWER(table_schema) = 'public' AND table_type = 'BASE TABLE'",
                String.class);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            for (String table : tables) {
                jdbc.execute("TRUNCATE TABLE \"" + table + "\" RESTART IDENTITY");
            }
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private static void wipeMedia() throws IOException {
        for (Path root : List.of(MEDIA_DIR, PRIVATE_MEDIA_DIR)) {
            if (!Files.exists(root)) continue;
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(root))
                        .forEach(p -> p.toFile().delete());
            }
        }
    }

    // ── Factories ────────────────────────────────────────────────────────────

    protected Client createClient(String email) {
        Client client = new Client(new Email(email), PASSWORD);
        client.setUsername(email.substring(0, email.indexOf('@')));
        return clientRepository.save(client);
    }

    /** A freshly registered worker with an empty (thus not publicly visible) profile. */
    protected Worker createWorker(String email) {
        Worker worker = new Worker(new Email(email), PASSWORD);
        return workerRepository.save(worker);
    }

    /** A worker whose profile is complete and visible in the public gallery. */
    protected Worker createVisibleWorker(String email) {
        GeographicZone zone = zoneRepository.save(new GeographicZone("Zone-" + UUID.randomUUID(), null));
        Service service = new Service();
        service.setName("Service-" + UUID.randomUUID());
        service = serviceRepository.save(service);

        Worker worker = new Worker(new Email(email), PASSWORD);
        worker.setUsername("visible-" + email.substring(0, email.indexOf('@')));
        worker.setDescription("A complete description");
        worker.setShortDescription("Short");
        worker.setPhone("+41790000000");
        worker.setGeographicZone(zone);
        worker.setServices(new java.util.ArrayList<>(List.of(service)));
        worker.setBirthdate(LocalDate.now().minusYears(25));
        worker.setInvalid(false);
        worker = workerRepository.save(worker);

        Photo photo = new Photo();
        photo.setWorker(worker);
        photo.setUrl("/media/" + worker.getId() + "/originals/seed.jpg");
        photo.setMainThumbUrl("/media/" + worker.getId() + "/thumbs/main/seed_main.jpg");
        photo = photoRepository.save(photo);

        Worker reloaded = workerRepository.findByIdWithPhotos(worker.getId()).orElseThrow();
        reloaded.setMainPhoto(photo);
        return workerRepository.save(reloaded);
    }

    protected Admin createAdmin(String email) {
        return adminRepository.save(new Admin(email, new Email(email), PASSWORD));
    }

    protected SuperAdmin createSuperAdmin(String email) {
        return userRepository.save(new SuperAdmin(email, new Email(email), PASSWORD));
    }

    protected String bearer(VenusUser user) {
        return "Bearer " + jwtProvider.generateToken(user);
    }

    protected <T extends VenusUser> T reload(T user) {
        @SuppressWarnings("unchecked")
        T fresh = (T) userRepository.findById(user.getId()).orElseThrow();
        return fresh;
    }

    protected String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
