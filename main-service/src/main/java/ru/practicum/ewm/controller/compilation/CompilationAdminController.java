package ru.practicum.ewm.controller.compilation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.service.compilation.CompilationService;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CompilationAdminController {
    private final CompilationService compilationService;

    @PostMapping
    public ResponseEntity<CompilationDto> createCompilation(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compilationService.createCompilation(newCompilationDto));
    }

    @PatchMapping("/{compId}")
    public ResponseEntity<CompilationDto> updateCompilation(@PathVariable Long compId,
                                                            @Valid @RequestBody UpdateCompilationRequest request) {
        return ResponseEntity.ok(compilationService.updateCompilation(compId, request));
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<Void> deleteCompilation(@PathVariable Long compId) {
        compilationService.deleteCompilation(compId);
        return ResponseEntity.noContent().build();
    }
}