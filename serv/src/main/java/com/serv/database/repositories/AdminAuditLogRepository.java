package com.serv.database.repositories;

import com.serv.database.entities.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminAuditLogRepository  extends JpaRepository<AdminAuditLog, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AdminAuditLog l SET l.userTarget = null WHERE l.userTarget.id = :userId")
    void nullifyUserTarget(@Param("userId") UUID userId);
}
