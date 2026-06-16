package io.github.phuonghuu.eloquent.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public final class CursorPaginatedResult<T> {

    private final List<T> items;
    private final Object nextCursorId;
    private final int size;
    private final boolean hasNext;

    public CursorPaginatedResult(List<T> items, Object nextCursorId, int size, boolean hasNext) {
        this.items = items == null ? Collections.<T>emptyList() : Collections.unmodifiableList(items);
        this.nextCursorId = nextCursorId;
        this.size = size;
        this.hasNext = hasNext;
    }

    @JsonProperty("items")
    public List<T> items() {
        return items;
    }

    @JsonProperty("items")
    public List<T> getItems() {
        return items();
    }

    @JsonProperty("nextCursorId")
    public Object nextCursorId() {
        return nextCursorId;
    }

    @JsonProperty("nextCursorId")
    public Object getNextCursorId() {
        return nextCursorId();
    }

    @JsonProperty("size")
    public int size() {
        return size;
    }

    @JsonProperty("size")
    public int getSize() {
        return size();
    }

    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }

    @JsonProperty("hasNext")
    public boolean isHasNext() {
        return hasNext();
    }
}

