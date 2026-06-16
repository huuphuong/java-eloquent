package io.github.phuonghuu.eloquent.meta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelationRegistryTest {

    @Test
    void registersEntityMetadataAndRelations() {
        RelationRegistry registry = new RelationRegistry();

        registry.forClass(User.class)
            .table("users")
            .primaryKey("id")
            .hasOne("profile", Profile.class, User::getProfileId, Profile::getId, User::setProfile)
            .belongsToMany("roles", Role.class, "user_roles", "user_id", "role_id", User::setRoles)
            .register();

        RelationRegistry.EntityMeta<User> meta = registry.require(User.class);
        assertEquals("users", meta.table());
        assertEquals("id", meta.primaryKeyProperty());
        assertEquals(RelationKind.HAS_ONE, meta.relation("profile").kind());
        assertEquals(Profile.class, meta.relation("profile").relatedType());
        assertEquals(RelationKind.BELONGS_TO_MANY, meta.relation("roles").kind());
        assertNotNull(meta.relation("roles"));
    }

    public static final class User {
        private Long id;
        private Long profileId;
        private Profile profile;
        private List<Role> roles;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

