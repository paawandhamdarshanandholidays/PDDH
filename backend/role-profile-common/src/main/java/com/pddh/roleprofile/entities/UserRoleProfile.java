package com.pddh.roleprofile.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_profile_active_id", columnList = "active,id"),
        @Index(name = "idx_user_profile_email", columnList = "email"),
        @Index(name = "idx_user_profile_username", columnList = "username") })
public class UserRoleProfile extends RoleProfileFields {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
