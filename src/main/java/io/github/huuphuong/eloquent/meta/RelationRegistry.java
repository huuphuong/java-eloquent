package io.github.huuphuong.eloquent.meta;

import io.github.huuphuong.eloquent.support.TextUtils;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class RelationRegistry {

    private final Map<Class<?>, EntityMeta<?>> entities = new LinkedHashMap<>();

    public <T> EntityRegistration<T> forClass(Class<T> type) {
        return new EntityRegistration<>(this, type);
    }

    @SuppressWarnings("unchecked")
    public <T> EntityMeta<T> require(Class<T> type) {
        EntityMeta<?> meta = entities.get(type);
        if (meta == null) {
            throw new IllegalArgumentException("No relation metadata registered for " + type.getName());
        }
        return (EntityMeta<T>) meta;
    }

    private <T> void register(EntityMeta<T> meta) {
        entities.put(meta.type(), meta);
    }

    public static final class EntityRegistration<T> {

        private final RelationRegistry registry;
        private final EntityMeta<T> meta;

        EntityRegistration(RelationRegistry registry, Class<T> type) {
            this.registry = registry;
            this.meta = new EntityMeta<>(type);
        }

        public EntityRegistration<T> table(String table) {
            meta.table = Objects.requireNonNull(table, "table is required");
            return this;
        }

        public EntityRegistration<T> primaryKey(String primaryKeyProperty) {
            meta.primaryKeyProperty = Objects.requireNonNull(primaryKeyProperty, "primaryKeyProperty is required");
            return this;
        }

        public <C> EntityRegistration<T> hasOne(
            String name,
            Class<C> relatedType,
            SFunction<T, ?> localKeyGetter,
            SFunction<C, ?> relatedKeyGetter,
            BiConsumer<T, C> setter
        ) {
            meta.relations.put(
                name,
                new RelationMeta(
                    name,
                    RelationKind.HAS_ONE,
                    relatedType,
                    LambdaPropertyResolver.propertyName(localKeyGetter),
                    LambdaPropertyResolver.propertyName(relatedKeyGetter),
                    null,
                    null,
                    null,
                    null,
                    null,
                    castSetter(setter),
                    false
                )
            );
            return this;
        }

        public <C> EntityRegistration<T> hasMany(
            String name,
            Class<C> relatedType,
            SFunction<T, ?> localKeyGetter,
            SFunction<C, ?> relatedKeyGetter,
            BiConsumer<T, java.util.List<C>> setter
        ) {
            meta.relations.put(
                name,
                new RelationMeta(
                    name,
                    RelationKind.HAS_MANY,
                    relatedType,
                    LambdaPropertyResolver.propertyName(localKeyGetter),
                    LambdaPropertyResolver.propertyName(relatedKeyGetter),
                    null,
                    null,
                    null,
                    null,
                    null,
                    castSetter(setter),
                    true
                )
            );
            return this;
        }

        public <C> EntityRegistration<T> belongsTo(
            String name,
            Class<C> relatedType,
            SFunction<T, ?> foreignKeyGetter,
            SFunction<C, ?> relatedKeyGetter,
            BiConsumer<T, C> setter
        ) {
            meta.relations.put(
                name,
                new RelationMeta(
                    name,
                    RelationKind.BELONGS_TO,
                    relatedType,
                    LambdaPropertyResolver.propertyName(foreignKeyGetter),
                    LambdaPropertyResolver.propertyName(relatedKeyGetter),
                    null,
                    null,
                    null,
                    null,
                    null,
                    castSetter(setter),
                    false
                )
            );
            return this;
        }

        public <C> EntityRegistration<T> belongsToMany(
            String name,
            Class<C> relatedType,
            String pivotTable,
            String pivotParentKey,
            String pivotRelatedKey,
            BiConsumer<T, java.util.List<C>> setter
        ) {
            meta.relations.put(
                name,
                new RelationMeta(
                    name,
                    RelationKind.BELONGS_TO_MANY,
                    relatedType,
                    null,
                    null,
                    pivotTable,
                    pivotParentKey,
                    pivotRelatedKey,
                    null,
                    null,
                    castSetter(setter),
                    true
                )
            );
            return this;
        }

        public <C> EntityRegistration<T> hasOneThroughPivot(
            String name,
            Class<C> relatedType,
            String pivotTable,
            String pivotParentKey,
            String pivotRelatedKey,
            String pivotWhereColumn,
            Object pivotWhereValue,
            BiConsumer<T, C> setter
        ) {
            meta.relations.put(
                name,
                new RelationMeta(
                    name,
                    RelationKind.HAS_ONE_THROUGH_PIVOT,
                    relatedType,
                    null,
                    null,
                    pivotTable,
                    pivotParentKey,
                    pivotRelatedKey,
                    pivotWhereColumn,
                    pivotWhereValue,
                    castSetter(setter),
                    false
                )
            );
            return this;
        }

        public RelationRegistry register() {
            if (TextUtils.isBlank(meta.table)) {
                throw new IllegalStateException("table is required for " + meta.type.getName());
            }
            if (TextUtils.isBlank(meta.primaryKeyProperty)) {
                throw new IllegalStateException("primaryKey is required for " + meta.type.getName());
            }
            registry.register(meta);
            return registry;
        }

        @SuppressWarnings("unchecked")
        private static <P, C> BiConsumer<Object, Object> castSetter(BiConsumer<P, C> setter) {
            return (parent, value) -> setter.accept((P) parent, (C) value);
        }
    }

    public static final class EntityMeta<T> {

        private final Class<T> type;
        private final Map<String, RelationMeta> relations = new LinkedHashMap<>();
        private String table;
        private String primaryKeyProperty;

        EntityMeta(Class<T> type) {
            this.type = type;
        }

        public Class<T> type() {
            return type;
        }

        public String table() {
            return table;
        }

        public String primaryKeyProperty() {
            return primaryKeyProperty;
        }

        public RelationMeta relation(String name) {
            RelationMeta relationMeta = relations.get(name);
            if (relationMeta == null) {
                throw new IllegalArgumentException("Unknown relation '" + name + "' for " + type.getName());
            }
            return relationMeta;
        }

        public Collection<RelationMeta> relations() {
            return relations.values();
        }
    }
}

