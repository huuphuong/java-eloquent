package io.github.huuphuong.eloquent.write;

import io.github.huuphuong.eloquent.meta.RelationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelationWriterBehaviorTest {

    @Test
    void createInsertsEntityValuesAndSkipsNullPrimaryKey() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(null);
        entity.setUsername("alice");
        entity.setStatus(Status.ACTIVE);
        entity.setActive(Boolean.TRUE);
        entity.setCreatedAt(LocalDateTime.of(2026, 6, 18, 17, 44, 46));

        UserEntity returned = writer.create(entity);

        assertSame(entity, returned);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertTrue(sqlCaptor.getValue().startsWith("INSERT INTO users"));
        assertFalse(paramsCaptor.getValue().hasValue("id"));
        assertEquals("alice", paramsCaptor.getValue().getValue("username"));
        assertEquals("ACTIVE", paramsCaptor.getValue().getValue("status"));
        assertEquals(Boolean.TRUE, paramsCaptor.getValue().getValue("active"));
        assertTrue(paramsCaptor.getValue().getValue("created_at") instanceof Timestamp);
    }

    @Test
    void insertBatchInsertsAllEntitiesInOneBatchGroup() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class))).thenReturn(new int[]{1, 1});

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity first = new UserEntity();
        first.setUsername("alice");
        UserEntity second = new UserEntity();
        second.setUsername("bob");

        boolean result = writer.insertBatch(Arrays.asList(first, second));

        assertTrue(result);
        ArgumentCaptor<MapSqlParameterSource[]> batchCaptor = ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertEquals(2, batchCaptor.getValue().length);
        assertEquals("alice", batchCaptor.getValue()[0].getValue("username"));
        assertEquals("bob", batchCaptor.getValue()[1].getValue("username"));
    }

    @Test
    void insertBatchIgnoreErrorReturnsFalseWhenBatchFails() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class)))
            .thenThrow(new RuntimeException("duplicate key"));

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setUsername("alice");

        boolean result = writer.insertBatchIgnoreError(Collections.singletonList(entity));

        assertFalse(result);
        verify(jdbcTemplate).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
    }

    @Test
    void updateWritesAllSimpleFieldsExceptPrimaryKey() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(10L);
        entity.setUsername("alice.updated");
        entity.setStatus(Status.INACTIVE);
        entity.setActive(Boolean.FALSE);

        int updated = writer.update(entity);

        assertEquals(1, updated);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertTrue(sqlCaptor.getValue().startsWith("UPDATE users SET "));
        assertTrue(sqlCaptor.getValue().contains("WHERE id = :__pk"));
        assertFalse(paramsCaptor.getValue().hasValue("id"));
        assertEquals(10L, paramsCaptor.getValue().getValue("__pk"));
        assertEquals("alice.updated", paramsCaptor.getValue().getValue("username"));
        assertEquals("INACTIVE", paramsCaptor.getValue().getValue("status"));
        assertEquals(Boolean.FALSE, paramsCaptor.getValue().getValue("active"));
    }

    @Test
    void updateThrowsWhenPrimaryKeyIsMissing() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);

        UserEntity entity = new UserEntity();
        entity.setUsername("alice");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> writer.update(entity));
        assertEquals("Primary key value is required for update", exception.getMessage());
        verify(jdbcTemplate, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void updatePartialOnlyWritesRequestedFields() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(20L);
        entity.setUsername("patched");
        entity.setStatus(null);

        int updated = writer.updatePartial(entity, "username", "status");

        assertEquals(1, updated);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), paramsCaptor.capture());
        assertEquals("patched", paramsCaptor.getValue().getValue("username"));
        assertTrue(paramsCaptor.getValue().hasValue("status"));
        assertEquals(null, paramsCaptor.getValue().getValue("status"));
        assertEquals(20L, paramsCaptor.getValue().getValue("__pk"));
        assertFalse(paramsCaptor.getValue().hasValue("active"));
    }

    @Test
    void updatePartialByMapUpdatesByEntityPrimaryKey() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(21L);

        LinkedHashMap<String, Object> patch = new LinkedHashMap<>();
        patch.put("username", "map.patch");
        patch.put("status", null);

        int updated = writer.updatePartialByMap(entity, patch);

        assertEquals(1, updated);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), paramsCaptor.capture());
        assertEquals("map.patch", paramsCaptor.getValue().getValue("username"));
        assertEquals(null, paramsCaptor.getValue().getValue("status"));
        assertEquals(21L, paramsCaptor.getValue().getValue("__pk"));
    }

    @Test
    void updatePartialByIdUpdatesWithoutLoadingEntityFirst() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);

        LinkedHashMap<String, Object> patch = new LinkedHashMap<>();
        patch.put("username", "direct.patch");

        int updated = writer.updatePartialById(22L, patch);

        assertEquals(1, updated);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), paramsCaptor.capture());
        assertEquals("direct.patch", paramsCaptor.getValue().getValue("username"));
        assertEquals(22L, paramsCaptor.getValue().getValue("__pk"));
    }

    @Test
    void upsertCreatesWhenPrimaryKeyIsMissing() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setUsername("new-user");

        UserEntity returned = writer.upsert(entity);

        assertSame(entity, returned);
        verify(jdbcTemplate, times(1)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void upsertUpdatesExistingRowWhenUpdateSucceeds() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(30L);
        entity.setUsername("existing");

        UserEntity returned = writer.upsert(entity);

        assertSame(entity, returned);
        verify(jdbcTemplate, times(1)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void upsertFallsBackToCreateWhenUpdateAffectsZeroRows() {
        RelationRegistry registry = userRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0, 1);

        RelationWriter<UserEntity> writer = new RelationWriter<>(jdbcTemplate, registry, UserEntity.class);
        UserEntity entity = new UserEntity();
        entity.setId(31L);
        entity.setUsername("missing");

        UserEntity returned = writer.upsert(entity);

        assertSame(entity, returned);
        verify(jdbcTemplate, times(2)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void attachCollectionInsertsOnlyMissingPivotRows() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Collections.singletonList(10L));
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class)))
            .thenReturn(new int[]{1, 1});

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);
        int affected = writer.attach("roles", 1L, Arrays.asList(10L, 11L, 12L));

        assertEquals(2, affected);
        ArgumentCaptor<MapSqlParameterSource[]> batchCaptor = ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());
        assertEquals(2, batchCaptor.getValue().length);
        assertEquals(11L, batchCaptor.getValue()[0].getValue("related_id"));
        assertEquals(12L, batchCaptor.getValue()[1].getValue("related_id"));
    }

    @Test
    void attachMapInsertsMissingPivotRowsWithExtraColumns() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Collections.singletonList(10L));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);

        LinkedHashMap<Long, Map<String, ?>> rows = new LinkedHashMap<>();
        rows.put(10L, pivotAttrs("MANAGER", 1));
        rows.put(11L, pivotAttrs("STAFF", 2));

        int affected = writer.attach("roles", 1L, rows);

        assertEquals(1, affected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), paramsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("INSERT INTO user_roles"));
        assertEquals(1L, paramsCaptor.getValue().getValue("user_id"));
        assertEquals(11L, paramsCaptor.getValue().getValue("role_id"));
        assertEquals("STAFF", paramsCaptor.getValue().getValue("role_code"));
        assertEquals(2, paramsCaptor.getValue().getValue("level_no"));
    }

    @Test
    void detachDeletesAllRowsWhenIdsAreNull() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(3);

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);
        int affected = writer.detach("roles", 1L, null);

        assertEquals(3, affected);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(MapSqlParameterSource.class));
        assertTrue(sqlCaptor.getValue().contains("DELETE FROM user_roles"));
        assertTrue(sqlCaptor.getValue().contains("user_id = :parent_id"));
    }

    @Test
    void detachDeletesSelectedRows() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(2);

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);
        int affected = writer.detach("roles", 1L, Arrays.asList(10L, 11L));

        assertEquals(2, affected);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(anyString(), paramsCaptor.capture());
        assertEquals(1L, paramsCaptor.getValue().getValue("parent_id"));
        assertTrue(paramsCaptor.getValue().hasValue("related_ids"));
    }

    @Test
    void syncCollectionDeletesMissingAndInsertsNewRows() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Arrays.asList(10L, 11L));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class))).thenReturn(new int[]{1});

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);
        int affected = writer.sync("roles", 1L, Arrays.asList(11L, 12L));

        assertEquals(1, affected);
        verify(jdbcTemplate).update(anyString(), any(MapSqlParameterSource.class));
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
    }

    @Test
    void syncWithoutDetachingOnlyAddsMissingRows() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Arrays.asList(10L, 11L));
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class))).thenReturn(new int[]{1});

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);
        int affected = writer.syncWithoutDetaching("roles", 1L, Arrays.asList(11L, 12L));

        assertEquals(1, affected);
        verify(jdbcTemplate, never()).update(anyString(), any(MapSqlParameterSource.class));
        verify(jdbcTemplate).batchUpdate(anyString(), any(MapSqlParameterSource[].class));
    }

    @Test
    void syncMapUpdatesExistingRowsAndInsertsNewRows() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Collections.singletonList(10L));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1, 1);

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);

        LinkedHashMap<Long, Map<String, ?>> rows = new LinkedHashMap<>();
        rows.put(10L, pivotAttrs("MANAGER", 1));
        rows.put(12L, pivotAttrs("STAFF", 2));

        int affected = writer.sync("roles", 1L, rows);

        assertEquals(2, affected);
        verify(jdbcTemplate, times(2)).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void aliasMethodsDelegateToPrimaryPivotOperations() {
        RelationRegistry registry = pivotRegistry();
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(MapSqlParameterSource.class), Mockito.eq(Object.class)))
            .thenReturn(Collections.singletonList(10L));
        when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbcTemplate.batchUpdate(anyString(), any(MapSqlParameterSource[].class))).thenReturn(new int[]{1});

        RelationWriter<PivotUser> writer = new RelationWriter<>(jdbcTemplate, registry, PivotUser.class);

        assertEquals(1, writer.dettach("roles", 1L, Arrays.asList(10L)));
        assertEquals(1, writer.syncWithoutDetachting("roles", 1L, Arrays.asList(11L)));
    }

    private static RelationRegistry userRegistry() {
        RelationRegistry registry = new RelationRegistry();
        registry.forClass(UserEntity.class)
            .table("users")
            .primaryKey("id")
            .register();
        return registry;
    }

    private static RelationRegistry pivotRegistry() {
        RelationRegistry registry = new RelationRegistry();
        registry.forClass(PivotUser.class)
            .table("users")
            .primaryKey("id")
            .belongsToMany("roles", PivotRole.class, "user_roles", "user_id", "role_id", PivotUser::setRoles)
            .register();
        registry.forClass(PivotRole.class)
            .table("roles")
            .primaryKey("id")
            .register();
        return registry;
    }

    private static Map<String, Object> pivotAttrs(String roleCode, int levelNo) {
        LinkedHashMap<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("role_code", roleCode);
        attrs.put("level_no", levelNo);
        return attrs;
    }

    public static final class UserEntity {
        private Long id;
        private String username;
        private Status status;
        private Boolean active;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static final class PivotUser {
        private Long id;
        private List<PivotRole> roles;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<PivotRole> getRoles() {
            return roles;
        }

        public void setRoles(List<PivotRole> roles) {
            this.roles = roles;
        }
    }

    public static final class PivotRole {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
