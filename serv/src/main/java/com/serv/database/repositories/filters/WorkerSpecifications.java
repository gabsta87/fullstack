package com.serv.database.repositories.filters;

import com.serv.database.entities.Worker;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDate;
import java.sql.Date;

public class WorkerSpecifications {

    public static Specification<Worker> isValidProfile() {
        return (root, query, cb) -> {
            LocalDate maxBirthDate = LocalDate.now().minusYears(18);
            Date targetDate = java.sql.Date.valueOf(maxBirthDate);

            return cb.and(
                    cb.lessThanOrEqualTo(root.get("birthdate"), targetDate), // Majeur (âge >= 18)
                    cb.equal(root.get("isLocked"), false),                     // !isLocked()
                    cb.equal(root.get("isBanned"), false),                     // !isBanned()
                    cb.equal(root.get("isExpired"), false),                    // !isExpired()
                    cb.equal(root.get("isHidden"), false),                     // !isHidden()
                    cb.equal(root.get("isInvalid"), false)                     // !isInvalid()
            );
        };
    }
}