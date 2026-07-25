package io.github.idoly.jdbc.repository.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.idoly.jdbc.repository.JdbcRepository;

class JdbcClientRepositoryIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    JdbcClientAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    DataJdbcRepositoriesAutoConfiguration.class,
                    JdbcClientRepositoryAutoConfiguration.class))
            .withUserConfiguration(RepositoryConfiguration.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:repository-integration;DB_CLOSE_DELAY=-1",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=");

    @Test
    void invokesJdbcClientThroughRepositoryProxy() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcClient jdbcClient = context.getBean(JdbcClient.class);
            jdbcClient.sql("CREATE TABLE sample (id BIGINT PRIMARY KEY, name VARCHAR(50))").update();
            jdbcClient.sql("INSERT INTO sample (id, name) VALUES (1, 'alpha')").update();

            SampleRepository repository = context.getBean(SampleRepository.class);

            assertThat(repository.countRows()).isEqualTo(1L);
            assertThat(repository.findName(1L)).isEqualTo("alpha");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJdbcRepositories(
            basePackageClasses = SampleRepository.class,
            considerNestedRepositories = true)
    static class RepositoryConfiguration {
    }

    interface SampleRepository extends JdbcRepository<Sample, Long> {

        default long countRows() {
            return sql("SELECT COUNT(*) FROM sample")
                    .query(Long.class)
                    .single();
        }

        default String findName(long id) {
            return sql("SELECT name FROM sample WHERE id = :id")
                    .param("id", id)
                    .query(String.class)
                    .single();
        }
    }

    @Table("sample")
    record Sample(@Id Long id, String name) {
    }
}
