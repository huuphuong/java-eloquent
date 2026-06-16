package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.meta.RelationMeta;
import io.github.huuphuong.eloquent.meta.RelationRegistry;
import io.github.huuphuong.eloquent.support.TextUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SqlGenerator {

    private SqlGenerator() {
    }

    public static SqlStatement buildRootSelect(
        RelationRegistry.EntityMeta<?> meta,
        List<FilterCondition> conditions,
        Integer page,
        Integer size
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(meta.table());
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            List<String> fragments = new ArrayList<String>();
            for (int index = 0; index < conditions.size(); index++) {
                fragments.add(conditions.get(index).toSql(params, index));
            }
            sql.append(String.join(" AND ", fragments));
        }

        appendPaging(sql, page, size);
        return new SqlStatement(sql.toString(), params);
    }

    public static String buildSelectClause(String tableAlias, List<String> requestedColumns, Collection<String> requiredColumns) {
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            return TextUtils.isBlank(tableAlias) ? "*" : tableAlias + ".*";
        }
        List<String> columns = mergeColumns(requestedColumns, requiredColumns);
        List<String> fragments = new ArrayList<String>();
        for (String column : columns) {
            String renderedColumn = toColumnName(column);
            if (!TextUtils.isBlank(tableAlias)) {
                renderedColumn = tableAlias + "." + renderedColumn;
            }
            fragments.add(renderedColumn);
        }
        return String.join(", ", fragments);
    }

    public static SqlStatement buildRelatedSelectFromParent(
        RelationRegistry.EntityMeta<?> childMeta,
        RelationMeta relationMeta,
        RelationRegistry.EntityMeta<?> parentMeta,
        Collection<?> parentIds
    ) {
        return buildRelatedSelectFromParent(childMeta, relationMeta, parentMeta, parentIds, Collections.singletonList(childMeta.primaryKeyProperty()));
    }

    public static SqlStatement buildRelatedSelectFromParent(
        RelationRegistry.EntityMeta<?> childMeta,
        RelationMeta relationMeta,
        RelationRegistry.EntityMeta<?> parentMeta,
        Collection<?> parentIds,
        List<String> requestedColumns
    ) {
        String childForeignKey = toColumnName(relationMeta.relatedKeyProperty());
        String selectClause = buildSelectClause(null, requestedColumns, java.util.Arrays.asList(childMeta.primaryKeyProperty(), relationMeta.relatedKeyProperty()));
        String sql = "SELECT " + selectClause + " FROM " + childMeta.table() + " WHERE " + childForeignKey + " IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", parentIds);
        return new SqlStatement(sql, params);
    }

    public static SqlStatement buildBelongsToSelect(
        RelationRegistry.EntityMeta<?> targetMeta,
        RelationMeta relationMeta,
        Collection<?> foreignKeys
    ) {
        return buildBelongsToSelect(targetMeta, relationMeta, foreignKeys, Collections.singletonList(targetMeta.primaryKeyProperty()));
    }

    public static SqlStatement buildBelongsToSelect(
        RelationRegistry.EntityMeta<?> targetMeta,
        RelationMeta relationMeta,
        Collection<?> foreignKeys,
        List<String> requestedColumns
    ) {
        String targetPk = toColumnName(relationMeta.relatedKeyProperty());
        String selectClause = buildSelectClause(null, requestedColumns, Collections.singletonList(targetMeta.primaryKeyProperty()));
        String sql = "SELECT " + selectClause + " FROM " + targetMeta.table() + " WHERE " + targetPk + " IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", foreignKeys);
        return new SqlStatement(sql, params);
    }

    public static SqlStatement buildBelongsToManySelect(
        RelationRegistry.EntityMeta<?> targetMeta,
        RelationMeta relationMeta,
        RelationRegistry.EntityMeta<?> parentMeta,
        Collection<?> parentIds,
        String pivotParentAlias,
        List<String> requestedColumns
    ) {
        String targetPk = toColumnName(targetMeta.primaryKeyProperty());
        String selectClause = buildSelectClause("t", requestedColumns, Collections.singletonList(targetMeta.primaryKeyProperty()));
        String sql = "SELECT " + selectClause + ", p." + relationMeta.pivotParentKey() + " AS " + pivotParentAlias
            + " FROM " + targetMeta.table() + " t JOIN " + relationMeta.pivotTable() + " p ON p."
            + relationMeta.pivotRelatedKey() + " = t." + targetPk + " WHERE p." + relationMeta.pivotParentKey() + " IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", parentIds);
        return new SqlStatement(sql, params);
    }

    public static SqlStatement buildHasOneThroughPivotSelect(
        RelationRegistry.EntityMeta<?> targetMeta,
        RelationMeta relationMeta,
        RelationRegistry.EntityMeta<?> parentMeta,
        Collection<?> parentIds,
        String pivotParentAlias,
        List<String> requestedColumns
    ) {
        String targetPk = toColumnName(targetMeta.primaryKeyProperty());
        String selectClause = buildSelectClause("t", requestedColumns, Collections.singletonList(targetMeta.primaryKeyProperty()));
        StringBuilder sql = new StringBuilder("SELECT ").append(selectClause)
            .append(", p.").append(relationMeta.pivotParentKey()).append(" AS ").append(pivotParentAlias)
            .append(" FROM ").append(targetMeta.table()).append(" t JOIN ").append(relationMeta.pivotTable())
            .append(" p ON p.").append(relationMeta.pivotRelatedKey()).append(" = t.").append(targetPk)
            .append(" WHERE p.").append(relationMeta.pivotParentKey()).append(" IN (:ids)");

        if (!TextUtils.isBlank(relationMeta.pivotWhereColumn())) {
            sql.append(" AND p.")
                .append(toColumnName(relationMeta.pivotWhereColumn()))
                .append(" = :pivot_where_value");
        }

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", parentIds);
        if (!TextUtils.isBlank(relationMeta.pivotWhereColumn())) {
            params.addValue("pivot_where_value", relationMeta.pivotWhereValue());
        }
        return new SqlStatement(sql.toString(), params);
    }

    public static String buildWindowOrderClause(List<WithNode.OrderClause> orderBy, String baseAlias, String defaultColumnExpression) {
        if (orderBy == null || orderBy.isEmpty()) {
            return defaultColumnExpression + " ASC";
        }
        List<String> fragments = new ArrayList<String>();
        for (WithNode.OrderClause clause : orderBy) {
            String column = toColumnName(clause.column());
            if (!column.contains(".")) {
                column = baseAlias + "." + column;
            }
            fragments.add(column + " " + clause.direction());
        }
        return String.join(", ", fragments);
    }

    public static SqlStatement wrapWithPerParentLimit(
        SqlStatement baseStatement,
        String partitionColumn,
        String orderClause,
        int limit
    ) {
        String sql = "SELECT * FROM (SELECT base.*, ROW_NUMBER() OVER (PARTITION BY base." + partitionColumn
            + " ORDER BY " + orderClause + ") AS __relation_row_number FROM (" + baseStatement.sql() + ") base) limited "
            + "WHERE limited.__relation_row_number <= " + limit;
        return new SqlStatement(sql, baseStatement.parameters());
    }

    private static List<String> mergeColumns(List<String> requestedColumns, Collection<String> requiredColumns) {
        Set<String> merged = new LinkedHashSet<String>();
        if (requestedColumns != null) {
            for (String column : requestedColumns) {
                if (!TextUtils.isBlank(column)) {
                    merged.add(toColumnName(column));
                }
            }
        }
        if (requiredColumns != null) {
            for (String column : requiredColumns) {
                if (!TextUtils.isBlank(column)) {
                    merged.add(toColumnName(column));
                }
            }
        }
        return new ArrayList<String>(merged);
    }

    private static void appendPaging(StringBuilder sql, Integer page, Integer size) {
        if (size == null || size <= 0) {
            return;
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int offset = (safePage - 1) * size;
        sql.append(" LIMIT ").append(size).append(" OFFSET ").append(offset);
    }

    public static String toColumnName(String propertyName) {
        if (TextUtils.isBlank(propertyName)) {
            return propertyName;
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < propertyName.length(); index++) {
            char current = propertyName.charAt(index);
            if (Character.isUpperCase(current)) {
                if (index > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}

