package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.meta.RelationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryBuilderSqlTest {

    @Test
    void buildsRootSelectSqlWithoutDatabaseAccess() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        QueryBuilder<User> query = new QueryBuilder<>(jdbcTemplate, registry, User.class);

        String sql = query.select("id", "name")
            .where("name", "Alice")
            .orderByDesc("id")
            .page(2)
            .size(10)
            .toSql();

        assertEquals("SELECT id, name FROM users WHERE name = :p_0 ORDER BY id DESC LIMIT 10 OFFSET 10", sql);
    }

    @Test
    void buildsDebugSqlWithValuesExpanded() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        QueryBuilder<User> query = new QueryBuilder<>(jdbcTemplate, registry, User.class);

        String debugSql = query.select("id", "name")
            .where("name", "Alice")
            .orderByDesc("id")
            .page(2)
            .size(10)
            .toDebugSql();

        assertEquals("SELECT id, name FROM users WHERE name = 'Alice' ORDER BY id DESC LIMIT 10 OFFSET 10", debugSql);
    }

    @Test
    void buildsRelationFilterSql() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        QueryBuilder<User> query = new QueryBuilder<>(jdbcTemplate, registry, User.class);

        String sql = query.whereHas("roles", child -> child.where("name", "ADMIN")).toSql();

        assertTrue(sql.contains("EXISTS (SELECT 1 FROM user_roles pivot JOIN roles target"));
        assertTrue(sql.contains("pivot.user_id = users.id"));
        assertTrue(sql.contains("target.name = :p_0"));
    }

    @Test
    void previewsNestedWithTreeSqls() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        QueryBuilder<User> query = new QueryBuilder<>(jdbcTemplate, registry, User.class);

        List<String> sqls = query.with("profile").toSqls();

        assertEquals(2, sqls.size());
        assertTrue(sqls.get(0).startsWith("SELECT "));
        assertTrue(sqls.get(1).contains("FROM profiles"));
    }

    private RelationRegistry registry() {
        RelationRegistry registry = new RelationRegistry();
        registry.forClass(User.class)
            .table("users")
            .primaryKey("id")
            .hasOne("profile", Profile.class, User::getProfileId, Profile::getId, User::setProfile)
            .belongsToMany("roles", Role.class, "user_roles", "user_id", "role_id", User::setRoles)
            .register();
        registry.forClass(Profile.class)
            .table("profiles")
            .primaryKey("id")
            .register();
        registry.forClass(Role.class)
            .table("roles")
            .primaryKey("id")
            .register();
        return registry;
    }

    public static final class User {
        private Long id;
        private String name;
        private Long profileId;
        private Profile profile;
        private List<Role> roles;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getProfileId() {
            return profileId;
        }

        public void setProfileId(Long profileId) {
            this.profileId = profileId;
        }

        public Profile getProfile() {
            return profile;
        }

        public void setProfile(Profile profile) {
            this.profile = profile;
        }

        public List<Role> getRoles() {
            return roles;
        }

        public void setRoles(List<Role> roles) {
            this.roles = roles;
        }
    }

    public static final class Profile {
        private Long id;
        private String bio;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }
    }

    public static final class Role {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

