package ru.practicum.ewm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.user.UserController;
import ru.practicum.ewm.dto.user.NewUserRequestDto;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.service.user.UserServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserServiceImpl.class)
class UserControllerTest {
    private static final NewUserRequestDto NEW_USER = new NewUserRequestDto("User", "user@mail.ru");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @Test
    void createUserReturns201WithSavedUser() throws Exception {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(NEW_USER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(NEW_USER.name()))
                .andExpect(jsonPath("$.email").value(NEW_USER.email()));
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    void createUserRejectsInvalidFields(NewUserRequestDto user) throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(userRepository);
    }

    private static Stream<NewUserRequestDto> invalidUsers() {
        return Stream.of(
                new NewUserRequestDto(null, NEW_USER.email()),
                new NewUserRequestDto("   ", NEW_USER.email()),
                new NewUserRequestDto("U", NEW_USER.email()),
                new NewUserRequestDto("U".repeat(251), NEW_USER.email()),
                new NewUserRequestDto(NEW_USER.name(), null),
                new NewUserRequestDto(NEW_USER.name(), "   "),
                new NewUserRequestDto(NEW_USER.name(), "invalid-email")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{"})
    void createUserRejectsMissingOrMalformedBody(String body) throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUserReturns409WhenEmailAlreadyExists() throws Exception {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Email already exists"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(NEW_USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    void getUsersReturnsEmptyListWithDefaultPagination() throws Exception {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getOffset()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void getUsersAppliesIdsAndExactOffset() throws Exception {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        when(userRepository.findByIdIn(eq(ids), any(Pageable.class)))
                .thenReturn(List.of(new User(4L, NEW_USER.name(), NEW_USER.email())));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1,2,3,4")
                        .param("from", "3")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].name").value(NEW_USER.name()))
                .andExpect(jsonPath("$[0].email").value(NEW_USER.email()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByIdIn(eq(ids), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(3L);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsersReturnsEmptyListForUnknownIds() throws Exception {
        when(userRepository.findByIdIn(eq(List.of(999L)), any(Pageable.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/users").param("ids", "999"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userRepository).findByIdIn(eq(List.of(999L)), any(Pageable.class));
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, -1"})
    void getUsersRejectsInvalidPagination(int from, int size) throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1,abc"})
    void getUsersRejectsNonNumericIds(String ids) throws Exception {
        mockMvc.perform(get("/admin/users").param("ids", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteUserReturns204WithEmptyBody() throws Exception {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(new User(1L, NEW_USER.name(), NEW_USER.email())));

        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserReturns404WhenUserDoesNotExist() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/admin/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(userRepository, never()).deleteById(anyLong());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void deleteUserRejectsNonPositiveId(long userId) throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository);
    }
}
