package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.practicum.client.StatClient;

@SpringBootApplication
@Import(StatClient.class)
public class MainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainServiceApplication.class, args);
    }
}
