package com.pddh.roleprofile;

import org.springframework.beans.factory.annotation.Value;

public record RoleProfileProperties(@Value("${app.profile-sync.table}") String table,
        @Value("${app.role.expected}") String role,
        @Value("${app.role.path}") String path,
        @Value("${app.profile-sync.topic}") String topic) { }
