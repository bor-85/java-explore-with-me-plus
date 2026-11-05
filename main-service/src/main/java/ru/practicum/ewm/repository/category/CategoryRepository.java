package ru.practicum.ewm.repository.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.category.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query(value = """
            SELECT *
            FROM categories c
            ORDER BY c.id
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Category> findAllPaged(@Param("from") int from,
                                @Param("size") int size);
}