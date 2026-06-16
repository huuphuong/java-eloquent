package io.github.huuphuong.eloquent.query;

import io.github.huuphuong.eloquent.api.CursorPaginatedResult;
import io.github.huuphuong.eloquent.api.PaginatedResult;
import io.github.huuphuong.eloquent.loader.EagerLoader;
import io.github.huuphuong.eloquent.meta.RelationKind;
import io.github.huuphuong.eloquent.meta.RelationMeta;
import io.github.huuphuong.eloquent.meta.RelationRegistry;
import io.github.huuphuong.eloquent.support.RowMapperFactory;
import io.github.huuphuong.eloquent.support.TextUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class QueryBuilder<T> implements Query<T> {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RelationRegistry registry;
    private final Class<T> type;
    private final boolean configurationOnly;

    private final List<String> selectColumns = new ArrayList<>();
    private final List<PredicateClause> predicates = new ArrayList<>();
    private final List<RelationFilter> relationFilters = new ArrayList<>();
    private final List<WithRequest> withRequests = new ArrayList<>();
    private final List<OrderClause> orderBy = new ArrayList<>();

    private Integer page;
    private Integer size;
    private Integer limit;
    private Integer offset;
    private Integer perParentLimit;
    private boolean alwaysEmpty;

    public QueryBuilder(NamedParameterJdbcTemplate jdbcTemplate, RelationRegistry registry, Class<T> type) {
        this(jdbcTemplate, registry, type, false);
    }

    public QueryBuilder(NamedParameterJdbcTemplate jdbcTemplate, RelationRegistry registry, Class<T> type, boolean configurationOnly) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
        this.registry = Objects.requireNonNull(registry, "registry is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.configurationOnly = configurationOnly;
    }

    public QueryBuilder<T> select(String... columns) {
        if (columns != null) {
            for (String column : columns) {
                if (!TextUtils.isBlank(column) && !selectColumns.contains(column)) {
                    selectColumns.add(column);
                }
            }
        }
        return this;
    }

    public QueryBuilder<T> where(String column, Object value) {
        if (value == null) {
            addPredicate("AND", FilterCondition.isNull(column));
        } else {
            addPredicate("AND", FilterCondition.equals(column, value));
        }
        return this;
    }

    public QueryBuilder<T> where(String column, String operator, Object value) {
        if (value == null && ("IS NULL".equalsIgnoreCase(operator) || "IS NOT NULL".equalsIgnoreCase(operator))) {
            addPredicate("AND", FilterCondition.operator(column, operator, null));
        } else if (value == null && (operator == null || "=".equals(operator.trim()))) {
            addPredicate("AND", FilterCondition.isNull(column));
        } else {
            addPredicate("AND", FilterCondition.operator(column, operator, value));
        }
        return this;
    }

    public QueryBuilder<T> orWhere(String column, Object value) {
        if (value == null) {
            addPredicate("OR", FilterCondition.isNull(column));
        } else {
            addPredicate("OR", FilterCondition.equals(column, value));
        }
        return this;
    }

    public QueryBuilder<T> orWhere(String column, String operator, Object value) {
        if (value == null && ("IS NULL".equalsIgnoreCase(operator) || "IS NOT NULL".equalsIgnoreCase(operator))) {
            addPredicate("OR", FilterCondition.operator(column, operator, null));
        } else if (value == null && (operator == null || "=".equals(operator.trim()))) {
            addPredicate("OR", FilterCondition.isNull(column));
        } else {
            addPredicate("OR", FilterCondition.operator(column, operator, value));
        }
        return this;
    }

    public QueryBuilder<T> whereRaw(String expression, List<?> values) {
        addPredicate("AND", FilterCondition.raw(expression, values));
        return this;
    }

    public QueryBuilder<T> orWhereRaw(String expression, List<?> values) {
        addPredicate("OR", FilterCondition.raw(expression, values));
        return this;
    }

    public QueryBuilder<T> whereNull(String column) {
        addPredicate("AND", FilterCondition.isNull(column));
        return this;
    }

    public QueryBuilder<T> orWhereNull(String column) {
        addPredicate("OR", FilterCondition.isNull(column));
        return this;
    }

    public QueryBuilder<T> whereNotNull(String column) {
        addPredicate("AND", FilterCondition.isNotNull(column));
        return this;
    }

    public QueryBuilder<T> orWhereNotNull(String column) {
        addPredicate("OR", FilterCondition.isNotNull(column));
        return this;
    }

    public QueryBuilder<T> whereBetween(String column, Object start, Object end) {
        addPredicate("AND", FilterCondition.between(column, start, end));
        return this;
    }

    public QueryBuilder<T> orWhereBetween(String column, Object start, Object end) {
        addPredicate("OR", FilterCondition.between(column, start, end));
        return this;
    }

    public QueryBuilder<T> whereLike(String column, Object value) {
        addPredicate("AND", FilterCondition.like(column, value));
        return this;
    }

    public QueryBuilder<T> orWhereLike(String column, Object value) {
        addPredicate("OR", FilterCondition.like(column, value));
        return this;
    }

    public QueryBuilder<T> whereDate(String column, Object value) {
        addPredicate("AND", FilterCondition.date(column, value));
        return this;
    }

    public QueryBuilder<T> orWhereDate(String column, Object value) {
        addPredicate("OR", FilterCondition.date(column, value));
        return this;
    }

    public QueryBuilder<T> whereJson(String column, Object value) {
        addPredicate("AND", FilterCondition.jsonContains(column, value));
        return this;
    }

    public QueryBuilder<T> orWhereJson(String column, Object value) {
        addPredicate("OR", FilterCondition.jsonContains(column, value));
        return this;
    }

    public QueryBuilder<T> whereIn(String column, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            alwaysEmpty = true;
            return this;
        }
        addPredicate("AND", FilterCondition.in(column, new ArrayList<>(values)));
        return this;
    }

    public QueryBuilder<T> whereHas(String relationName, Consumer<QueryBuilder<?>> callback) {
        relationFilters.add(new RelationFilter(relationName, callback));
        return this;
    }

    public QueryBuilder<T> whereHasIn(String relationName, String column, Collection<?> values) {
        return whereHas(relationName, query -> query.whereIn(column, values));
    }

    public QueryBuilder<T> with(String path) {
        return with(path, null);
    }

    public QueryBuilder<T> with(String path, Consumer<QueryBuilder<?>> callback) {
        if (!TextUtils.isBlank(path)) {
            withRequests.add(new WithRequest(path, callback));
        }
        return this;
    }

    public QueryBuilder<T> page(int page) {
        this.page = page;
        return this;
    }

    public QueryBuilder<T> size(int size) {
        this.size = size;
        return this;
    }

    public QueryBuilder<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder<T> limitPerParent(int limit) {
        this.perParentLimit = limit;
        return this;
    }

    public QueryBuilder<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    public QueryBuilder<T> orderBy(String column) {
        return orderBy(column, false);
    }

    public QueryBuilder<T> orderByDesc(String column) {
        return orderBy(column, true);
    }

    public QueryBuilder<T> orderBy(String column, boolean descending) {
        return orderBy(column, descending ? "DESC" : "ASC");
    }

    public QueryBuilder<T> orderBy(String column, String direction) {
        if (!TextUtils.isBlank(column)) {
            orderBy.add(new OrderClause(column, normalizeDirection(direction)));
        }
        return this;
    }

    SqlStatement toSqlStatement() {
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (alwaysEmpty) {
            return new SqlStatement("SELECT * FROM " + meta.table() + " WHERE 1 = 0", params);
        }

        AtomicInteger index = new AtomicInteger(0);
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(SqlGenerator.buildSelectClause(null, selectColumns, Collections.singletonList(meta.primaryKeyProperty())));
        sql.append(" FROM ").append(meta.table());

        String predicate = buildPredicateSql(meta, params, index, null);
        if (!TextUtils.isBlank(predicate)) {
            sql.append(" WHERE ").append(predicate);
        }

        appendOrderBy(sql);
        appendPaging(sql);
        return new SqlStatement(sql.toString(), params);
    }

    public String toSql() {
        return toSqlStatement().sql();
    }

    public String toDebugSql() {
        return toSqlStatement().toDebugSql();
    }

    public List<String> toSqls() {
        return collectPreviewSqls(false);
    }

    public List<String> toDebugSqls() {
        return collectPreviewSqls(true);
    }

    @Override
    public List<T> get() {
        if (configurationOnly) {
            throw new IllegalStateException("This QueryBuilder is configured only for relation callbacks");
        }
        if (alwaysEmpty) {
            return Collections.emptyList();
        }

        SqlStatement statement = toSqlStatement();
        List<T> result = jdbcTemplate.query(statement.sql(), statement.parameters(), RowMapperFactory.create(type));

        if (!withRequests.isEmpty() && !result.isEmpty()) {
            WithNode withTree = buildWithTree(registry.require(type));
            EagerLoader.load(result, type, withTree, registry, jdbcTemplate);
        }

        return result;
    }

    @Override
    public T first() {
        return executeSingle(copy -> copy.limit(1));
    }

    @Override
    public T find(Object id) {
        if (id == null) {
            return null;
        }
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        return executeSingle(copy -> copy.where(meta.primaryKeyProperty(), id));
    }

    public PaginatedResult<T> paginate() {
        if (page == null || size == null) {
            throw new IllegalStateException("paginate requires page() and size() or paginate(page, size)");
        }
        return paginate(page, size);
    }

    public PaginatedResult<T> paginate(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        QueryBuilder<T> copy = snapshot();
        copy.page = page;
        copy.size = size;
        copy.limit = null;
        copy.offset = null;
        long total = count(copy);
        List<T> items = copy.get();
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        return new PaginatedResult<>(
            items,
            total,
            page,
            size,
            totalPages,
            page < totalPages,
            page > 1
        );
    }

    public CursorPaginatedResult<T> cursorPaginate(Object lastId) {
        if (size == null) {
            throw new IllegalStateException("cursorPaginate requires size() or cursorPaginate(lastId, size)");
        }
        return cursorPaginate(lastId, size);
    }

    public CursorPaginatedResult<T> cursorPaginate(Object lastId, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        CursorOrdering cursorOrdering = resolveCursorOrdering(meta);
        QueryBuilder<T> copy = snapshot();
        copy.page = null;
        copy.size = null;
        copy.limit = size + 1;
        copy.offset = null;
        copy.orderBy.clear();
        copy.orderBy.add(new OrderClause(cursorOrdering.column(), cursorOrdering.direction()));
        if (lastId != null) {
            copy.where(meta.primaryKeyProperty(), cursorOrdering.direction().equals("DESC") ? "<" : ">", lastId);
        }

        List<T> items = copy.get();
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = new ArrayList<>(items.subList(0, size));
        }

        Object nextCursorId = null;
        if (!items.isEmpty()) {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(items.get(items.size() - 1));
            nextCursorId = wrapper.getPropertyValue(meta.primaryKeyProperty());
        }

        return new CursorPaginatedResult<>(items, nextCursorId, size, hasNext);
    }

    private CursorOrdering resolveCursorOrdering(RelationRegistry.EntityMeta<T> meta) {
        if (orderBy.isEmpty()) {
            return new CursorOrdering(meta.primaryKeyProperty(), "ASC");
        }
        if (orderBy.size() != 1) {
            throw new IllegalStateException("cursorPaginate supports only one orderBy column");
        }

        OrderClause clause = orderBy.get(0);
        if (!meta.primaryKeyProperty().equals(clause.column())) {
            throw new IllegalStateException(
                "cursorPaginate currently supports only ordering by the primary key '" + meta.primaryKeyProperty() + "'"
            );
        }
        return new CursorOrdering(clause.column(), clause.direction());
    }

    private T executeSingle(Consumer<QueryBuilder<T>> customizer) {
        if (configurationOnly) {
            throw new IllegalStateException("This QueryBuilder is configured only for relation callbacks");
        }
        if (alwaysEmpty) {
            return null;
        }

        QueryBuilder<T> copy = snapshot();
        copy.page = null;
        copy.size = null;
        copy.limit = null;
        copy.offset = null;
        if (customizer != null) {
            customizer.accept(copy);
        }
        List<T> result = copy.get();
        return result.isEmpty() ? null : result.get(0);
    }

    private long count(QueryBuilder<T> source) {
        if (source.alwaysEmpty) {
            return 0L;
        }
        RelationRegistry.EntityMeta<T> meta = registry.require(type);
        MapSqlParameterSource params = new MapSqlParameterSource();
        AtomicInteger index = new AtomicInteger(0);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(meta.table());

        String predicate = source.buildPredicateSql(meta, params, index, null);
        if (!TextUtils.isBlank(predicate)) {
            sql.append(" WHERE ").append(predicate);
        }

        Number total = jdbcTemplate.queryForObject(sql.toString(), params, Number.class);
        return total == null ? 0L : total.longValue();
    }

    private QueryBuilder<T> snapshot() {
        QueryBuilder<T> copy = new QueryBuilder<>(jdbcTemplate, registry, type);
        copy.selectColumns.addAll(this.selectColumns);
        copy.predicates.addAll(this.predicates);
        copy.relationFilters.addAll(this.relationFilters);
        copy.withRequests.addAll(this.withRequests);
        copy.orderBy.addAll(this.orderBy);
        copy.page = this.page;
        copy.size = this.size;
        copy.limit = this.limit;
        copy.offset = this.offset;
        copy.perParentLimit = this.perParentLimit;
        copy.alwaysEmpty = this.alwaysEmpty;
        return copy;
    }

    private WithNode buildWithTree(RelationRegistry.EntityMeta<?> parentMeta) {
        WithNode root = new WithNode("__root__");
        for (WithRequest request : withRequests) {
            attachWithRequest(root, parentMeta, request, request.path().split("\\."), 0);
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private List<String> collectPreviewSqls(boolean debug) {
        List<String> sqls = new ArrayList<>();
        SqlStatement root = toSqlStatement();
        sqls.add(debug ? compactSql(root.toDebugSql()) : root.sql());

        if (!withRequests.isEmpty()) {
            WithNode withTree = buildWithTree(registry.require(type));
            collectPreviewSqls(registry.require(type), withTree, sqls, debug);
        }

        return sqls;
    }

    @SuppressWarnings("unchecked")
    private void collectPreviewSqls(
        RelationRegistry.EntityMeta<?> parentMeta,
        WithNode node,
        List<String> sqls,
        boolean debug
    ) {
        for (WithNode childNode : node.children()) {
            RelationMeta relationMeta = parentMeta.relation(childNode.name());
            SqlStatement statement = buildRelationPreviewStatement(parentMeta, relationMeta, childNode);
            sqls.add(debug ? compactSql(statement.toDebugSql()) : statement.sql());
            collectPreviewSqls(
                registry.require((Class<Object>) relationMeta.relatedType()),
                childNode,
                sqls,
                debug
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void attachWithRequest(
        WithNode currentNode,
        RelationRegistry.EntityMeta<?> currentMeta,
        WithRequest request,
        String[] segments,
        int index
    ) {
        String segment = segments[index];
        RelationMeta relationMeta = currentMeta.relation(segment);
        WithNode childNode = currentNode.child(segment);

        if (index == segments.length - 1) {
            if (request.callback() != null) {
                QueryBuilder<?> childBuilder = new QueryBuilder<>(jdbcTemplate, registry, (Class<Object>) relationMeta.relatedType(), true);
                request.callback().accept(childBuilder);
                childNode.absorb(childBuilder.exportRelationNode(registry.require((Class<Object>) relationMeta.relatedType())));
            }
            return;
        }

        attachWithRequest(
            childNode,
            registry.require((Class<Object>) relationMeta.relatedType()),
            request,
            segments,
            index + 1
        );
    }

    private WithNode exportRelationNode(RelationRegistry.EntityMeta<?> meta) {
        WithNode root = new WithNode("__root__");
        root.select(selectColumns);
        root.limit(limit);
        root.offset(offset);
        for (PredicateClause predicate : predicates) {
            if (predicate.predicate() instanceof FilterCondition) {
                FilterCondition condition = (FilterCondition) predicate.predicate();
                if ("OR".equalsIgnoreCase(predicate.connector())) {
                    root.orWhere(condition);
                } else {
                    root.where(condition);
                }
            } else {
                root.addPredicate(predicate.connector(), predicate.predicate());
            }
        }
        for (OrderClause clause : orderBy) {
            root.orderBy(clause.column(), clause.direction());
        }
        root.limitPerParent(perParentLimit);
        for (WithRequest request : withRequests) {
            attachWithRequest(root, meta, request, request.path().split("\\."), 0);
        }
        return root;
    }

    private SqlStatement buildRelationPreviewStatement(
        RelationRegistry.EntityMeta<?> parentMeta,
        RelationMeta relationMeta,
        WithNode node
    ) {
        SqlStatement statement;
        switch (relationMeta.kind()) {
            case HAS_ONE:
            case HAS_MANY:
                statement = SqlGenerator.buildRelatedSelectFromParent(
                    registry.require((Class<Object>) relationMeta.relatedType()),
                    relationMeta,
                    parentMeta,
                    Collections.emptyList(),
                    node.selectColumns()
                );
                break;
            case BELONGS_TO:
                statement = SqlGenerator.buildBelongsToSelect(
                    registry.require((Class<Object>) relationMeta.relatedType()),
                    relationMeta,
                    Collections.emptyList(),
                    node.selectColumns()
                );
                break;
            case BELONGS_TO_MANY:
                statement = SqlGenerator.buildBelongsToManySelect(
                    registry.require((Class<Object>) relationMeta.relatedType()),
                    relationMeta,
                    parentMeta,
                    Collections.emptyList(),
                    "__relation_parent_id",
                    node.selectColumns()
                );
                break;
            case HAS_ONE_THROUGH_PIVOT:
                statement = SqlGenerator.buildHasOneThroughPivotSelect(
                    registry.require((Class<Object>) relationMeta.relatedType()),
                    relationMeta,
                    parentMeta,
                    Collections.emptyList(),
                    "__relation_parent_id",
                    node.selectColumns()
                );
                break;
            default:
                throw new IllegalStateException("Unsupported relation kind: " + relationMeta.kind());
        }

        String sql = statement.sql();
        String childAlias = (relationMeta.kind() == RelationKind.BELONGS_TO_MANY || relationMeta.kind() == RelationKind.HAS_ONE_THROUGH_PIVOT) ? "t" : "child";
        String childPredicate = EagerLoader.buildNodePredicate(node, statement.parameters(), new AtomicInteger(1000), childAlias);
        if (!TextUtils.isBlank(childPredicate)) {
            sql += " AND " + childPredicate;
        }
        if (node.perParentLimit() != null) {
            if (relationMeta.kind() != RelationKind.BELONGS_TO_MANY) {
                throw new IllegalStateException("limitPerParent is currently supported only for belongsToMany relations");
            }
            String defaultOrderColumn = "base." + SqlGenerator.toColumnName(
                registry.require((Class<Object>) relationMeta.relatedType()).primaryKeyProperty()
            );
            String orderClause = SqlGenerator.buildWindowOrderClause(node.orderBy(), "base", defaultOrderColumn);
            return SqlGenerator.wrapWithPerParentLimit(
                new SqlStatement(sql, statement.parameters()),
                "__relation_parent_id",
                orderClause,
                node.perParentLimit()
            );
        }
        return new SqlStatement(EagerLoader.applyRelationModifiers(sql, node), statement.parameters());
    }

    private String buildPredicateSql(
        RelationRegistry.EntityMeta<?> meta,
        MapSqlParameterSource params,
        AtomicInteger index,
        String tableAlias
    ) {
        List<String> fragments = new ArrayList<>();
        for (int i = 0; i < predicates.size(); i++) {
            PredicateClause clause = predicates.get(i);
            String fragment = clause.predicate().toSql(meta, params, index, tableAlias);
            if (TextUtils.isBlank(fragment)) {
                continue;
            }
            if (fragments.isEmpty()) {
                fragments.add(fragment);
            } else {
                fragments.add(clause.connector() + " " + fragment);
            }
        }
        for (RelationFilter relationFilter : relationFilters) {
            String fragment = relationFilter.toSql(meta, params, index, tableAlias);
            if (TextUtils.isBlank(fragment)) {
                continue;
            }
            if (fragments.isEmpty()) {
                fragments.add(fragment);
            } else {
                fragments.add("AND " + fragment);
            }
        }
        return String.join(" ", fragments);
    }

    private void appendOrderBy(StringBuilder sql) {
        if (orderBy.isEmpty()) {
            return;
        }
        List<String> fragments = new ArrayList<>();
        for (OrderClause clause : orderBy) {
            fragments.add(SqlGenerator.toColumnName(clause.column()) + " " + clause.direction());
        }
        sql.append(" ORDER BY ").append(String.join(", ", fragments));
    }

    private void appendPaging(StringBuilder sql) {
        Integer effectiveLimit = limit;
        Integer effectiveOffset = offset;

        if (effectiveLimit == null && size != null && size > 0) {
            effectiveLimit = size;
            int safePage = page == null || page < 1 ? 1 : page;
            if (effectiveOffset == null) {
                effectiveOffset = (safePage - 1) * size;
            }
        }

        if (effectiveLimit != null) {
            sql.append(" LIMIT ").append(effectiveLimit);
        }
        if (effectiveOffset != null) {
            sql.append(" OFFSET ").append(effectiveOffset);
        }
    }

    private static final class OrderClause {
        private final String column;
        private final String direction;

        private OrderClause(String column, String direction) {
            this.column = column;
            this.direction = direction;
        }

        private String column() {
            return column;
        }

        private String direction() {
            return direction;
        }
    }

    private static final class CursorOrdering {
        private final String column;
        private final String direction;

        private CursorOrdering(String column, String direction) {
            this.column = column;
            this.direction = direction;
        }

        private String column() {
            return column;
        }

        private String direction() {
            return direction;
        }
    }

    private static final class WithRequest {
        private final String path;
        private final Consumer<QueryBuilder<?>> callback;

        private WithRequest(String path, Consumer<QueryBuilder<?>> callback) {
            this.path = path;
            this.callback = callback;
        }

        private String path() {
            return path;
        }

        private Consumer<QueryBuilder<?>> callback() {
            return callback;
        }
    }

    private static final class PredicateClause {
        private final String connector;
        private final QueryPredicate predicate;

        private PredicateClause(String connector, QueryPredicate predicate) {
            this.connector = connector;
            this.predicate = predicate;
        }

        private String connector() {
            return connector;
        }

        private QueryPredicate predicate() {
            return predicate;
        }
    }

    private void addPredicate(String connector, QueryPredicate predicate) {
        if (predicate != null) {
            predicates.add(new PredicateClause(connector, predicate));
        }
    }

    private final class RelationFilter implements QueryPredicate {

        private final String relationName;
        private final Consumer<QueryBuilder<?>> callback;

        private RelationFilter(String relationName, Consumer<QueryBuilder<?>> callback) {
            this.relationName = relationName;
            this.callback = callback;
        }

        @Override
        public String toSql(
            RelationRegistry.EntityMeta<?> parentMeta,
            MapSqlParameterSource params,
            AtomicInteger index,
            String tableAlias
        ) {
            RelationMeta relationMeta = parentMeta.relation(relationName);
            QueryBuilder<?> childBuilder = new QueryBuilder<>(jdbcTemplate, registry, relationMeta.relatedType());
            if (callback != null) {
                callback.accept(childBuilder);
            }

            String childAlias = relationMeta.kind() == RelationKind.BELONGS_TO_MANY ? "target" : "child";
            String childPredicate = childBuilder.buildPredicateSql(
                registry.require(relationMeta.relatedType()),
                params,
                index,
                childAlias
            );
            String childWhere = TextUtils.isBlank(childPredicate) ? "" : " AND " + childPredicate;

            String parentTable = tableAlias == null ? parentMeta.table() : tableAlias;
            if (relationMeta.kind() == RelationKind.HAS_ONE || relationMeta.kind() == RelationKind.HAS_MANY) {
                RelationRegistry.EntityMeta<?> childMeta = registry.require(relationMeta.relatedType());
                return "EXISTS (SELECT 1 FROM " + childMeta.table() + " child WHERE child."
                    + SqlGenerator.toColumnName(relationMeta.relatedKeyProperty()) + " = "
                    + parentTable + "." + SqlGenerator.toColumnName(parentMeta.primaryKeyProperty())
                    + childWhere + ")";
            }
            if (relationMeta.kind() == RelationKind.BELONGS_TO) {
                RelationRegistry.EntityMeta<?> targetMeta = registry.require(relationMeta.relatedType());
                return "EXISTS (SELECT 1 FROM " + targetMeta.table() + " child WHERE child."
                    + SqlGenerator.toColumnName(relationMeta.relatedKeyProperty()) + " = "
                    + parentTable + "." + SqlGenerator.toColumnName(relationMeta.localKeyProperty())
                    + childWhere + ")";
            }
            if (relationMeta.kind() == RelationKind.BELONGS_TO_MANY) {
                RelationRegistry.EntityMeta<?> targetMeta = registry.require(relationMeta.relatedType());
                return "EXISTS (SELECT 1 FROM " + relationMeta.pivotTable() + " pivot "
                    + "JOIN " + targetMeta.table() + " target ON target."
                    + SqlGenerator.toColumnName(targetMeta.primaryKeyProperty()) + " = pivot."
                    + relationMeta.pivotRelatedKey()
                    + " WHERE pivot." + relationMeta.pivotParentKey() + " = "
                    + parentTable + "." + SqlGenerator.toColumnName(parentMeta.primaryKeyProperty())
                    + childWhere + ")";
            }
            if (relationMeta.kind() == RelationKind.HAS_ONE_THROUGH_PIVOT) {
                RelationRegistry.EntityMeta<?> targetMeta = registry.require(relationMeta.relatedType());
                String pivotFilter = TextUtils.isBlank(relationMeta.pivotWhereColumn())
                    ? ""
                    : " AND pivot." + SqlGenerator.toColumnName(relationMeta.pivotWhereColumn()) + " = " + formatLiteral(relationMeta.pivotWhereValue());
                return "EXISTS (SELECT 1 FROM " + relationMeta.pivotTable() + " pivot "
                    + "JOIN " + targetMeta.table() + " target ON target."
                    + SqlGenerator.toColumnName(targetMeta.primaryKeyProperty()) + " = pivot."
                    + relationMeta.pivotRelatedKey()
                    + " WHERE pivot." + relationMeta.pivotParentKey() + " = "
                    + parentTable + "." + SqlGenerator.toColumnName(parentMeta.primaryKeyProperty())
                    + pivotFilter
                    + childWhere + ")";
            }
            throw new IllegalStateException("Unsupported relation kind: " + relationMeta.kind());
        }
    }

    private static String formatLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String escaped = value.toString().replace("'", "''");
        return "'" + escaped + "'";
    }

    private static String compactSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeDirection(String direction) {
        if (TextUtils.isBlank(direction)) {
            return "ASC";
        }
        String normalized = direction.trim().toUpperCase();
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            throw new IllegalArgumentException("orderBy direction must be ASC or DESC");
        }
        return normalized;
    }
}

