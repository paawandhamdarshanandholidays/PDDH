package com.pddh.manager_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import com.pddh.roleprofile.RoleProfileConfiguration;

@SpringBootApplication
@Import(RoleProfileConfiguration.class)
@EntityScan("com.pddh.roleprofile.entities")
public class ManagerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManagerServiceApplication.class, args);
    }
}
