package com.serv.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan("com.serv.controller")
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.serv.database")
@EntityScan(basePackages = "com.serv.database")
public class ServApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServApplication.class, args);
	}

}
