package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.common.ReportStatus;
import com.serv.database.entities.*;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.CommentRepository;
import com.serv.database.repositories.ReportRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.requests.CommentRequest;
import com.serv.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountControllerClient.class)
class AccountControllerClientTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkerRepository workerRepository;

    @MockBean
    private WorkerService galleryService;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private ReportRepository reportRepository;

    @MockBean
    private CommentRepository commentRepository;

    private Client testClient;
    private Worker testWorker;
    private UUID clientId;
    private UUID workerId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        workerId = UUID.randomUUID();

        testClient = new Client();
        testClient.setId(clientId);
        testClient.setEmail(new Email("client@venus.com"));

        testWorker = new Worker();
        testWorker.setId(workerId);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMe() throws Exception {
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));

        // 💡 Utilisation de mockMvc.perform()
        mockMvc.perform(get("/account/client/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testAddFavorite() throws Exception {
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(testWorker));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);

        mockMvc.perform(post("/account/client/favorites/" + workerId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testReportWorker() throws Exception {
        com.serv.requests.Report req = new com.serv.requests.Report(workerId.toString(), "Inappropriate content");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(testWorker));
        when(reportRepository.findByWorkerAndAuthor(testWorker, testClient)).thenReturn(Optional.empty());
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/account/client/report-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testWriteComment() throws Exception {
        CommentRequest req = new CommentRequest(workerId.toString(), "Great service!");

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(testWorker));
        when(commentRepository.findByAuthorAndWorker(testClient, testWorker)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/account/client/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testAddFavoriteWorkerNotFound() throws Exception {
        when(workerRepository.findById(workerId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/account/client/favorites/" + workerId))
                .andExpect(status().isNotFound());
    }

    // 🛡️ CAS LIMITE : Signalement en double / conflit (409 Conflict)
    @Test
    @WithMockUser(roles = "CLIENT")
    void testReportWorkerConflict() throws Exception {
        com.serv.requests.Report req = new com.serv.requests.Report(workerId.toString(), "Spam");
        Report existingReport = new Report(testWorker, testClient, "Spam");
        existingReport.setStatus(ReportStatus.RESOLVED); // Statut qui bloque la modification

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(testWorker));
        when(reportRepository.findByWorkerAndAuthor(testWorker, testClient)).thenReturn(Optional.of(existingReport));

        mockMvc.perform(post("/account/client/report-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // 🛡️ CAS LIMITE : Tentative de supprimer le commentaire d'un autre (403 Forbidden)
    @Test
    @WithMockUser(roles = "CLIENT")
    void testDeleteCommentForbidden() throws Exception {
        Long commentId = 99L;
        Client anotherClient = new Client();
        anotherClient.setId(UUID.randomUUID()); // Un autre auteur

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(anotherClient); // Le commentaire n'appartient pas au client connecté

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        mockMvc.perform(delete("/account/client/comment/" + commentId))
                .andExpect(status().isForbidden());
    }
}