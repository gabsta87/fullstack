package com.serv.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @NonNull
    public Optional<Worker> findById(@NonNull Long id);

    @NonNull
    @Override
    List<Worker> findAll();
}
