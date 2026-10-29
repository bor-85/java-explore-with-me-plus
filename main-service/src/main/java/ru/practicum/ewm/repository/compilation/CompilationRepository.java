package ru.practicum.ewm.repository.compilation;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.compilation.Compilation;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Optional<Compilation> findByTitleIgnoreCase(String title);

    List<Compilation> findAllByOrderByIdAsc();

    List<Compilation> findByPinnedOrderByIdAsc(boolean pinned);
}