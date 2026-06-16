package io.github.phuonghuu.eloquent.write;

import io.github.phuonghuu.eloquent.meta.RelationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationWriterPreviewTest {

    @Test
    void previewsPivotAttachStatementsWithoutDatabaseWrites() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(jdbcTemplate.queryForList(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.eq(Object.class)
        )).thenReturn(Collections.emptyList());

        RelationWriter<User> writer = new RelationWriter<>(jdbcTemplate, registry, User.class);
        List<String> sqls = writer.previewAttachSql("roles", 1L, Arrays.asList(10L, 11L));

        assertEquals(2, sqls.size());
        assertTrue(sqls.get(0).contains("INSERT INTO user_roles"));
        assertTrue(sqls.get(0).contains("1"));
        assertTrue(sqls.get(0).contains("10"));
    }

    @Test
    void previewsDetachAllStatementWhenNoIdsAreProvided() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        RelationWriter<User> writer = new RelationWriter<>(jdbcTemplate, registry, User.class);

        List<String> sqls = writer.previewDetachSql("roles", 1L, null);

        assertEquals(1, sqls.size());
        assertTrue(sqls.get(0).contains("DELETE FROM user_roles"));
        assertTrue(sqls.get(0).contains("user_id = 1"));
    }

    @Test
    void relationKitFactoryExposesTheMainEntryPoint() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

        io.github.phuonghuu.eloquent.RelationKit kit = io.github.phuonghuu.eloquent.RelationKit.of(jdbcTemplate, registry);

        assertTrue(kit.query(User.class) != null);
        assertTrue(kit.write(User.class) != null);
        assertEquals(jdbcTemplate, kit.jdbcTemplate());
        assertEquals(registry, kit.registry());
    }

    private RelationRegistry registry() {
        RelationRegistry registry = new RelationRegistry();
        registry.forClass(User.class)
            .table("users")
            .primaryKey("id")
            .belongsToMany("roles", Role.class, "user_roles", "user_id", "role_id", User::setRoles)
            .register();
        registry.forClass(Role.class)
            .table("roles")
            .primaryKey("id")
            .register();
        return registry;
    }

    public static final class User {
        private Long id;
        private List<Role> roles;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<Role> getRoles() {
            return roles;
        }

        public void setRoles(List<Role> roles) {
            this.roles = roles;
        }
    }

    public static final class Role {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}

