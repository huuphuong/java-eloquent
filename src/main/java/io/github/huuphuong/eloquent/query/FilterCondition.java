package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.meta.RelationRegistry;
import io.github.huuphuong.eloquent.support.TextUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FilterCondition implements QueryPredicate {

    private enum Kind {
        EQUALS,
        OPERATOR,
        IN,
        IS_NULL,
        NOT_NULL,
        BETWEEN,
        LIKE,
        DATE,
        RAW
    }

    private static final Pattern QUESTION_MARK_PATTERN = Pattern.compile("\\?");

    private final Kind kind;
    private final String column;
    private final String operator;
    private final Object value;
    private final List<?> rawValues;
    private final Object secondValue;

    private FilterCondition(Kind kind, String column, String operator, Object value, Object secondValue, List<?> rawValues) {
        this.kind = kind;
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.secondValue = secondValue;
        this.rawValues = rawValues;
    }

    static FilterCondition equals(String column, Object value) {
        return new FilterCondition(Kind.EQUALS, column, "=", value, null, null);
    }

    static FilterCondition operator(String column, String operator, Object value) {
        return new FilterCondition(Kind.OPERATOR, column, normalizeOperator(operator), value, null, null);
    }

    static FilterCondition in(String column, List<?> values) {
        return new FilterCondition(Kind.IN, column, "IN", values, null, null);
    }

    static FilterCondition isNull(String column) {
        return new FilterCondition(Kind.IS_NULL, column, "IS NULL", null, null, null);
    }

    static FilterCondition isNotNull(String column) {
        return new FilterCondition(Kind.NOT_NULL, column, "IS NOT NULL", null, null, null);
    }

    static FilterCondition between(String column, Object start, Object end) {
        return new FilterCondition(Kind.BETWEEN, column, "BETWEEN", start, end, null);
    }

    static FilterCondition like(String column, Object value) {
        return new FilterCondition(Kind.LIKE, column, "LIKE", value, null, null);
    }

    static FilterCondition date(String column, Object value) {
        return new FilterCondition(Kind.DATE, column, "DATE", value, null, null);
    }

    static FilterCondition raw(String expression, List<?> values) {
        return new FilterCondition(Kind.RAW, expression, null, null, null, values);
    }

    @Override
    public String toSql(
        RelationRegistry.EntityMeta<?> meta,
        MapSqlParameterSource params,
        AtomicInteger index,
        String tableAlias
    ) {
        return toSql(params, index.getAndIncrement(), tableAlias);
    }

    String toSql(MapSqlParameterSource params, int index) {
        return toSql(params, index, null);
    }

    String toSql(MapSqlParameterSource params, int index, String tableAlias) {
        String columnName = SqlGenerator.toColumnName(column);
        if (!TextUtils.isBlank(tableAlias) && !columnName.contains(".")) {
            columnName = tableAlias + "." + columnName;
        }
        if (kind == Kind.EQUALS) {
            String param = "p_" + index;
            params.addValue(param, value);
            return columnName + " = :" + param;
        }
        if (kind == Kind.OPERATOR) {
            if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
                return columnName + " " + operator;
            }
            String param = "p_" + index;
            params.addValue(param, value);
            return columnName + " " + operator + " :" + param;
        }
        if (kind == Kind.IN) {
            String param = "p_" + index;
            params.addValue(param, value);
            return columnName + " IN (:" + param + ")";
        }
        if (kind == Kind.IS_NULL) {
            return columnName + " IS NULL";
        }
        if (kind == Kind.NOT_NULL) {
            return columnName + " IS NOT NULL";
        }
        if (kind == Kind.BETWEEN) {
            String startParam = "p_" + index + "_0";
            String endParam = "p_" + index + "_1";
            params.addValue(startParam, value);
            params.addValue(endParam, secondValue);
            return columnName + " BETWEEN :" + startParam + " AND :" + endParam;
        }
        if (kind == Kind.LIKE) {
            String param = "p_" + index;
            params.addValue(param, value);
            return columnName + " LIKE :" + param;
        }
        if (kind == Kind.DATE) {
            String param = "p_" + index;
            params.addValue(param, normalizeDateValue(value));
            return "DATE(" + columnName + ") = :" + param;
        }
        return renderRaw(params, index);
    }

    private String renderRaw(MapSqlParameterSource params, int index) {
        if (TextUtils.isBlank(column)) {
            return "";
        }
        Matcher matcher = QUESTION_MARK_PATTERN.matcher(column);
        StringBuffer result = new StringBuffer();
        AtomicInteger rawIndex = new AtomicInteger(0);
        while (matcher.find()) {
            if (rawValues == null || rawIndex.get() >= rawValues.size()) {
                throw new IllegalArgumentException("whereRaw parameter count does not match placeholders");
            }
            Object rawValue = rawValues.get(rawIndex.getAndIncrement());
            String param = "p_" + index + "_" + (rawIndex.get() - 1);
            params.addValue(param, rawValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(":" + param));
        }
        if (rawValues != null && rawIndex.get() != rawValues.size()) {
            throw new IllegalArgumentException("whereRaw parameter count does not match placeholders");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String normalizeOperator(String operator) {
        if (TextUtils.isBlank(operator)) {
            return "=";
        }
        return operator.trim().toUpperCase();
    }

    private static Object normalizeDateValue(Object value) {
        if (value instanceof LocalDate) {
            return value;
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toLocalDate();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        }
        return value;
    }

}

