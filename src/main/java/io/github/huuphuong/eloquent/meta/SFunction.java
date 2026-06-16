package io.github.huuphuong.eloquent.meta;

import java.io.Serializable;

@FunctionalInterface
public interface SFunction<T, R> extends java.util.function.Function<T, R>, Serializable {
}

