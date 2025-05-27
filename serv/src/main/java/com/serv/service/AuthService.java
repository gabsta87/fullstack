package com.serv.service;

import com.serv.database.VenusUser;
import com.serv.database.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private UserRepository userRepository;

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

}
