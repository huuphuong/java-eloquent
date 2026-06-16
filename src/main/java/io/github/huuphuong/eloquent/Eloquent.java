package io.github.huuphuong.eloquent;

import io.github.huuphuong.eloquent.meta.RelationRegistry;
import io.github.huuphuong.eloquent.query.QueryBuilder;
import io.github.huuphuong.eloquent.write.RelationWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.Objects;

public final class Eloquent {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RelationRegistry registry;

    public static Eloquent of(NamedParameterJdbcTemplate jdbcTemplate, RelationRegistry registry) {
        return new Eloquent(jdbcTemplate, registry);
    }

    public Eloquent(NamedParameterJdbcTemplate jdbcTemplate, RelationRegistry registry) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
        this.registry = Objects.requireNonNull(registry, "registry is required");
    }

    public NamedParameterJdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    public RelationRegistry registry() {
        return registry;
    }

    public <T> QueryBuilder<T> query(Class<T> type) {
        return new QueryBuilder<>(jdbcTemplate, registry, type);
    }

    public <T> RelationWriter<T> write(Class<T> type) {
        return new RelationWriter<>(jdbcTemplate, registry, type);
    }

    public <T> int updatePartialByMap(Class<T> type, Object id, Map<String, ?> patchValues) {
        return write(type).updatePartialById(id, patchValues);
    }
}

