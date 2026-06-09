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
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDetails loadUserByUsername(String username) {
        Optional<VenusUser> user = userRepository.findByUsername(username);
        if(user.isPresent()) {
            var venusUser = user.get();
            return User.builder()
                    .username(venusUser.getUsername())
                    .password(venusUser.getPasswordHash())
                    .build();
        }else{
            throw new UsernameNotFoundException(username +" not found");
        }
    }

    public VenusUser verifyCredentials(String username, String password){
        Optional<VenusUser> user = userRepository.findByUsername(username);

        if(user.isPresent() && user.get().checkPassword(password)){
            if(user.get().getRole() == UserRole.WORKER)
                return workerRepository.findByIdWithPhotos(user.get().getId()).orElse(null);

            return user.get();
        }
        return null;
    }

}
