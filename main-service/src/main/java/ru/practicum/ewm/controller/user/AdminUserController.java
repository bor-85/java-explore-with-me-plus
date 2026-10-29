package ru.practicum.ewm.controller.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.service.user.AdminUserService;

import java.util.List;


@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;


    @GetMapping
    public List<UserDto> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0", name = "from") @PositiveOrZero Integer offset,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        return adminUserService.findAllUsers(ids, offset, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequestDto user) {
        return adminUserService.createUser(user);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable @Positive Long userId) {
        adminUserService.deleteUser(userId);
    }
}
