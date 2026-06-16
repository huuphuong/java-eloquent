package io.github.phuonghuu.eloquent.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public final class PaginatedResult<T> {

    private final List<T> items;
    private final long total;
    private final int currentPage;
    private final int size;
    private final long totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PaginatedResult(
        List<T> items,
        long total,
        int currentPage,
        int size,
        long totalPages,
        boolean hasNext,
        boolean hasPrevious
    ) {
        this.items = items == null ? Collections.<T>emptyList() : Collections.unmodifiableList(items);
        this.total = total;
        this.currentPage = currentPage;
        this.size = size;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    @JsonProperty("items")
    public List<T> items() {
        return items;
    }

    @JsonProperty("items")
    public List<T> getItems() {
        return items();
    }

    @JsonProperty("total")
    public long total() {
        return total;
    }

    @JsonProperty("total")
    public long getTotal() {
        return total();
    }

    @JsonProperty("currentPage")
    public int currentPage() {
        return currentPage;
    }

    @JsonProperty("currentPage")
    public int getCurrentPage() {
        return currentPage();
    }

    @JsonProperty("size")
    public int size() {
        return size;
    }

    @JsonProperty("size")
    public int getSize() {
        return size();
    }

    @JsonProperty("totalPages")
    public long totalPages() {
        return totalPages;
    }

    @JsonProperty("totalPages")
    public long getTotalPages() {
        return totalPages();
    }

    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }

    @JsonProperty("hasNext")
    public boolean isHasNext() {
        return hasNext();
    }

    @JsonProperty("hasPrevious")
    public boolean hasPrevious() {
        return hasPrevious;
    }

    @JsonProperty("hasPrevious")
    public boolean isHasPrevious() {
        return hasPrevious();
    }
}

