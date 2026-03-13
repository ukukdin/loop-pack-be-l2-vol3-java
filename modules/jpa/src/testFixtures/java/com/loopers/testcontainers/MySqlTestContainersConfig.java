package com.loopers.testcontainers;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class MySqlTestContainersConfig {

    private static final PostgreSQLContainer<?> postgresContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("loopers")
            .withUsername("test")
            .withPassword("test");
        postgresContainer.start();

        System.setProperty("datasource.postgres-jpa.main.jdbc-url", postgresContainer.getJdbcUrl());
        System.setProperty("datasource.postgres-jpa.main.username", postgresContainer.getUsername());
        System.setProperty("datasource.postgres-jpa.main.password", postgresContainer.getPassword());
    }
}
