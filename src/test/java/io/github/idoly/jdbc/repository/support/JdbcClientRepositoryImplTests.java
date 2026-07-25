package io.github.idoly.jdbc.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcClientRepositoryImplTests {

    private JdbcClientRepositoryImpl<Object, Long> repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jdbc-client-repository;DB_CLOSE_DELAY=-1");
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("DROP TABLE IF EXISTS sample").update();
        jdbcClient.sql("CREATE TABLE sample (id BIGINT PRIMARY KEY, name VARCHAR(50))").update();
        jdbcClient.sql("INSERT INTO sample (id, name) VALUES (1, 'alpha')").update();
        repository = new JdbcClientRepositoryImpl<>(jdbcClient);
    }

    @Test
    void delegatesStatementsToJdbcClient() {
        String name = repository.sql("SELECT name FROM sample WHERE id = :id")
                .param("id", 1L)
                .query(String.class)
                .single();

        assertThat(name).isEqualTo("alpha");
    }

    @Test
    void rejectsMissingJdbcClient() {
        assertThatThrownBy(() -> new JdbcClientRepositoryImpl<>(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JdbcClient must not be null");
    }
}
