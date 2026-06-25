package com.serv.service;

import com.serv.common.UserRole;
import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService implements UserDetailsService{

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<VenusUser> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            VenusUser venusUser = userOpt.get();

            return User.builder()
                    .username(venusUser.getEmail().getValue())
                    .password(venusUser.getPasswordHash())
                    .authorities("ROLE_" + venusUser.getRole().name())
                    .build();
        } else {
            throw new UsernameNotFoundException("User with email " + email + " not found");
        }
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
