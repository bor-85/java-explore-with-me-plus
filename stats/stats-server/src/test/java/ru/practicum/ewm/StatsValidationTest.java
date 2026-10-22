package ru.practicum.ewm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StatsValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void endpointHitDtoShouldNotAllowNullApp() {
        EndpointHitDto dto = new EndpointHitDto(
                null,
                null,
                "/events/1",
                "192.163.0.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("app");
    }

    @Test
    void endpointHitDtoShouldNotAllowBlankApp() {
        EndpointHitDto dto = new EndpointHitDto(
                null,
                "   ",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("app");
    }

    @Test
    void endpointHitDtoShouldNotAllowBlankUri() {
        EndpointHitDto dto = new EndpointHitDto(
                null,
                "ewm-main-service",
                "   ",
                "192.163.0.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("uri");
    }

    @Test
    void endpointHitDtoShouldNotAllowBlankIp() {
        EndpointHitDto dto = new EndpointHitDto(
                null,
                "ewm-main-service",
                "/events/1",
                "   ",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("ip");
    }

    @Test
    void endpointHitDtoShouldNotAllowNullTimestamp() {
        EndpointHitDto dto = new EndpointHitDto(
                null,
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                null
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("timestamp");
    }
}
