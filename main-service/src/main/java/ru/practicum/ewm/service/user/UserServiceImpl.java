package ru.practicum.ewm.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.util.OffsetPageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<User> findAllUsers(List<Long> ids, Integer offset, Integer size) {
        Pageable pageable = new OffsetPageable(
                offset,
                size,
                Sort.by("id").ascending()
        );


        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable).getContent();
        }

        return userRepository.findByIdIn(ids, pageable);
    }

    @Override
    @Transactional
    public User createUser(NewUserRequestDto user) {
        return userRepository.save(UserMapper.toUser(user));
    }


    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (userRepository.findById(userId).isPresent()) {
            userRepository.deleteById(userId);
        } else throw new NotFoundException("Не найден пользователь с id = " + userId);
    }
}
