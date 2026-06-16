package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.api.CursorPaginatedResult;
import io.github.huuphuong.eloquent.api.PaginatedResult;
import io.github.huuphuong.eloquent.meta.RelationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryBuilderReadTest {

    @Test
    void getLoadsNestedRelationsAndThroughPivotRelations() throws Exception {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateForReadGraph();

        QueryBuilder<Department> query = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        List<Department> departments = query
            .with("card")
            .with("users.profile")
            .with("manager")
            .orderBy("id")
            .get();

        assertEquals(2, departments.size());

        Department engineering = departments.get(0);
        assertEquals(Long.valueOf(1L), engineering.getId());
        assertEquals(Long.valueOf(500L), engineering.getCard().getId());
        assertEquals(2, engineering.getUsers().size());
        assertEquals(Long.valueOf(10L), engineering.getUsers().get(0).getId());
        assertEquals(Long.valueOf(100L), engineering.getUsers().get(0).getProfile().getId());
        assertNull(engineering.getUsers().get(1).getProfile());
        assertEquals(Long.valueOf(99L), engineering.getManager().getId());

        Department support = departments.get(1);
        assertEquals(Long.valueOf(2L), support.getId());
        assertNull(support.getCard());
        assertTrue(support.getUsers().isEmpty());
        assertNull(support.getManager());

        verify(jdbcTemplate, times(1)).query(
            containsSql("FROM departments"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        verify(jdbcTemplate, times(1)).query(
            containsSql("FROM department_cards WHERE department_id IN"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        verify(jdbcTemplate, times(1)).query(
            containsSql("FROM users WHERE department_id IN"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        verify(jdbcTemplate, times(1)).query(
            containsSql("FROM profiles WHERE id IN"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        verify(jdbcTemplate, times(1)).query(
            containsSql("JOIN positions p"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
    }

    @Test
    void paginateReturnsMetadataAndUsesCountQuery() throws Exception {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateForReadGraph();

        QueryBuilder<Department> query = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        PaginatedResult<Department> result = query.orderBy("id").paginate(2, 2);

        assertEquals(4L, result.total());
        assertEquals(2, result.currentPage());
        assertEquals(2, result.size());
        assertEquals(2L, result.totalPages());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());
        assertEquals(2, result.items().size());
        assertEquals(Long.valueOf(3L), result.items().get(0).getId());
        assertEquals(Long.valueOf(4L), result.items().get(1).getId());

        verify(jdbcTemplate, times(1)).queryForObject(
            containsSql("SELECT COUNT(*) FROM departments"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            eq(Number.class)
        );
    }

    @Test
    void cursorPaginateSupportsForwardPaging() throws Exception {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateForReadGraph();

        QueryBuilder<Department> firstQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        CursorPaginatedResult<Department> firstPage = firstQuery.orderBy("id").cursorPaginate(null, 2);

        assertEquals(2, firstPage.items().size());
        assertEquals(Long.valueOf(2L), firstPage.nextCursorId());
        assertTrue(firstPage.hasNext());
        assertEquals(Long.valueOf(1L), firstPage.items().get(0).getId());
        assertEquals(Long.valueOf(2L), firstPage.items().get(1).getId());

        QueryBuilder<Department> cursorOneQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        CursorPaginatedResult<Department> cursorOnePage = cursorOneQuery.orderBy("id").cursorPaginate(1L, 2);

        assertEquals(2, cursorOnePage.items().size());
        assertEquals(Long.valueOf(3L), cursorOnePage.nextCursorId());
        assertTrue(cursorOnePage.hasNext());
        assertEquals(Long.valueOf(2L), cursorOnePage.items().get(0).getId());
        assertEquals(Long.valueOf(3L), cursorOnePage.items().get(1).getId());

        QueryBuilder<Department> nextQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        CursorPaginatedResult<Department> nextPage = nextQuery.orderBy("id").cursorPaginate(2L, 2);

        assertEquals(2, nextPage.items().size());
        assertEquals(Long.valueOf(4L), nextPage.nextCursorId());
        assertFalse(nextPage.hasNext());
        assertEquals(Long.valueOf(3L), nextPage.items().get(0).getId());
        assertEquals(Long.valueOf(4L), nextPage.items().get(1).getId());
    }

    @Test
    void firstFindAndEmptyWhereInAreSafe() throws Exception {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = jdbcTemplateForReadGraph();

        QueryBuilder<Department> firstQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        Department first = firstQuery.orderBy("id").first();
        assertEquals(Long.valueOf(1L), first.getId());

        QueryBuilder<Department> missingFirstQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        assertNull(missingFirstQuery.where("name", "missing").first());

        QueryBuilder<Department> findQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        Department found = findQuery.find(2L);
        assertEquals(Long.valueOf(2L), found.getId());

        QueryBuilder<Department> emptyQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        List<Department> empty = emptyQuery.whereIn("id", Collections.emptyList()).get();
        assertTrue(empty.isEmpty());

        QueryBuilder<Department> nullFindQuery = new QueryBuilder<>(jdbcTemplate, registry, Department.class);
        assertNull(nullFindQuery.find(null));

        verify(jdbcTemplate, times(3)).query(
            containsSql("FROM departments"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        verify(jdbcTemplate, times(0)).query(
            containsSql("WHERE 1 = 0"),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
    }

    @Test
    void rejectsInvalidPaginationAndCursorConfiguration() {
        RelationRegistry registry = registry();
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        QueryBuilder<Department> query = new QueryBuilder<>(jdbcTemplate, registry, Department.class);

        assertThrows(IllegalArgumentException.class, () -> query.paginate(0, 10));
        assertThrows(IllegalArgumentException.class, () -> query.paginate(1, 0));
        assertThrows(IllegalArgumentException.class, () -> query.cursorPaginate(null, 0));
        assertThrows(IllegalStateException.class, () -> query.cursorPaginate(null));

        assertThrows(IllegalStateException.class, () -> query.orderBy("name").cursorPaginate(null, 10));
        assertThrows(IllegalStateException.class, () -> query.orderBy("id").orderBy("name").cursorPaginate(null, 10));
    }

    private NamedParameterJdbcTemplate jdbcTemplateForReadGraph() throws Exception {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.any(MapSqlParameterSource.class), eq(Number.class)))
            .thenReturn(4);
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            MapSqlParameterSource params = invocation.getArgument(1);
            RowMapper<?> rowMapper = invocation.getArgument(2);

            if (sql.contains("FROM departments WHERE id >")) {
                Object cursor = params.getValue("p_0");
                if (Long.valueOf(1L).equals(cursor)) {
                    return mapRows(rowMapper, departmentRows(2L, 3L, 4L));
                }
                if (Long.valueOf(2L).equals(cursor)) {
                    return mapRows(rowMapper, departmentRows(3L, 4L));
                }
                return mapRows(rowMapper, departmentRows(2L, 3L));
            }
            if (sql.contains("FROM departments WHERE id <")) {
                Object cursor = params.getValue("p_0");
                if (Long.valueOf(4L).equals(cursor)) {
                    return mapRows(rowMapper, departmentRows(3L, 2L));
                }
                return mapRows(rowMapper, departmentRows(2L, 1L));
            }
            if (sql.contains("FROM departments")) {
                if (sql.contains("WHERE name = :p_0")) {
                    return Collections.emptyList();
                }
                if (sql.contains("LIMIT 1")) {
                    return mapRows(rowMapper, departmentRows(1L));
                }
                if (sql.contains("WHERE id = :p_0")) {
                    return mapRows(rowMapper, departmentRows(2L));
                }
                if (sql.contains("LIMIT 3")) {
                    if (sql.contains("WHERE id > :p_0")) {
                        Object cursor = params.getValue("p_0");
                        if (Long.valueOf(1L).equals(cursor)) {
                            return mapRows(rowMapper, departmentRows(2L, 3L, 4L));
                        }
                        if (Long.valueOf(2L).equals(cursor)) {
                            return mapRows(rowMapper, departmentRows(3L, 4L));
                        }
                    }
                    if (sql.contains("WHERE id < :p_0")) {
                        Object cursor = params.getValue("p_0");
                        if (Long.valueOf(4L).equals(cursor)) {
                            return mapRows(rowMapper, departmentRows(3L, 2L, 1L));
                        }
                    }
                    return mapRows(rowMapper, departmentRows(1L, 2L, 3L));
                }
                if (sql.contains("OFFSET 2")) {
                    return mapRows(rowMapper, departmentRows(3L, 4L));
                }
                if (sql.contains("ORDER BY id ASC")) {
                    return mapRows(rowMapper, departmentRows(1L, 2L));
                }
                return mapRows(rowMapper, departmentRows(1L, 2L, 3L, 4L));
            }
            if (sql.contains("FROM users WHERE department_id IN")) {
                return mapRows(rowMapper, userRows());
            }
            if (sql.contains("FROM department_cards WHERE department_id IN")) {
                return mapRows(rowMapper, departmentCardRows());
            }
            if (sql.contains("FROM profiles WHERE id IN")) {
                return mapRows(rowMapper, profileRows());
            }
            if (sql.contains("JOIN positions p")) {
                return mapRows(rowMapper, managerRows());
            }

            throw new AssertionError("Unexpected SQL: " + sql);
        }).when(jdbcTemplate).query(
            anyString(),
            ArgumentMatchers.any(MapSqlParameterSource.class),
            ArgumentMatchers.any(RowMapper.class)
        );
        return jdbcTemplate;
    }

    private RelationRegistry registry() {
        RelationRegistry registry = new RelationRegistry();
        registry.forClass(Department.class)
            .table("departments")
            .primaryKey("id")
            .hasOne("card", DepartmentCard.class, Department::getId, DepartmentCard::getDepartmentId, Department::setCard)
            .hasMany("users", User.class, Department::getId, User::getDepartmentId, Department::setUsers)
            .hasOneThroughPivot("manager", User.class, "positions", "department_id", "user_id", "role_code", "MANAGER", Department::setManager)
            .register();
        registry.forClass(DepartmentCard.class)
            .table("department_cards")
            .primaryKey("id")
            .register();
        registry.forClass(User.class)
            .table("users")
            .primaryKey("id")
            .belongsTo("profile", Profile.class, User::getProfileId, Profile::getId, User::setProfile)
            .register();
        registry.forClass(Profile.class)
            .table("profiles")
            .primaryKey("id")
            .register();
        return registry;
    }

    private List<Map<String, Object>> departmentRows(Long... ids) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Long id : ids) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", id);
            row.put("name", "department-" + id);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> userRows() {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        Map<String, Object> first = new LinkedHashMap<String, Object>();
        first.put("id", 10L);
        first.put("name", "alice");
        first.put("department_id", 1L);
        first.put("profile_id", 100L);
        rows.add(first);

        Map<String, Object> second = new LinkedHashMap<String, Object>();
        second.put("id", 11L);
        second.put("name", "bob");
        second.put("department_id", 1L);
        second.put("profile_id", null);
        rows.add(second);

        return rows;
    }

    private List<Map<String, Object>> departmentCardRows() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 500L);
        row.put("department_id", 1L);
        row.put("label", "engineering");
        return Collections.<Map<String, Object>>singletonList(row);
    }

    private List<Map<String, Object>> profileRows() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 100L);
        row.put("bio", "engineering profile");
        return Collections.<Map<String, Object>>singletonList(row);
    }

    private List<Map<String, Object>> managerRows() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", 99L);
        row.put("name", "manager");
        row.put("department_id", null);
        row.put("profile_id", null);
        row.put("__relation_parent_id", 1L);
        return Collections.<Map<String, Object>>singletonList(row);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> mapRows(RowMapper<?> rowMapper, List<Map<String, Object>> rows) throws SQLException {
        List<T> mapped = new ArrayList<T>();
        int rowNum = 0;
        for (Map<String, Object> row : rows) {
            ResultSet resultSet = resultSetFor(row);
            mapped.add((T) rowMapper.mapRow(resultSet, rowNum++));
        }
        return mapped;
    }

    private ResultSet resultSetFor(Map<String, Object> row) throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetMetaData metaData = Mockito.mock(ResultSetMetaData.class);
        List<String> columns = new ArrayList<String>(row.keySet());

        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            int columnIndex = i + 1;
            String column = columns.get(i);
            when(metaData.getColumnLabel(columnIndex)).thenReturn(column);
            when(metaData.getColumnName(columnIndex)).thenReturn(column);
        }

        doAnswer(invocation -> valueByIndex(row, columns, (Integer) invocation.getArgument(0))).when(resultSet).getObject(ArgumentMatchers.anyInt());
        doAnswer(invocation -> valueByLabel(row, (String) invocation.getArgument(0))).when(resultSet).getObject(ArgumentMatchers.anyString());
        doAnswer(invocation -> asLong(valueByIndex(row, columns, (Integer) invocation.getArgument(0)))).when(resultSet).getLong(ArgumentMatchers.anyInt());
        doAnswer(invocation -> asLong(valueByLabel(row, (String) invocation.getArgument(0)))).when(resultSet).getLong(ArgumentMatchers.anyString());
        doAnswer(invocation -> asString(valueByIndex(row, columns, (Integer) invocation.getArgument(0)))).when(resultSet).getString(ArgumentMatchers.anyInt());
        doAnswer(invocation -> asString(valueByLabel(row, (String) invocation.getArgument(0)))).when(resultSet).getString(ArgumentMatchers.anyString());
        doAnswer(invocation -> asTimestamp(valueByIndex(row, columns, (Integer) invocation.getArgument(0)))).when(resultSet).getTimestamp(ArgumentMatchers.anyInt());
        doAnswer(invocation -> asTimestamp(valueByLabel(row, (String) invocation.getArgument(0)))).when(resultSet).getTimestamp(ArgumentMatchers.anyString());
        when(resultSet.wasNull()).thenReturn(false);
        return resultSet;
    }

    private Object valueByIndex(Map<String, Object> row, List<String> columns, int index) {
        if (index < 1 || index > columns.size()) {
            return null;
        }
        return row.get(columns.get(index - 1));
    }

    private Object valueByLabel(Map<String, Object> row, String label) {
        return row.get(label);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Timestamp asTimestamp(Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return null;
    }

    private String containsSql(String expected) {
        return ArgumentMatchers.contains(expected);
    }

    public static final class Department {
        private Long id;
        private String name;
        private DepartmentCard card;
        private List<User> users = new ArrayList<User>();
        private User manager;

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

        public DepartmentCard getCard() {
            return card;
        }

        public void setCard(DepartmentCard card) {
            this.card = card;
        }

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }

        public User getManager() {
            return manager;
        }

        public void setManager(User manager) {
            this.manager = manager;
        }
    }

    public static final class DepartmentCard {
        private Long id;
        private Long departmentId;
        private String label;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static final class User {
        private Long id;
        private String name;
        private Long departmentId;
        private Long profileId;
        private Profile profile;

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

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
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
}

