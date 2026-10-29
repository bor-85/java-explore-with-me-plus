package ru.practicum.ewm.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.Objects;

//Утилита для корректной работы пагинации в тех случаях, когда offset не кратен size
public record OffsetPageable(
        int offset,
        int size,
        Sort sort
) implements Pageable {

    public OffsetPageable {
        if (offset < 0) {
            throw new IllegalArgumentException("offset должен быть >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size должен быть > 0");
        }
        Objects.requireNonNull(sort, "sort");
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    @NonNull
    public Sort getSort() {
        return sort;
    }

    @Override
    public int getPageNumber() {
        return offset / size;
    }

    @Override
    @NonNull
    public Pageable next() {
        return new OffsetPageable(Math.addExact(offset, size), size, sort);
    }

    @Override
    @NonNull
    public Pageable previousOrFirst() {
        return new OffsetPageable(Math.max(0, offset - size), size, sort);
    }

    @Override
    @NonNull
    public Pageable first() {
        return new OffsetPageable(0, size, sort);
    }

    @Override
    @NonNull
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber должен быть >= 0");
        }
        return new OffsetPageable(
                Math.multiplyExact(pageNumber, size),
                size,
                sort
        );
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}