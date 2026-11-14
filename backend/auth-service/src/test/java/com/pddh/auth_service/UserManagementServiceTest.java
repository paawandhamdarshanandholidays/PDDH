package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.repository.PasswordResetOtpRepository;
import com.pddh.auth_service.repository.PasswordResetRequestRepository;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;
import com.pddh.auth_service.service.UserManagementService;
import com.pddh.auth_service.service.ProfileSyncService;
import com.pddh.auth_service.service.RegistrationIdentityService;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserManagementServiceTest {
    private UserRepository userRepository;
    private PasswordResetRequestRepository resetRequestRepository;
    private PasswordResetOtpRepository resetOtpRepository;
    private PasswordEncoder passwordEncoder;
    private JdbcTemplate jdbcTemplate;
    private ProfileSyncService profileSyncService;
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        resetRequestRepository = mock(PasswordResetRequestRepository.class);
        resetOtpRepository = mock(PasswordResetOtpRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        profileSyncService = mock(ProfileSyncService.class);
        service = new UserManagementService(
                userRepository,
                resetRequestRepository,
                resetOtpRepository,
                passwordEncoder,
                jdbcTemplate,
                profileSyncService,
                new RegistrationIdentityService(userRepository));
    }

    @Test
    void deleteUserRemovesResetDataAndManagedUser() {
        User staff = user(17L, "STAFF");
        when(userRepository.findById(17L)).thenReturn(Optional.of(staff));

        service.deleteUser(UserRole.CEO, 17L);

        verify(resetRequestRepository).deleteAllReferencingUser(17L);
        verify(resetOtpRepository).deleteAllByEmailIgnoreCase("staff@example.com");
        verify(userRepository).delete(staff);
    }

    @Test
    void deleteUserRejectsPeerRole() {
        User anotherAdmin = user(18L, "ADMIN");
        when(userRepository.findById(18L)).thenReturn(Optional.of(anotherAdmin));

        assertThatThrownBy(() -> service.deleteUser(UserRole.ADMIN, 18L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You cannot manage this user");

        verifyNoInteractions(resetRequestRepository, resetOtpRepository);
    }

    @Test
    void createsAndSynchronizesEveryRoleFromAuthorizedManagement() {
        AtomicLong nextId = new AtomicLong();
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(nextId.incrementAndGet());
                    return user;
                });
        ArgumentCaptor<User> profiles = ArgumentCaptor.forClass(User.class);

        for (UserRole role : UserRole.values()) {
            RegisterRequest request = new RegisterRequest(
                    role.name(), role.name().toLowerCase(), "password123", role.name(),
                    role.name().toLowerCase() + "@example.com", null, "Others");

            var result = service.createUser(UserRole.CEO, request);

            assertThat(result.role()).isEqualTo(role.name());
        }

        verify(userRepository, org.mockito.Mockito.times(UserRole.values().length))
                .saveAndFlush(any(User.class));
        verify(profileSyncService, org.mockito.Mockito.times(UserRole.values().length)).upsert(profiles.capture());
        assertThat(profiles.getAllValues()).extracting(User::getRole)
                .containsExactly("CEO", "ADMIN", "MANAGER", "STAFF", "CUSTOMER");
    }

    @Test
    void rejectsCreatingCeoWhenOneAlreadyExists() {
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class))).thenReturn(null);
        when(userRepository.existsByRoleIgnoreCase("CEO")).thenReturn(true);
        RegisterRequest request = new RegisterRequest(
                "Another CEO", "another", "password123", "CEO", "another@example.com", null, "Others");

        assertThatThrownBy(() -> service.createUser(UserRole.CEO, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("A CEO profile already exists")
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);

        verify(userRepository, org.mockito.Mockito.never()).saveAndFlush(any(User.class));
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setName("Test " + role);
        user.setUsername("test" + role.toLowerCase());
        user.setEmail("staff@example.com");
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
