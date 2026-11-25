package com.serv.services;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    /** Hash compared when the account does not exist, so response time does not reveal registered emails. */
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("venus-timing-equalizer");

    public VenusUser verifyCredentials(String email, String password){
        Optional<VenusUser> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            new BCryptPasswordEncoder().matches(password == null ? "" : password, DUMMY_HASH);
            return null;
        }

        if(user.get().checkPassword(password)){
            if(user.get().getRole() == UserRole.WORKER)
                return workerRepository.findByIdWithPhotos(user.get().getId()).orElse(null);

            return user.get();
        }
        return null;
    }

}
