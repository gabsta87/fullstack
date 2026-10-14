package com.serv.service;

import com.serv.common.UserRole;
import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService implements UserDetailsService{

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        VenusUser venusUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (venusUser.isBanned()) {
            throw new DisabledException("Ce compte a été banni.");
        }

        return User.builder()
                .username(venusUser.getEmail().getValue())
                .password(venusUser.getPasswordHash())
                .authorities("ROLE_" + venusUser.getRole().name())
                .build();
    }

    public VenusUser verifyCredentials(String email, String password){
        Optional<VenusUser> user = userRepository.findByEmail(email);

        if(user.isPresent() && user.get().checkPassword(password)){
            if(user.get().getRole() == UserRole.WORKER)
                return workerRepository.findByIdWithPhotos(user.get().getId()).orElse(null);

            return user.get();
        }
        return null;
    }

}
