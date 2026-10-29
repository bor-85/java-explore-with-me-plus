package ru.practicum.ewm.service.user;

import jakarta.validation.constraints.Positive;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.model.User;

import java.util.List;

public interface UserService {

    List<User> findAllUsers(List<Long> ids, Integer offset, Integer size);

    User createUser(NewUserRequestDto user);

    void deleteUser(@Positive Long userId);
}
