package io.github.phuonghuu.eloquent.meta;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class RelationMeta {

    private final String name;
    private final RelationKind kind;
    private final Class<?> relatedType;
    private final String localKeyProperty;
    private final String relatedKeyProperty;
    private final String pivotTable;
    private final String pivotParentKey;
    private final String pivotRelatedKey;
    private final String pivotWhereColumn;
    private final Object pivotWhereValue;
    private final BiConsumer<Object, Object> setter;
    private final boolean collection;

    RelationMeta(
        String name,
        RelationKind kind,
        Class<?> relatedType,
        String localKeyProperty,
        String relatedKeyProperty,
        String pivotTable,
        String pivotParentKey,
        String pivotRelatedKey,
        String pivotWhereColumn,
        Object pivotWhereValue,
        BiConsumer<Object, Object> setter,
        boolean collection
    ) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.kind = Objects.requireNonNull(kind, "kind is required");
        this.relatedType = Objects.requireNonNull(relatedType, "relatedType is required");
        this.localKeyProperty = localKeyProperty;
        this.relatedKeyProperty = relatedKeyProperty;
        this.pivotTable = pivotTable;
        this.pivotParentKey = pivotParentKey;
        this.pivotRelatedKey = pivotRelatedKey;
        this.pivotWhereColumn = pivotWhereColumn;
        this.pivotWhereValue = pivotWhereValue;
        this.setter = Objects.requireNonNull(setter, "setter is required");
        this.collection = collection;
    }

    public String name() {
        return name;
    }

    public RelationKind kind() {
        return kind;
    }

    public Class<?> relatedType() {
        return relatedType;
    }

    public String localKeyProperty() {
        return localKeyProperty;
    }

    public String relatedKeyProperty() {
        return relatedKeyProperty;
    }

    public String pivotTable() {
        return pivotTable;
    }

    public String pivotParentKey() {
        return pivotParentKey;
    }

    public String pivotRelatedKey() {
        return pivotRelatedKey;
    }

    public String pivotWhereColumn() {
        return pivotWhereColumn;
    }

    public Object pivotWhereValue() {
        return pivotWhereValue;
    }

    public boolean collection() {
        return collection;
    }

    @SuppressWarnings("unchecked")
    public <P> void attach(P parent, Object value) {
        setter.accept(parent, value);
    }
}

