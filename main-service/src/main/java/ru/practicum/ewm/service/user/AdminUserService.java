package ru.practicum.ewm.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.ErrorMessages;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.user.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private final UserRepository userRepository;

    public List<UserDto> findAllUsers(List<Long> ids, Integer offset, Integer size) {
        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAllPaged(offset, size)
                : userRepository.findByIdsIn(ids, offset, size);
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Transactional
    public UserDto createUser(NewUserRequestDto user) {
        return UserMapper.toDto(userRepository.save(UserMapper.toUser(user)));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId));
        }
        userRepository.deleteById(userId);
    }
}
