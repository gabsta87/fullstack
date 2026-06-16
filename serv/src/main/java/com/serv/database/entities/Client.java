package com.serv.database.entities;

import com.serv.common.TablesNames;
import com.serv.common.UserRole;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("CLIENT")
@Table(name = TablesNames.CLIENTS)
public class Client extends VenusUser {

    @OneToMany(fetch = FetchType.EAGER)
    Collection<Worker> favorites;

    public Client(String name, Email email, String password) {
        super(name,email,password);
        this.role = UserRole.CLIENT;
    }

    public Client(){
        this.role = UserRole.CLIENT;
    }

}
