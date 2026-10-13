package ru.ten.parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TenParserApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenParserApplication.class, args);
    }
}
