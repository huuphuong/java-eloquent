package io.github.phuonghuu.eloquent.meta;

import java.io.Serializable;

@FunctionalInterface
public interface SFunction<T, R> extends java.util.function.Function<T, R>, Serializable {
}

