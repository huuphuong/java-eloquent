package io.github.phuonghuu.eloquent.support;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RowMapperFactoryTest {

    @Test
    void mapsTimestampToZonedDateTime() throws Exception {
        RowMapper<EventRecord> rowMapper = RowMapperFactory.create(EventRecord.class);
        ResultSet resultSet = resultSet(
            column("id", 7L, Types.BIGINT, Long.class.getName()),
            column("created_at", Timestamp.valueOf("2024-06-15 12:34:56"), Types.TIMESTAMP, Timestamp.class.getName())
        );

        EventRecord record = rowMapper.mapRow(resultSet, 0);

        assertEquals(Long.valueOf(7L), record.getId());
        assertEquals(
            Timestamp.valueOf("2024-06-15 12:34:56").toInstant().atZone(ZoneId.systemDefault()),
            record.getCreatedAt()
        );
    }

    private ResultSet resultSet(Column... columns) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        List<Column> columnList = new ArrayList<Column>();
        for (Column column : columns) {
            columnList.add(column);
        }

        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columnList.size());
        for (int i = 0; i < columnList.size(); i++) {
            int columnIndex = i + 1;
            Column column = columnList.get(i);
            when(metaData.getColumnLabel(columnIndex)).thenReturn(column.name);
            when(metaData.getColumnName(columnIndex)).thenReturn(column.name);
            when(metaData.getColumnType(columnIndex)).thenReturn(column.sqlType);
            when(metaData.getColumnClassName(columnIndex)).thenReturn(column.className);
        }

        doAnswer(invocation -> valueByIndex(columnList, (Integer) invocation.getArgument(0))).when(resultSet).getObject(anyInt());
        doAnswer(invocation -> valueByName(columnList, (String) invocation.getArgument(0))).when(resultSet).getObject(any(String.class));
        doAnswer(invocation -> valueByIndex(columnList, (Integer) invocation.getArgument(0))).when(resultSet).getObject(anyInt(), any(Class.class));
        doAnswer(invocation -> valueByName(columnList, (String) invocation.getArgument(0))).when(resultSet).getObject(any(String.class), any(Class.class));
        doAnswer(invocation -> asTimestamp(valueByIndex(columnList, (Integer) invocation.getArgument(0)))).when(resultSet).getTimestamp(anyInt());
        doAnswer(invocation -> asTimestamp(valueByName(columnList, (String) invocation.getArgument(0)))).when(resultSet).getTimestamp(any(String.class));
        doAnswer(invocation -> asLong(valueByIndex(columnList, (Integer) invocation.getArgument(0)))).when(resultSet).getLong(anyInt());
        doAnswer(invocation -> asLong(valueByName(columnList, (String) invocation.getArgument(0)))).when(resultSet).getLong(any(String.class));
        doAnswer(invocation -> asInt(valueByIndex(columnList, (Integer) invocation.getArgument(0)))).when(resultSet).getInt(anyInt());
        doAnswer(invocation -> asInt(valueByName(columnList, (String) invocation.getArgument(0)))).when(resultSet).getInt(any(String.class));
        when(resultSet.wasNull()).thenReturn(false);
        return resultSet;
    }

    private Object valueByIndex(List<Column> columns, int index) {
        if (index < 1 || index > columns.size()) {
            return null;
        }
        return columns.get(index - 1).value;
    }

    private Object valueByName(List<Column> columns, String name) {
        for (Column column : columns) {
            if (column.name.equals(name)) {
                return column.value;
            }
        }
        return null;
    }

    private Column column(String name, Object value, int sqlType, String className) {
        return new Column(name, value, sqlType, className);
    }

    private Timestamp asTimestamp(Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return null;
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static final class Column {
        private final String name;
        private final Object value;
        private final int sqlType;
        private final String className;

        private Column(String name, Object value, int sqlType, String className) {
            this.name = name;
            this.value = value;
            this.sqlType = sqlType;
            this.className = className;
        }
    }

    public static final class EventRecord {
        private Long id;
        private ZonedDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public ZonedDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(ZonedDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}

