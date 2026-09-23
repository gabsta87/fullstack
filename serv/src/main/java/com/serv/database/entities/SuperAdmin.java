package com.serv.database.entities;

import com.serv.common.UserRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Entity
@DiscriminatorValue("SUPER_ADMIN")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
public class SuperAdmin extends Admin{
    public SuperAdmin(String username, Email email, String password) {
        super(username, email, password);
    }

    public boolean isSuperAdmin() {return true;}

    @Override
    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = super.getAuthorities();
        authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        return authorities;
    }

    public UserRole getRole(){
        return UserRole.SUPER_ADMIN;
    }
}
