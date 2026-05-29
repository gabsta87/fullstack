package com.serv.database.repositories.filters;

import com.serv.database.entities.Worker;
import org.springframework.data.jpa.domain.Specification;

public class WorkerSpecifications {
    public static Specification<Worker> isAvailable() {
        return (root, query, cb) -> cb.isTrue(root.get("available"));
    }

    public static Specification<Worker> isNotAvailable() {
        return (root, query, cb) -> cb.isFalse(root.get("available"));
    }

    public static Specification<Worker> isNotDisabled() {
        return (root, query, cb) -> cb.isFalse(root.get("disabled"));
    }

    public static Specification<Worker> hasRegion(String region) {
        return (region == null) ? null : (root, query, cb) -> cb.equal(root.get("region"), region);
    }
}
