package com.pddh.roleprofile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleProfileConfiguration {
    @Bean
    RoleIdentityService roleProfileIdentityService(@Value("${app.jwt.secret}") String secret) {
        return new RoleIdentityService(secret);
    }
}
