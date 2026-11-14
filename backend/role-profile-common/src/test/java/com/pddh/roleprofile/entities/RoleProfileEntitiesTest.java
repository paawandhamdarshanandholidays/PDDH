package com.pddh.roleprofile.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import jakarta.persistence.Table;

class RoleProfileEntitiesTest {
    @Test
    void mapsEveryRoleToItsDedicatedTable() {
        assertThat(UserRoleProfile.class.getAnnotation(Table.class).name()).isEqualTo("users");
        assertThat(AdminRoleProfile.class.getAnnotation(Table.class).name()).isEqualTo("admin");
        assertThat(ManagerRoleProfile.class.getAnnotation(Table.class).name()).isEqualTo("manager");
        assertThat(CeoRoleProfile.class.getAnnotation(Table.class).name()).isEqualTo("ceo");
        assertThat(StaffRoleProfile.class.getAnnotation(Table.class).name()).isEqualTo("staff");
    }

    @Test
    void storesMappedProfileFieldsAndGeneratedRoleIds() {
        Instant now = Instant.now();
        RoleProfileFields[] profiles = {
                new UserRoleProfile(), new AdminRoleProfile(), new ManagerRoleProfile(),
                new CeoRoleProfile(), new StaffRoleProfile() };
        for (RoleProfileFields profile : profiles) {
            profile.setIdentityId(4L);
            profile.setRole("STAFF");
            profile.setName("Name");
            profile.setUsername("name4");
            profile.setEmail("name@test.com");
            profile.setPasswordHash("hash");
            profile.setMobile("555");
            profile.setGender("Others");
            profile.setActive(false);
            profile.setLastLoginAt(now);
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            assertThat(profile.getIdentityId()).isEqualTo(4L);
            assertThat(profile.getRole()).isEqualTo("STAFF");
            assertThat(profile.getName()).isEqualTo("Name");
            assertThat(profile.getUsername()).isEqualTo("name4");
            assertThat(profile.getEmail()).isEqualTo("name@test.com");
            assertThat(profile.getPasswordHash()).isEqualTo("hash");
            assertThat(profile.getPassword()).isEqualTo("hash");
            assertThat(profile.getMobile()).isEqualTo("555");
            assertThat(profile.getGender()).isEqualTo("Others");
            assertThat(profile.isActive()).isFalse();
            assertThat(profile.getLastLoginAt()).isEqualTo(now);
            assertThat(profile.getCreatedAt()).isEqualTo(now);
            assertThat(profile.getUpdatedAt()).isEqualTo(now);
        }
        assertThat(((UserRoleProfile) profiles[0]).getId()).isNull();
        assertThat(((AdminRoleProfile) profiles[1]).getId()).isNull();
        assertThat(((ManagerRoleProfile) profiles[2]).getId()).isNull();
        assertThat(((CeoRoleProfile) profiles[3]).getId()).isNull();
        assertThat(((StaffRoleProfile) profiles[4]).getId()).isNull();
        ((UserRoleProfile) profiles[0]).setId(1L);
        ((AdminRoleProfile) profiles[1]).setId(2L);
        ((ManagerRoleProfile) profiles[2]).setId(3L);
        ((CeoRoleProfile) profiles[3]).setId(4L);
        ((StaffRoleProfile) profiles[4]).setId(5L);
        assertThat(((UserRoleProfile) profiles[0]).getId()).isEqualTo(1L);
        assertThat(((AdminRoleProfile) profiles[1]).getId()).isEqualTo(2L);
        assertThat(((ManagerRoleProfile) profiles[2]).getId()).isEqualTo(3L);
        assertThat(((CeoRoleProfile) profiles[3]).getId()).isEqualTo(4L);
        assertThat(((StaffRoleProfile) profiles[4]).getId()).isEqualTo(5L);
    }
}
