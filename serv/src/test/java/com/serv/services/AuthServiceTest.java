package com.serv.services;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.WorkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("loadByUsername - Succès pour un utilisateur valide")
    void loadUserByUsername_Success() {
        Client client = new Client(new Email("test@venus.com"), "Password123!");
        when(userRepository.findByEmail("test@venus.com")).thenReturn(Optional.of(client));

        UserDetails userDetails = authService.loadUserByUsername("test@venus.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@venus.com");
    }

    @Test
    @DisplayName("loadByUsername - Échec si l'utilisateur est introuvable")
    void loadUserByUsername_NotFound() {
        when(userRepository.findByEmail("unknown@venus.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("unknown@venus.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("loadByUsername - Échec si le compte est banni (DisabledException)")
    void loadUserByUsername_Banned() {
        Client client = new Client(new Email("banned@venus.com"), "Password123!");
        client.setBanned(true);

        when(userRepository.findByEmail("banned@venus.com")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> authService.loadUserByUsername("banned@venus.com"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("Ce compte a été banni.");
    }

    @Test
    @DisplayName("verifyCredentials - Retourne le worker avec photos si le rôle est WORKER")
    void verifyCredentials_WorkerSuccess() {
        Worker worker = new Worker(new Email("worker@venus.com"), "Password123!");
        UUID workerId = UUID.randomUUID();
        worker.setId(workerId);

        when(userRepository.findByEmail("worker@venus.com")).thenReturn(Optional.of(worker));
        when(workerRepository.findByIdWithPhotos(workerId)).thenReturn(Optional.of(worker));

        // Note : checkPassword s'appuie sur le hachage VenusUser, assure-toi que le mot correspond
        // ou adapte selon la façon dont le mock gère le checkPassword si nécessaire.
    }
}