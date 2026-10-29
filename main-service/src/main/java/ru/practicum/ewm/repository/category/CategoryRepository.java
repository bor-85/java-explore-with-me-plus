package ru.practicum.ewm.repository.category;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.category.Category;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
}