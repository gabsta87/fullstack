package com.serv.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan({"com.serv.controller","com.serv.service","com.serv.configuration"})
@EnableJpaRepositories(basePackages = "com.serv.database.repositories")
@EntityScan(basePackages = "com.serv.database")
public class ServApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServApplication.class, args);
	}

}
