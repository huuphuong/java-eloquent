package io.github.phuonghuu.eloquent.write;

import io.github.phuonghuu.eloquent.meta.RelationKind;
import io.github.phuonghuu.eloquent.meta.RelationMeta;
import io.github.phuonghuu.eloquent.meta.RelationRegistry;
import io.github.phuonghuu.eloquent.query.SqlGenerator;
import io.github.phuonghuu.eloquent.query.SqlStatement;
import io.github.phuonghuu.eloquent.support.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public final class RelationWriter<T> {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RelationRegistry registry;
    private final Class<T> type;

    public RelationWriter(NamedParameterJdbcTemplate jdbcTemplate, RelationRegistry registry, Class<T> type) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
        this.registry = Objects.requireNonNull(registry, "registry is required");
        this.type = Objects.requireNonNull(type, "type is required");
    }

    public T create(T entity) {
        Objects.requireNonNull(entity, "entity is required");
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        Map<String, Object> values = extractInsertValues(meta, entity);
        if (values.isEmpty()) {
            throw new IllegalStateException("No writable properties found for " + type.getName());
        }

        String sql = buildInsertSql(meta.table(), values);
        jdbcTemplate.update(sql, new MapSqlParameterSource(values));
        return entity;
    }

    public List<T> createMany(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(create(entity));
        }
        return result;
    }

    public int update(T entity) {
        Objects.requireNonNull(entity, "entity is required");
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        Object primaryKeyValue = propertyValue(entity, meta.primaryKeyProperty());
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("Primary key value is required for update");
        }

        Map<String, Object> values = extractUpdateValues(meta, entity);
        if (values.isEmpty()) {
            return 0;
        }

        String pkColumn = SqlGenerator.toColumnName(meta.primaryKeyProperty());
        StringBuilder sql = new StringBuilder("UPDATE ").append(meta.table()).append(" SET ");
        List<String> assignments = new ArrayList<>();
        for (String column : values.keySet()) {
            assignments.add(column + " = :" + column);
        }
        sql.append(String.join(", ", assignments));
        sql.append(" WHERE ").append(pkColumn).append(" = :__pk");

        MapSqlParameterSource params = new MapSqlParameterSource(values);
        params.addValue("__pk", normalizeWriteValue(primaryKeyValue));
        return jdbcTemplate.update(sql.toString(), params);
    }

    public int updatePartial(T entity, String... properties) {
        Objects.requireNonNull(entity, "entity is required");
        if (properties == null || properties.length == 0) {
            throw new IllegalArgumentException("At least one property is required for partial update");
        }
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        Map<String, Object> patchValues = new LinkedHashMap<>();
        for (String property : properties) {
            Object explicitValue = propertyValue(entity, property);
            collectPartialUpdateValue(meta, property, explicitValue, patchValues);
        }
        Object primaryKeyValue = propertyValue(entity, meta.primaryKeyProperty());
        return executePartialUpdate(meta, primaryKeyValue, patchValues);
    }

    public int updatePartialByMap(T entity, Map<String, ?> patchValues) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(patchValues, "patchValues is required");

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        for (Map.Entry<String, ?> entry : patchValues.entrySet()) {
            collectPartialUpdateValue(meta, entry.getKey(), entry.getValue(), values);
        }
        Object primaryKeyValue = propertyValue(entity, meta.primaryKeyProperty());
        return executePartialUpdate(meta, primaryKeyValue, values);
    }

    public int updatePartialById(Object id, Map<String, ?> patchValues) {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(patchValues, "patchValues is required");

        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : patchValues.entrySet()) {
            collectPartialUpdateValue(meta, entry.getKey(), entry.getValue(), values);
        }
        return executePartialUpdate(meta, id, values);
    }

    public T upsert(T entity) {
        Objects.requireNonNull(entity, "entity is required");
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        Object primaryKeyValue = propertyValue(entity, meta.primaryKeyProperty());
        if (primaryKeyValue == null) {
            return create(entity);
        }

        int updated = update(entity);
        if (updated > 0) {
            return entity;
        }
        return create(entity);
    }

    public int attach(String relationName, Object parentId, Collection<?> relatedIds) {
        return syncInternal(relationName, parentId, relatedIds, false, true);
    }

    public int attach(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        Objects.requireNonNull(parentId, "parentId is required");
        Objects.requireNonNull(relatedRows, "relatedRows is required");

        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashMap<Object, Map<String, ?>> normalizedRows = normalizePivotRows(relatedRows);
        if (normalizedRows.isEmpty()) {
            return 0;
        }

        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);

        LinkedHashMap<Object, Map<String, ?>> toInsert = new LinkedHashMap<>();
        for (Map.Entry<Object, Map<String, ?>> entry : normalizedRows.entrySet()) {
            if (!existingIds.contains(entry.getKey())) {
                toInsert.put(entry.getKey(), entry.getValue());
            }
        }

        return insertPivotRows(relationMeta, normalizedParentId, toInsert);
    }

    public List<String> previewAttachSql(String relationName, Object parentId, Collection<?> relatedIds) {
        Objects.requireNonNull(parentId, "parentId is required");
        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashSet<Object> desiredIds = normalizeIdSet(relatedIds);
        if (desiredIds.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);
        List<String> sqls = new ArrayList<>();
        for (Object relatedId : desiredIds) {
            if (!existingIds.contains(relatedId)) {
                sqls.add(compactSql(buildPivotInsertStatement(relationMeta, normalizedParentId, relatedId, Collections.<String, Object>emptyMap()).toDebugSql()));
            }
        }
        return sqls;
    }

    public List<String> previewAttachSql(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        Objects.requireNonNull(parentId, "parentId is required");
        Objects.requireNonNull(relatedRows, "relatedRows is required");

        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashMap<Object, Map<String, ?>> normalizedRows = normalizePivotRows(relatedRows);
        if (normalizedRows.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);
        List<String> sqls = new ArrayList<>();
        for (Map.Entry<Object, Map<String, ?>> entry : normalizedRows.entrySet()) {
            if (!existingIds.contains(entry.getKey())) {
                sqls.add(compactSql(
                    buildPivotInsertStatement(relationMeta, normalizedParentId, entry.getKey(), entry.getValue()).toDebugSql()
                ));
            }
        }
        return sqls;
    }

    public int detach(String relationName, Object parentId, Collection<?> relatedIds) {
        return detachInternal(relationName, parentId, relatedIds);
    }

    @Deprecated
    public int dettach(String relationName, Object parentId, Collection<?> relatedIds) {
        return detach(relationName, parentId, relatedIds);
    }

    public List<String> previewDetachSql(String relationName, Object parentId, Collection<?> relatedIds) {
        Objects.requireNonNull(parentId, "parentId is required");
        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        if (relatedIds == null || relatedIds.isEmpty()) {
            List<String> sqls = new ArrayList<>();
            sqls.add(compactSql(buildPivotDetachAllStatement(relationMeta, normalizedParentId).toDebugSql()));
            return sqls;
        }

        LinkedHashSet<Object> ids = normalizeIdSet(relatedIds);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sqls = new ArrayList<>();
        sqls.add(compactSql(buildPivotDetachStatement(relationMeta, normalizedParentId, ids).toDebugSql()));
        return sqls;
    }

    public int sync(String relationName, Object parentId, Collection<?> relatedIds) {
        return syncInternal(relationName, parentId, relatedIds, true, false);
    }

    public int sync(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        return syncPivotRows(relationName, parentId, relatedRows, true);
    }

    public List<String> previewSyncSql(String relationName, Object parentId, Collection<?> relatedIds) {
        return previewSyncSqlInternal(relationName, parentId, relatedIds, true, false);
    }

    public List<String> previewSyncSql(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        return previewSyncPivotRows(relationName, parentId, relatedRows, true);
    }

    public int syncWithoutDetaching(String relationName, Object parentId, Collection<?> relatedIds) {
        return syncInternal(relationName, parentId, relatedIds, false, true);
    }

    public int syncWithoutDetaching(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        return syncPivotRows(relationName, parentId, relatedRows, false);
    }

    public List<String> previewSyncWithoutDetachingSql(String relationName, Object parentId, Collection<?> relatedIds) {
        return previewSyncSqlInternal(relationName, parentId, relatedIds, false, true);
    }

    public List<String> previewSyncWithoutDetachingSql(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows) {
        return previewSyncPivotRows(relationName, parentId, relatedRows, false);
    }

    @Deprecated
    public int syncWithoutDetachting(String relationName, Object parentId, Collection<?> relatedIds) {
        return syncWithoutDetaching(relationName, parentId, relatedIds);
    }

    private int syncInternal(String relationName, Object parentId, Collection<?> relatedIds, boolean detachMissing, boolean attachMissing) {
        Objects.requireNonNull(parentId, "parentId is required");
        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashSet<Object> desiredIds = normalizeIdSet(relatedIds);
        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);

        int affected = 0;
        if (detachMissing) {
            LinkedHashSet<Object> toDelete = new LinkedHashSet<>(existingIds);
            toDelete.removeAll(desiredIds);
            affected += deletePivotRows(relationMeta, normalizedParentId, toDelete);
        }
        if (attachMissing) {
            LinkedHashSet<Object> toInsert = new LinkedHashSet<>(desiredIds);
            toInsert.removeAll(existingIds);
            affected += insertPivotRows(relationMeta, normalizedParentId, toInsert);
        }
        return affected;
    }

    private int syncPivotRows(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows, boolean detachMissing) {
        Objects.requireNonNull(parentId, "parentId is required");
        Objects.requireNonNull(relatedRows, "relatedRows is required");

        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashMap<Object, Map<String, ?>> desiredRows = normalizePivotRows(relatedRows);
        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);
        LinkedHashSet<Object> desiredIds = new LinkedHashSet<>(desiredRows.keySet());

        int affected = 0;
        if (detachMissing) {
            LinkedHashSet<Object> toDelete = new LinkedHashSet<>(existingIds);
            toDelete.removeAll(desiredIds);
            affected += deletePivotRows(relationMeta, normalizedParentId, toDelete);
        }

        LinkedHashMap<Object, Map<String, ?>> toInsert = new LinkedHashMap<>();
        for (Map.Entry<Object, Map<String, ?>> entry : desiredRows.entrySet()) {
            if (existingIds.contains(entry.getKey())) {
                affected += updatePivotRow(relationMeta, normalizedParentId, entry.getKey(), entry.getValue());
            } else {
                toInsert.put(entry.getKey(), entry.getValue());
            }
        }
        affected += insertPivotRows(relationMeta, normalizedParentId, toInsert);
        return affected;
    }

    private int detachInternal(String relationName, Object parentId, Collection<?> relatedIds) {
        Objects.requireNonNull(parentId, "parentId is required");
        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        if (relatedIds == null || relatedIds.isEmpty()) {
            String sql = "DELETE FROM " + relationMeta.pivotTable() + " WHERE "
                + relationMeta.pivotParentKey() + " = :parent_id";
            return jdbcTemplate.update(sql, new MapSqlParameterSource().addValue("parent_id", normalizedParentId));
        }

        LinkedHashSet<Object> ids = normalizeIdSet(relatedIds);
        if (ids.isEmpty()) {
            return 0;
        }

        String sql = "DELETE FROM " + relationMeta.pivotTable() + " WHERE "
            + relationMeta.pivotParentKey() + " = :parent_id AND "
            + relationMeta.pivotRelatedKey() + " IN (:related_ids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("parent_id", normalizedParentId)
            .addValue("related_ids", ids);
        return jdbcTemplate.update(sql, params);
    }

    private List<String> previewSyncSqlInternal(String relationName, Object parentId, Collection<?> relatedIds, boolean detachMissing, boolean attachMissing) {
        Objects.requireNonNull(parentId, "parentId is required");
        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashSet<Object> desiredIds = normalizeIdSet(relatedIds);
        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);

        List<String> sqls = new ArrayList<>();
        if (detachMissing) {
            LinkedHashSet<Object> toDelete = new LinkedHashSet<>(existingIds);
            toDelete.removeAll(desiredIds);
            if (!toDelete.isEmpty()) {
                sqls.add(compactSql(buildPivotDetachStatement(relationMeta, normalizedParentId, toDelete).toDebugSql()));
            }
        }
        if (attachMissing) {
            LinkedHashSet<Object> toInsert = new LinkedHashSet<>(desiredIds);
            toInsert.removeAll(existingIds);
            for (Object relatedId : toInsert) {
                sqls.add(compactSql(buildPivotInsertStatement(relationMeta, normalizedParentId, relatedId, Collections.<String, Object>emptyMap()).toDebugSql()));
            }
        }
        return sqls;
    }

    private List<String> previewSyncPivotRows(String relationName, Object parentId, Map<?, ? extends Map<String, ?>> relatedRows, boolean detachMissing) {
        Objects.requireNonNull(parentId, "parentId is required");
        Objects.requireNonNull(relatedRows, "relatedRows is required");

        RelationMeta relationMeta = requireBelongsToManyRelation(relationName);
        Object normalizedParentId = normalizeWriteValue(parentId);
        LinkedHashMap<Object, Map<String, ?>> desiredRows = normalizePivotRows(relatedRows);
        LinkedHashSet<Object> existingIds = loadRelatedIds(relationMeta, normalizedParentId);
        LinkedHashSet<Object> desiredIds = new LinkedHashSet<>(desiredRows.keySet());

        List<String> sqls = new ArrayList<>();
        if (detachMissing) {
            LinkedHashSet<Object> toDelete = new LinkedHashSet<>(existingIds);
            toDelete.removeAll(desiredIds);
            if (!toDelete.isEmpty()) {
                sqls.add(compactSql(buildPivotDetachStatement(relationMeta, normalizedParentId, toDelete).toDebugSql()));
            }
        }

        for (Map.Entry<Object, Map<String, ?>> entry : desiredRows.entrySet()) {
            if (existingIds.contains(entry.getKey())) {
                sqls.add(compactSql(buildPivotUpdateStatement(relationMeta, normalizedParentId, entry.getKey(), entry.getValue()).toDebugSql()));
            } else {
                sqls.add(compactSql(buildPivotInsertStatement(relationMeta, normalizedParentId, entry.getKey(), entry.getValue()).toDebugSql()));
            }
        }
        return sqls;
    }

    private RelationMeta requireBelongsToManyRelation(String relationName) {
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        RelationMeta relationMeta = meta.relation(relationName);
        if (relationMeta.kind() != RelationKind.BELONGS_TO_MANY) {
            throw new IllegalStateException("Mutation methods are currently supported only for belongsToMany relations");
        }
        return relationMeta;
    }

    private LinkedHashSet<Object> loadRelatedIds(RelationMeta relationMeta, Object parentId) {
        String sql = "SELECT " + relationMeta.pivotRelatedKey() + " FROM " + relationMeta.pivotTable()
            + " WHERE " + relationMeta.pivotParentKey() + " = :parent_id";
        List<Object> values = jdbcTemplate.queryForList(sql, new MapSqlParameterSource().addValue("parent_id", parentId), Object.class);
        return normalizeIdSet(values);
    }

    private int insertPivotRows(RelationMeta relationMeta, Object parentId, Collection<Object> relatedIds) {
        if (relatedIds == null || relatedIds.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO " + relationMeta.pivotTable() + " ("
            + relationMeta.pivotParentKey() + ", " + relationMeta.pivotRelatedKey()
            + ") VALUES (:parent_id, :related_id)";
        List<MapSqlParameterSource> sources = new ArrayList<>();
        for (Object relatedId : relatedIds) {
            sources.add(new MapSqlParameterSource()
                .addValue("parent_id", parentId)
                .addValue("related_id", normalizeWriteValue(relatedId)));
        }
        int[] results = jdbcTemplate.batchUpdate(sql, sources.toArray(new MapSqlParameterSource[0]));
        int affected = 0;
        for (int result : results) {
            affected += Math.max(result, 0);
        }
        return affected;
    }

    private int insertPivotRows(RelationMeta relationMeta, Object parentId, Map<Object, Map<String, ?>> relatedRows) {
        if (relatedRows == null || relatedRows.isEmpty()) {
            return 0;
        }

        int affected = 0;
        for (Map.Entry<Object, Map<String, ?>> entry : relatedRows.entrySet()) {
            affected += insertPivotRow(relationMeta, parentId, entry.getKey(), entry.getValue());
        }
        return affected;
    }

    private SqlStatement buildPivotInsertStatement(RelationMeta relationMeta, Object parentId, Object relatedId, Map<String, ?> pivotValues) {
        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        columns.put(relationMeta.pivotParentKey(), normalizeWriteValue(parentId));
        columns.put(relationMeta.pivotRelatedKey(), normalizeWriteValue(relatedId));
        if (pivotValues != null) {
            for (Map.Entry<String, ?> entry : pivotValues.entrySet()) {
                String column = SqlGenerator.toColumnName(entry.getKey());
                if (TextUtils.isBlank(column)) {
                    continue;
                }
                columns.put(column, normalizeWriteValue(entry.getValue()));
            }
        }

        String sql = buildInsertSql(relationMeta.pivotTable(), columns);
        return new SqlStatement(sql, new MapSqlParameterSource(columns));
    }

    private SqlStatement buildPivotUpdateStatement(RelationMeta relationMeta, Object parentId, Object relatedId, Map<String, ?> pivotValues) {
        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        if (pivotValues != null) {
            for (Map.Entry<String, ?> entry : pivotValues.entrySet()) {
                String column = SqlGenerator.toColumnName(entry.getKey());
                if (TextUtils.isBlank(column)) {
                    continue;
                }
                columns.put(column, normalizeWriteValue(entry.getValue()));
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(relationMeta.pivotTable()).append(" SET ");
        List<String> assignments = new ArrayList<>();
        for (String column : columns.keySet()) {
            assignments.add(column + " = :" + column);
        }
        sql.append(String.join(", ", assignments));
        sql.append(" WHERE ").append(relationMeta.pivotParentKey()).append(" = :parent_id")
            .append(" AND ").append(relationMeta.pivotRelatedKey()).append(" = :related_id");

        MapSqlParameterSource params = new MapSqlParameterSource(columns)
            .addValue("parent_id", normalizeWriteValue(parentId))
            .addValue("related_id", normalizeWriteValue(relatedId));
        return new SqlStatement(sql.toString(), params);
    }

    private SqlStatement buildPivotDetachAllStatement(RelationMeta relationMeta, Object parentId) {
        String sql = "DELETE FROM " + relationMeta.pivotTable() + " WHERE " + relationMeta.pivotParentKey() + " = :parent_id";
        return new SqlStatement(sql, new MapSqlParameterSource().addValue("parent_id", parentId));
    }

    private SqlStatement buildPivotDetachStatement(RelationMeta relationMeta, Object parentId, Collection<Object> relatedIds) {
        String sql = "DELETE FROM " + relationMeta.pivotTable() + " WHERE "
            + relationMeta.pivotParentKey() + " = :parent_id AND "
            + relationMeta.pivotRelatedKey() + " IN (:related_ids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("parent_id", parentId)
            .addValue("related_ids", relatedIds);
        return new SqlStatement(sql, params);
    }

    private int deletePivotRows(RelationMeta relationMeta, Object parentId, Collection<Object> relatedIds) {
        if (relatedIds == null || relatedIds.isEmpty()) {
            return 0;
        }

        String sql = "DELETE FROM " + relationMeta.pivotTable() + " WHERE "
            + relationMeta.pivotParentKey() + " = :parent_id AND "
            + relationMeta.pivotRelatedKey() + " IN (:related_ids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("parent_id", parentId)
            .addValue("related_ids", relatedIds);
        return jdbcTemplate.update(sql, params);
    }

    private Map<String, Object> extractInsertValues(RelationRegistry.EntityMeta<T> meta, T entity) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
            String property = descriptor.getName();
            if ("class".equals(property) || !wrapper.isReadableProperty(property)) {
                continue;
            }
            if (!BeanUtils.isSimpleValueType(descriptor.getPropertyType())) {
                continue;
            }

            Object value = wrapper.getPropertyValue(property);
            if (value == null && property.equals(meta.primaryKeyProperty())) {
                continue;
            }
            values.put(SqlGenerator.toColumnName(property), normalizeWriteValue(value));
        }
        return values;
    }

    private Map<String, Object> extractUpdateValues(RelationRegistry.EntityMeta<T> meta, T entity) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
            String property = descriptor.getName();
            if ("class".equals(property) || property.equals(meta.primaryKeyProperty()) || !wrapper.isReadableProperty(property)) {
                continue;
            }
            if (!BeanUtils.isSimpleValueType(descriptor.getPropertyType())) {
                continue;
            }

            values.put(SqlGenerator.toColumnName(property), normalizeWriteValue(wrapper.getPropertyValue(property)));
        }
        return values;
    }

    private void collectPartialUpdateValue(RelationRegistry.EntityMeta<T> meta, String requested, Object explicitValue, Map<String, Object> values) {
        if (TextUtils.isBlank(requested)) {
            return;
        }
        PropertyDescriptor descriptor = findPropertyDescriptor(type, requested);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown writable property '" + requested + "' for " + type.getName());
        }
        String property = descriptor.getName();
        if (property.equals(meta.primaryKeyProperty())) {
            throw new IllegalArgumentException("Primary key property cannot be updated: " + property);
        }
        if (!BeanUtils.isSimpleValueType(descriptor.getPropertyType())) {
            throw new IllegalArgumentException("Property is not writable as a simple value: " + property);
        }
        values.put(SqlGenerator.toColumnName(property), normalizeWriteValue(explicitValue));
    }

    private int executePartialUpdate(RelationRegistry.EntityMeta<T> meta, Object primaryKeyValue, Map<String, Object> values) {
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("Primary key value is required for update");
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("No valid writable properties found for partial update");
        }

        String pkColumn = SqlGenerator.toColumnName(meta.primaryKeyProperty());
        StringBuilder sql = new StringBuilder("UPDATE ").append(meta.table()).append(" SET ");
        List<String> assignments = new ArrayList<>();
        for (String column : values.keySet()) {
            assignments.add(column + " = :" + column);
        }
        sql.append(String.join(", ", assignments));
        sql.append(" WHERE ").append(pkColumn).append(" = :__pk");

        MapSqlParameterSource params = new MapSqlParameterSource(values);
        params.addValue("__pk", normalizeWriteValue(primaryKeyValue));
        return jdbcTemplate.update(sql.toString(), params);
    }

    private PropertyDescriptor findPropertyDescriptor(Class<?> beanType, String requested) {
        String normalizedRequested = SqlGenerator.toColumnName(requested);
        for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(beanType)) {
            String property = descriptor.getName();
            if ("class".equals(property)) {
                continue;
            }
            if (property.equals(requested) || SqlGenerator.toColumnName(property).equals(normalizedRequested)) {
                return descriptor;
            }
        }
        return null;
    }

    private String buildInsertSql(String table, Map<String, Object> values) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table).append(" (");
        sql.append(String.join(", ", values.keySet()));
        sql.append(") VALUES (");
        List<String> params = new ArrayList<>();
        for (String column : values.keySet()) {
            params.add(":" + column);
        }
        sql.append(String.join(", ", params));
        sql.append(")");
        return sql.toString();
    }

    private LinkedHashSet<Object> normalizeIdSet(Collection<?> ids) {
        LinkedHashSet<Object> values = new LinkedHashSet<>();
        if (ids == null) {
            return values;
        }
        for (Object id : ids) {
            if (id != null) {
                values.add(normalizeWriteValue(id));
            }
        }
        return values;
    }

    private int insertPivotRow(RelationMeta relationMeta, Object parentId, Object relatedId, Map<String, ?> pivotValues) {
        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        columns.put(relationMeta.pivotParentKey(), normalizeWriteValue(parentId));
        columns.put(relationMeta.pivotRelatedKey(), normalizeWriteValue(relatedId));
        if (pivotValues != null) {
            for (Map.Entry<String, ?> entry : pivotValues.entrySet()) {
                String column = SqlGenerator.toColumnName(entry.getKey());
                if (TextUtils.isBlank(column)) {
                    continue;
                }
                columns.put(column, normalizeWriteValue(entry.getValue()));
            }
        }

        String sql = buildInsertSql(relationMeta.pivotTable(), columns);
        return jdbcTemplate.update(sql, new MapSqlParameterSource(columns));
    }

    private int updatePivotRow(RelationMeta relationMeta, Object parentId, Object relatedId, Map<String, ?> pivotValues) {
        if (pivotValues == null || pivotValues.isEmpty()) {
            return 0;
        }

        LinkedHashMap<String, Object> columns = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : pivotValues.entrySet()) {
            String column = SqlGenerator.toColumnName(entry.getKey());
            if (TextUtils.isBlank(column)) {
                continue;
            }
            columns.put(column, normalizeWriteValue(entry.getValue()));
        }
        if (columns.isEmpty()) {
            return 0;
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(relationMeta.pivotTable()).append(" SET ");
        List<String> assignments = new ArrayList<>();
        for (String column : columns.keySet()) {
            assignments.add(column + " = :" + column);
        }
        sql.append(String.join(", ", assignments));
        sql.append(" WHERE ").append(relationMeta.pivotParentKey()).append(" = :parent_id")
            .append(" AND ").append(relationMeta.pivotRelatedKey()).append(" = :related_id");

        MapSqlParameterSource params = new MapSqlParameterSource(columns)
            .addValue("parent_id", normalizeWriteValue(parentId))
            .addValue("related_id", normalizeWriteValue(relatedId));
        return jdbcTemplate.update(sql.toString(), params);
    }

    private LinkedHashMap<Object, Map<String, ?>> normalizePivotRows(Map<?, ? extends Map<String, ?>> rows) {
        LinkedHashMap<Object, Map<String, ?>> values = new LinkedHashMap<>();
        if (rows == null) {
            return values;
        }
        for (Map.Entry<?, ? extends Map<String, ?>> entry : rows.entrySet()) {
            Object relatedId = entry.getKey();
            if (relatedId == null) {
                continue;
            }
            values.put(normalizeWriteValue(relatedId), entry.getValue());
        }
        return values;
    }

    private static String compactSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static Object propertyValue(Object target, String property) {
        return new BeanWrapperImpl(target).getPropertyValue(property);
    }

    private static Object normalizeWriteValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof ZonedDateTime) {
            return Timestamp.from(((ZonedDateTime) value).toInstant());
        }
        if (value instanceof OffsetDateTime) {
            return Timestamp.from(((OffsetDateTime) value).toInstant());
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) value);
        }
        if (value instanceof Instant) {
            return Timestamp.from((Instant) value);
        }
        if (value instanceof BigDecimal) {
            return value;
        }
        return value;
    }
}

