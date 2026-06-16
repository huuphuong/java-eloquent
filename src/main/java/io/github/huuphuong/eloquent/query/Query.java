package io.github.huuphuong.eloquent.query;

import java.util.List;

public interface Query<T> {
    List<T> get();

    T first();

    T find(Object id);
}

