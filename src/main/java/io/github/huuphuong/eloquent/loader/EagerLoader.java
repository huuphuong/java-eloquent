package io.github.huuphuong.eloquent.loader;

import io.github.huuphuong.eloquent.meta.RelationKind;
import io.github.huuphuong.eloquent.meta.RelationMeta;
import io.github.huuphuong.eloquent.meta.RelationRegistry;
import io.github.huuphuong.eloquent.query.QueryBuilder;
import io.github.huuphuong.eloquent.query.SqlGenerator;
import io.github.huuphuong.eloquent.query.SqlStatement;
import io.github.huuphuong.eloquent.query.WithNode;
import io.github.huuphuong.eloquent.support.RowMapperFactory;
import io.github.huuphuong.eloquent.support.TextUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class EagerLoader {

    private static final String PIVOT_PARENT_ALIAS = "__relation_parent_id";

    private EagerLoader() {
    }

    public static <T> void load(
        List<T> parents,
        Class<T> parentType,
        WithNode withTree,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        if (CollectionUtils.isEmpty(parents) || withTree == null || withTree.children().isEmpty()) {
            return;
        }

        RelationRegistry.EntityMeta<T> parentMeta = registry.require(parentType);
        for (WithNode node : withTree.children()) {
            RelationMeta relationMeta = parentMeta.relation(node.name());
            loadRelation(parents, parentMeta, relationMeta, node, registry, jdbcTemplate);
        }
    }

    private static <P> void loadRelation(
        List<P> parents,
        RelationRegistry.EntityMeta<P> parentMeta,
        RelationMeta relationMeta,
        WithNode node,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        switch (relationMeta.kind()) {
            case HAS_ONE:
            case HAS_MANY:
                loadFromParentToChild(parents, parentMeta, relationMeta, node, registry, jdbcTemplate);
                break;
            case BELONGS_TO:
                loadBelongsTo(parents, parentMeta, relationMeta, node, registry, jdbcTemplate);
                break;
            case BELONGS_TO_MANY:
                loadBelongsToMany(parents, parentMeta, relationMeta, node, registry, jdbcTemplate);
                break;
            case HAS_ONE_THROUGH_PIVOT:
                loadHasOneThroughPivot(parents, parentMeta, relationMeta, node, registry, jdbcTemplate);
                break;
            default:
                throw new IllegalStateException("Unsupported relation kind: " + relationMeta.kind());
        }
    }

    private static <P> void loadFromParentToChild(
        List<P> parents,
        RelationRegistry.EntityMeta<P> parentMeta,
        RelationMeta relationMeta,
        WithNode node,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        List<Object> parentIds = distinctValues(parents, relationMeta.localKeyProperty());
        if (parentIds.isEmpty()) {
            attachEmpty(parentMeta, parents, relationMeta);
            return;
        }

        RelationRegistry.EntityMeta<?> childMeta = registry.require((Class<?>) relationMeta.relatedType());
        SqlStatement statement = SqlGenerator.buildRelatedSelectFromParent(
            childMeta,
            relationMeta,
            parentMeta,
            parentIds,
            node.selectColumns()
        );
        String sql = statement.sql();
        String childPredicate = buildNodePredicate(node, statement.parameters(), new java.util.concurrent.atomic.AtomicInteger(1000), "child");
        if (!TextUtils.isBlank(childPredicate)) {
            sql += " AND " + childPredicate;
        }
        sql = applyRelationModifiers(sql, node);
        List<?> children = jdbcTemplate.query(sql, statement.parameters(), RowMapperFactory.create(relationMeta.relatedType()));
        Map<Object, List<Object>> grouped = groupBy(children, relationMeta.relatedKeyProperty());

        for (P parent : parents) {
            Object key = propertyValue(parent, relationMeta.localKeyProperty());
            if (relationMeta.kind() == RelationKind.HAS_ONE) {
                List<Object> list = grouped.get(key);
                relationMeta.attach(parent, list == null || list.isEmpty() ? null : list.get(0));
            } else {
                relationMeta.attach(parent, new ArrayList<Object>(grouped.containsKey(key) ? grouped.get(key) : Collections.<Object>emptyList()));
            }
        }

        if (node.children().isEmpty() || children.isEmpty()) {
            return;
        }
        load((List<Object>) children, (Class<Object>) relationMeta.relatedType(), node, registry, jdbcTemplate);
    }

    private static <P> void loadBelongsTo(
        List<P> parents,
        RelationRegistry.EntityMeta<P> parentMeta,
        RelationMeta relationMeta,
        WithNode node,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        List<Object> foreignKeys = distinctValues(parents, relationMeta.localKeyProperty());
        if (foreignKeys.isEmpty()) {
            for (P parent : parents) {
                relationMeta.attach(parent, null);
            }
            return;
        }

        RelationRegistry.EntityMeta<?> targetMeta = registry.require((Class<?>) relationMeta.relatedType());
        SqlStatement statement = SqlGenerator.buildBelongsToSelect(
            targetMeta,
            relationMeta,
            foreignKeys,
            node.selectColumns()
        );
        String sql = statement.sql();
        String childPredicate = buildNodePredicate(node, statement.parameters(), new java.util.concurrent.atomic.AtomicInteger(1000), "child");
        if (!TextUtils.isBlank(childPredicate)) {
            sql += " AND " + childPredicate;
        }
        sql = applyRelationModifiers(sql, node);
        List<?> children = jdbcTemplate.query(sql, statement.parameters(), RowMapperFactory.create(relationMeta.relatedType()));
        Map<Object, Object> indexed = indexBy(children, relationMeta.relatedKeyProperty());

        for (P parent : parents) {
            Object fk = propertyValue(parent, relationMeta.localKeyProperty());
            relationMeta.attach(parent, indexed.get(fk));
        }

        if (node.children().isEmpty() || children.isEmpty()) {
            return;
        }
        load((List<Object>) children, (Class<Object>) relationMeta.relatedType(), node, registry, jdbcTemplate);
    }

    private static <P> void loadBelongsToMany(
        List<P> parents,
        RelationRegistry.EntityMeta<P> parentMeta,
        RelationMeta relationMeta,
        WithNode node,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        List<Object> parentIds = distinctValues(parents, parentMeta.primaryKeyProperty());
        if (parentIds.isEmpty()) {
            attachEmpty(parentMeta, parents, relationMeta);
            return;
        }

        RelationRegistry.EntityMeta<?> targetMeta = registry.require((Class<?>) relationMeta.relatedType());
        SqlStatement statement = SqlGenerator.buildBelongsToManySelect(
            targetMeta,
            relationMeta,
            parentMeta,
            parentIds,
            PIVOT_PARENT_ALIAS,
            node.selectColumns()
        );
        String sql = statement.sql();
        String childPredicate = buildNodePredicate(node, statement.parameters(), new java.util.concurrent.atomic.AtomicInteger(1000), "t");
        if (!TextUtils.isBlank(childPredicate)) {
            sql += " AND " + childPredicate;
        }
        if (node.perParentLimit() != null) {
            String orderClause = SqlGenerator.buildWindowOrderClause(
                node.orderBy(),
                "base",
                "base." + SqlGenerator.toColumnName(targetMeta.primaryKeyProperty())
            );
            statement = SqlGenerator.wrapWithPerParentLimit(
                new SqlStatement(sql, statement.parameters()),
                "__relation_parent_id",
                orderClause,
                node.perParentLimit()
            );
            sql = statement.sql();
        } else {
            sql = applyRelationModifiers(sql, node);
        }
        List<PivotRow> rows = jdbcTemplate.query(
            sql,
            statement.parameters(),
            (rs, rowNum) -> new PivotRow(
                RowMapperFactory.create(relationMeta.relatedType()).mapRow(rs, rowNum),
                rs.getObject(PIVOT_PARENT_ALIAS)
            )
        );
        Map<Object, List<Object>> grouped = groupByPivotParent(rows);

        for (P parent : parents) {
            Object parentId = propertyValue(parent, parentMeta.primaryKeyProperty());
            relationMeta.attach(parent, new ArrayList<Object>(grouped.containsKey(parentId) ? grouped.get(parentId) : Collections.<Object>emptyList()));
        }

        List<Object> children = new ArrayList<Object>();
        for (PivotRow row : rows) {
            children.add(row.entity());
        }
        if (node.children().isEmpty() || children.isEmpty()) {
            return;
        }
        load((List<Object>) children, (Class<Object>) relationMeta.relatedType(), node, registry, jdbcTemplate);
    }

    private static <P> void loadHasOneThroughPivot(
        List<P> parents,
        RelationRegistry.EntityMeta<P> parentMeta,
        RelationMeta relationMeta,
        WithNode node,
        RelationRegistry registry,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        List<Object> parentIds = distinctValues(parents, parentMeta.primaryKeyProperty());
        if (parentIds.isEmpty()) {
            attachEmpty(parentMeta, parents, relationMeta);
            return;
        }

        RelationRegistry.EntityMeta<?> targetMeta = registry.require((Class<?>) relationMeta.relatedType());
        SqlStatement statement = SqlGenerator.buildHasOneThroughPivotSelect(
            targetMeta,
            relationMeta,
            parentMeta,
            parentIds,
            PIVOT_PARENT_ALIAS,
            node.selectColumns()
        );
        String sql = statement.sql();
        String childPredicate = buildNodePredicate(node, statement.parameters(), new java.util.concurrent.atomic.AtomicInteger(1000), "t");
        if (!TextUtils.isBlank(childPredicate)) {
            sql += " AND " + childPredicate;
        }
        if (node.perParentLimit() != null) {
            String orderClause = SqlGenerator.buildWindowOrderClause(
                node.orderBy(),
                "base",
                "base." + SqlGenerator.toColumnName(targetMeta.primaryKeyProperty())
            );
            statement = SqlGenerator.wrapWithPerParentLimit(
                new SqlStatement(sql, statement.parameters()),
                "__relation_parent_id",
                orderClause,
                node.perParentLimit()
            );
            sql = statement.sql();
        } else {
            sql = applyRelationModifiers(sql, node);
        }
        List<PivotRow> rows = jdbcTemplate.query(
            sql,
            statement.parameters(),
            (rs, rowNum) -> new PivotRow(
                RowMapperFactory.create(relationMeta.relatedType()).mapRow(rs, rowNum),
                rs.getObject(PIVOT_PARENT_ALIAS)
            )
        );
        Map<Object, List<Object>> grouped = groupByPivotParent(rows);

        for (P parent : parents) {
            Object parentId = propertyValue(parent, parentMeta.primaryKeyProperty());
            List<Object> list = grouped.get(parentId);
            relationMeta.attach(parent, list == null || list.isEmpty() ? null : list.get(0));
        }

        List<Object> children = new ArrayList<Object>();
        for (PivotRow row : rows) {
            children.add(row.entity());
        }
        if (node.children().isEmpty() || children.isEmpty()) {
            return;
        }
        load((List<Object>) children, (Class<Object>) relationMeta.relatedType(), node, registry, jdbcTemplate);
    }

    private static <P> void attachEmpty(
        RelationRegistry.EntityMeta<P> parentMeta,
        List<P> parents,
        RelationMeta relationMeta
    ) {
        for (P parent : parents) {
            if (relationMeta.collection()) {
                relationMeta.attach(parent, Collections.emptyList());
            } else {
                relationMeta.attach(parent, null);
            }
        }
    }

    private static Map<Object, List<Object>> groupByPivotParent(List<PivotRow> rows) {
        Map<Object, List<Object>> grouped = new LinkedHashMap<>();
        for (PivotRow row : rows) {
            grouped.computeIfAbsent(row.parentId(), ignored -> new ArrayList<>()).add(row.entity());
        }
        return grouped;
    }

    private static Map<Object, List<Object>> groupBy(List<?> rows, String property) {
        Map<Object, List<Object>> grouped = new LinkedHashMap<>();
        for (Object row : rows) {
            Object key = propertyValue(row, property);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private static Map<Object, Object> indexBy(List<?> rows, String property) {
        Map<Object, Object> indexed = new LinkedHashMap<>();
        for (Object row : rows) {
            indexed.put(propertyValue(row, property), row);
        }
        return indexed;
    }

    private static List<Object> distinctValues(List<?> rows, String property) {
        List<Object> values = new ArrayList<>();
        for (Object row : rows) {
            Object value = propertyValue(row, property);
            if (value != null && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private static Object propertyValue(Object target, String property) {
        return new org.springframework.beans.BeanWrapperImpl(target).getPropertyValue(property);
    }

    public static String applyRelationModifiers(String sql, WithNode node) {
        StringBuilder builder = new StringBuilder(sql);
        if (!node.orderBy().isEmpty()) {
            List<String> fragments = new ArrayList<>();
            for (WithNode.OrderClause clause : node.orderBy()) {
                fragments.add(SqlGenerator.toColumnName(clause.column()) + " " + clause.direction());
            }
            builder.append(" ORDER BY ").append(String.join(", ", fragments));
        }

        if (node.limit() != null) {
            builder.append(" LIMIT ").append(node.limit());
        }
        if (node.offset() != null) {
            builder.append(" OFFSET ").append(node.offset());
        }
        return builder.toString();
    }

    public static String buildNodePredicate(
        WithNode node,
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params,
        java.util.concurrent.atomic.AtomicInteger index,
        String alias
    ) {
        List<String> fragments = new ArrayList<>();
        for (WithNode.PredicateClause clause : node.predicates()) {
            String fragment = clause.predicate().toSql(null, params, index, alias);
            if (TextUtils.isBlank(fragment)) {
                continue;
            }
            if (fragments.isEmpty()) {
                fragments.add(fragment);
            } else {
                fragments.add(clause.connector() + " " + fragment);
            }
        }
        return String.join(" ", fragments);
    }

    private static final class PivotRow {
        private final Object entity;
        private final Object parentId;

        private PivotRow(Object entity, Object parentId) {
            this.entity = entity;
            this.parentId = parentId;
        }

        private Object entity() {
            return entity;
        }

        private Object parentId() {
            return parentId;
        }
    }
}

