package com.pddh.auth_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.ProfileSyncBackfillMarkerRepository;
import com.pddh.auth_service.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ProfileSyncBackfillTest {
    @Test
    void skipsCompletedBackfillAfterTakingTransactionAdvisoryLock() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserRepository users = mock(UserRepository.class);
        ProfileSyncService profileSync = mock(ProfileSyncService.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")).thenReturn(statement);
        when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection));
        ProfileSyncBackfillMarkerRepository markers = mock(ProfileSyncBackfillMarkerRepository.class);
        when(markers.existsById((short) 2)).thenReturn(true);
        ProfileSyncBackfill backfill = new ProfileSyncBackfill(jdbc, users, markers, profileSync);

        backfill.backfillExistingProfiles();

        verify(statement).setLong(1, 764321906L);
        verify(statement).execute();
        verify(users, never()).findAll(any(Pageable.class));
        verify(profileSync, never()).upsert(any(User.class));
    }

    @Test
    void pagesExistingAccountsAndQueuesEachProfileOnce() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserRepository users = mock(UserRepository.class);
        ProfileSyncService profileSync = mock(ProfileSyncService.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")).thenReturn(statement);
        when(jdbc.execute(any(ConnectionCallback.class))).thenAnswer(invocation ->
                ((ConnectionCallback<?>) invocation.getArgument(0)).doInConnection(connection));
        ProfileSyncBackfillMarkerRepository markers = mock(ProfileSyncBackfillMarkerRepository.class);
        when(markers.existsById((short) 2)).thenReturn(false);
        User user = new User();
        user.setId(1L);
        when(users.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        new ProfileSyncBackfill(jdbc, users, markers, profileSync).backfillExistingProfiles();

        verify(profileSync).upsert(user);
        verify(markers).saveAndFlush(any());
    }
}
