package io.github.phuonghuu.eloquent.query;

import io.github.phuonghuu.eloquent.meta.RelationRegistry;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.concurrent.atomic.AtomicInteger;

public interface QueryPredicate {
    String toSql(RelationRegistry.EntityMeta<?> meta, MapSqlParameterSource params, AtomicInteger index, String tableAlias);
}

