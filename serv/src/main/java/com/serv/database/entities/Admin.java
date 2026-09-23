package com.serv.database.entities;

import com.serv.common.UserRole;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@NoArgsConstructor
@Entity
public class Admin extends VenusUser{

    public Admin(String username, Email email, String password) {
        super(email,password);
        this.username = username;
    }

    public boolean isAdmin(){
        return true;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = super.getAuthorities();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return authorities;
    }

    public UserRole getRole(){
        return UserRole.ADMIN;
    }
}
