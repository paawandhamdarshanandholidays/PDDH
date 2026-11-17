package com.pddh.auth_service.entities;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "combined_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uq_combined_roles_role_username", columnNames = { "role", "username" }),
        @UniqueConstraint(name = "uq_combined_roles_role_email", columnNames = { "role", "email" }),
        @UniqueConstraint(name = "uq_combined_roles_role_mobile", columnNames = { "role", "mobile" })
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String role;
    
	
	private String mobile;
	
	private String gender;

	@Column(nullable = false)
	private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // getters and setters
}
