package io.github.huuphuong.eloquent.query;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Arrays;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterConditionTest {

    @Test
    void rendersRawSqlWithBoundParameters() {
        FilterCondition condition = FilterCondition.raw("level_no > ? AND status = ?", Arrays.asList(3, "ACTIVE"));
        MapSqlParameterSource params = new MapSqlParameterSource();

        String sql = condition.toSql(params, 0);

        assertEquals("level_no > :p_0_0 AND status = :p_0_1", sql);
        assertEquals(3, params.getValue("p_0_0"));
        assertEquals("ACTIVE", params.getValue("p_0_1"));
    }

    @Test
    void normalizesDateValuesFromDifferentTemporalTypes() {
        MapSqlParameterSource localDateParams = new MapSqlParameterSource();
        String localDateSql = FilterCondition.date("createdAt", LocalDate.of(2024, 6, 15)).toSql(localDateParams, 0);
        assertEquals("DATE(created_at) = :p_0", localDateSql);
        assertEquals(LocalDate.of(2024, 6, 15), localDateParams.getValue("p_0"));

        MapSqlParameterSource localDateTimeParams = new MapSqlParameterSource();
        String localDateTimeSql = FilterCondition.date("createdAt", LocalDateTime.of(2024, 6, 15, 23, 45)).toSql(localDateTimeParams, 1);
        assertEquals("DATE(created_at) = :p_1", localDateTimeSql);
        assertEquals(LocalDate.of(2024, 6, 15), localDateTimeParams.getValue("p_1"));

        MapSqlParameterSource zonedDateTimeParams = new MapSqlParameterSource();
        ZonedDateTime zoned = ZonedDateTime.of(2024, 6, 15, 0, 30, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh"));
        String zonedDateTimeSql = FilterCondition.date("createdAt", zoned).toSql(zonedDateTimeParams, 2);
        assertEquals("DATE(created_at) = :p_2", zonedDateTimeSql);
        assertEquals(LocalDate.of(2024, 6, 15), zonedDateTimeParams.getValue("p_2"));
    }

    @Test
    void rejectsMismatchedRawPlaceholderCounts() {
        MapSqlParameterSource params = new MapSqlParameterSource();

        assertThrows(IllegalArgumentException.class, () -> FilterCondition.raw("level_no > ? AND status = ?", Arrays.asList(3)).toSql(params, 3));
        assertThrows(IllegalArgumentException.class, () -> FilterCondition.raw("level_no > ?", Arrays.asList(3, "ACTIVE")).toSql(params, 4));
    }

}

