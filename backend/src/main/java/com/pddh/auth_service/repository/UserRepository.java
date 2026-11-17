package com.pddh.auth_service.repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.pddh.auth_service.entities.User;
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
	Optional<User> findByUsernameIgnoreCaseAndRoleIgnoreCase(String username, String role);
	Optional<User> findByRoleIgnoreCaseAndEmailIgnoreCase(String role, String email);
	boolean existsByRoleIgnoreCaseAndEmailIgnoreCase(String role, String email);
	boolean existsByRoleIgnoreCaseAndUsernameIgnoreCase(String role, String username);
	boolean existsByRoleIgnoreCaseAndMobileIgnoreCase(String role, String mobile);
	boolean existsByRoleIgnoreCaseAndIdNot(String role, Long id);
	boolean existsByRoleIgnoreCaseAndEmailIgnoreCaseAndIdNot(String role, String email, Long id);
	boolean existsByRoleIgnoreCaseAndUsernameIgnoreCaseAndIdNot(String role, String username, Long id);
	boolean existsByRoleIgnoreCaseAndMobileIgnoreCaseAndIdNot(String role, String mobile, Long id);
	boolean existsByRoleIgnoreCase(String role);
	Page<User> findAllByRoleIgnoreCase(String role, Pageable pageable);
	List<PasswordResetTarget> findAllProjectedBy();
	@Query("select distinct u.role from User u where u.active = true and u.role is not null")
	List<String> findDistinctActiveRoles();

	@Modifying
	@Transactional
	@Query("update User u set u.lastLoginAt = :lastLoginAt "
			+ "where lower(u.username) = lower(:username) and lower(u.role) = lower(:role)")
	int updateLastLoginAt(
			@Param("username") String username,
			@Param("role") String role,
			@Param("lastLoginAt") Instant lastLoginAt);

	interface PasswordResetTarget {
		Long getId();
		String getName();
		String getUsername();
		String getEmail();
		String getMobile();
		String getRole();
		boolean isActive();
	}
}
