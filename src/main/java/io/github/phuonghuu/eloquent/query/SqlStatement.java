package io.github.phuonghuu.eloquent.query;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlStatement {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!:):([A-Za-z0-9_]+)");

    private final String sql;
    private final MapSqlParameterSource parameters;

    public SqlStatement(String sql, MapSqlParameterSource parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String sql() {
        return sql;
    }

    public MapSqlParameterSource parameters() {
        return parameters;
    }

    public String toDebugSql() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (parameters != null) {
            Arrays.stream(parameters.getParameterNames()).forEach(name -> values.put(name, parameters.getValue(name)));
        }

        Matcher matcher = PARAMETER_PATTERN.matcher(sql);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = values.get(name);
            if (value instanceof Collection<?> && ((Collection<?>) value).isEmpty()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(":" + name));
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(formatValue(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) value;
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(formatValue(item));
                first = false;
            }
            return builder.toString();
        }
        String escaped = value.toString().replace("'", "''");
        return "'" + escaped + "'";
    }
}

