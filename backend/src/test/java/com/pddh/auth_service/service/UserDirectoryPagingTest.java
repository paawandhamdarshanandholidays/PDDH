package com.pddh.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.repository.UserDirectoryRepository;
import com.pddh.auth_service.security.UserRole;

class UserDirectoryPagingTest {
    @Test
    void managerCanLoadCombinedAccountsAndManualRowsInSortedPagesOfAtMostFifty() {
        JdbcTemplate jdbc = jdbcTemplate("directory_paging");
        jdbc.update("INSERT INTO combined_roles (id, name, username, email, role, mobile) "
                + "VALUES (1, 'Account User', 'account', 'account@example.test', 'CUSTOMER', '123')");
        jdbc.update("INSERT INTO user_directory (id, customer_id, name, contact_number, email, address, referred_by, service_provided) "
                + "VALUES (2, NULL, 'Manual User', '456', 'manual@example.test', 'Address', 'Friend', 'Tour')");
        UserDirectoryService service = new UserDirectoryService(mock(UserDirectoryRepository.class), jdbc);

        var response = service.listPage(UserRole.MANAGER, 0, 50, "email", "DESC");

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).extracting(item -> item.source()).containsExactly("MANUAL", "ACCOUNT");
        assertThat(response.content()).extracting(item -> item.id()).containsExactly("MANUAL-2", "ACCOUNT-1");
    }

    @Test
    void rejectsDirectoryPagesAboveFiftyAndCustomerAccess() {
        UserDirectoryService service = new UserDirectoryService(
                mock(UserDirectoryRepository.class), mock(JdbcTemplate.class));
        assertThatThrownBy(() -> service.listPage(UserRole.MANAGER, 0, 51, "name", "ASC"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.listPage(UserRole.CUSTOMER, 0, 50, "name", "ASC"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void refusesToMutateRegisteredCustomerRowsThroughDirectoryEndpoints() {
        UserDirectoryService service = new UserDirectoryService(
                mock(UserDirectoryRepository.class), mock(JdbcTemplate.class));
        var request = new com.pddh.auth_service.dto.UserDirectoryRequest(
                "Name", "123", "name@example.test", "Address", "Friend", "Tour");

        assertThatThrownBy(() -> service.update(UserRole.ADMIN, "ACCOUNT-1", request))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.delete(UserRole.ADMIN, "ACCOUNT-1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private JdbcTemplate jdbcTemplate(String name) {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE combined_roles (
                    id BIGINT PRIMARY KEY, name VARCHAR(120), username VARCHAR(80),
                    email VARCHAR(254), role VARCHAR(30), mobile VARCHAR(30)
                )
                """);
        jdbc.execute("""
                CREATE TABLE user_directory (
                    id BIGINT PRIMARY KEY, customer_id BIGINT, name VARCHAR(120),
                    contact_number VARCHAR(30), email VARCHAR(254), address VARCHAR(300),
                    referred_by VARCHAR(120), service_provided VARCHAR(160)
                )
                """);
        return jdbc;
    }
}
