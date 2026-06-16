package io.github.phuonghuu.eloquent.query;

import io.github.phuonghuu.eloquent.support.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WithNode {

    private final String name;
    private final Map<String, WithNode> children = new LinkedHashMap<>();
    private final List<String> selectColumns = new ArrayList<>();
    private final List<PredicateClause> predicates = new ArrayList<>();
    private final List<OrderClause> orderBy = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private Integer perParentLimit;

    WithNode(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public String name() {
        return name;
    }

    public Collection<WithNode> children() {
        return children.values();
    }

    public List<String> selectColumns() {
        return Collections.unmodifiableList(new ArrayList<String>(selectColumns));
    }

    public List<OrderClause> orderBy() {
        return Collections.unmodifiableList(new ArrayList<OrderClause>(orderBy));
    }

    public List<PredicateClause> predicates() {
        return Collections.unmodifiableList(new ArrayList<PredicateClause>(predicates));
    }

    public Integer limit() {
        return limit;
    }

    public Integer offset() {
        return offset;
    }

    public Integer perParentLimit() {
        return perParentLimit;
    }

    public boolean hasProjection() {
        return !selectColumns.isEmpty();
    }

    public WithNode child(String name) {
        return children.computeIfAbsent(name, WithNode::new);
    }

    public void select(Collection<String> columns) {
        if (columns == null) {
            return;
        }
        for (String column : columns) {
            if (!TextUtils.isBlank(column) && !selectColumns.contains(column)) {
                selectColumns.add(column);
            }
        }
    }

    public void where(FilterCondition condition) {
        addPredicate("AND", condition);
    }

    public void orWhere(FilterCondition condition) {
        addPredicate("OR", condition);
    }

    public void whereRaw(String expression, List<?> values) {
        addPredicate("AND", FilterCondition.raw(expression, values));
    }

    public void orWhereRaw(String expression, List<?> values) {
        addPredicate("OR", FilterCondition.raw(expression, values));
    }

    public void whereNull(String column) {
        addPredicate("AND", FilterCondition.isNull(column));
    }

    public void orWhereNull(String column) {
        addPredicate("OR", FilterCondition.isNull(column));
    }

    public void whereNotNull(String column) {
        addPredicate("AND", FilterCondition.isNotNull(column));
    }

    public void orWhereNotNull(String column) {
        addPredicate("OR", FilterCondition.isNotNull(column));
    }

    public void whereBetween(String column, Object start, Object end) {
        addPredicate("AND", FilterCondition.between(column, start, end));
    }

    public void orWhereBetween(String column, Object start, Object end) {
        addPredicate("OR", FilterCondition.between(column, start, end));
    }

    public void whereLike(String column, Object value) {
        addPredicate("AND", FilterCondition.like(column, value));
    }

    public void orWhereLike(String column, Object value) {
        addPredicate("OR", FilterCondition.like(column, value));
    }

    public void whereDate(String column, Object value) {
        addPredicate("AND", FilterCondition.date(column, value));
    }

    public void orWhereDate(String column, Object value) {
        addPredicate("OR", FilterCondition.date(column, value));
    }

    public void whereJson(String column, Object value) {
        addPredicate("AND", FilterCondition.jsonContains(column, value));
    }

    public void orWhereJson(String column, Object value) {
        addPredicate("OR", FilterCondition.jsonContains(column, value));
    }

    public void orderBy(String column, String direction) {
        if (!TextUtils.isBlank(column)) {
            orderBy.add(new OrderClause(column, direction == null ? "ASC" : direction));
        }
    }

    public void limit(Integer limit) {
        this.limit = limit;
    }

    public void offset(Integer offset) {
        this.offset = offset;
    }

    public void limitPerParent(Integer limit) {
        this.perParentLimit = limit;
    }

    void addPredicate(String connector, QueryPredicate predicate) {
        if (predicate != null) {
            predicates.add(new PredicateClause(connector, predicate));
        }
    }

    public void absorb(WithNode other) {
        select(other.selectColumns);
        this.predicates.addAll(other.predicates);
        this.orderBy.addAll(other.orderBy);
        if (other.limit != null) {
            this.limit = other.limit;
        }
        if (other.offset != null) {
            this.offset = other.offset;
        }
        if (other.perParentLimit != null) {
            this.perParentLimit = other.perParentLimit;
        }
        for (WithNode child : other.children.values()) {
            this.children.put(child.name(), child);
        }
    }

    void addPath(String[] path, int index) {
        if (index >= path.length) {
            return;
        }
        String segment = path[index];
        WithNode child = children.computeIfAbsent(segment, WithNode::new);
        child.addPath(path, index + 1);
    }

    public static final class OrderClause {
        private final String column;
        private final String direction;

        public OrderClause(String column, String direction) {
            this.column = column;
            this.direction = direction;
        }

        public String column() {
            return column;
        }

        public String direction() {
            return direction;
        }
    }

    public static final class PredicateClause {
        private final String connector;
        private final QueryPredicate predicate;

        public PredicateClause(String connector, QueryPredicate predicate) {
            this.connector = connector;
            this.predicate = predicate;
        }

        public String connector() {
            return connector;
        }

        public QueryPredicate predicate() {
            return predicate;
        }
    }
}

