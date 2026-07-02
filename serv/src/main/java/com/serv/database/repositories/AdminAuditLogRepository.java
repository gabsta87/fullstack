package com.serv.database.repositories;

import com.serv.database.entities.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository  extends JpaRepository<AdminAuditLog, Integer> {

}
