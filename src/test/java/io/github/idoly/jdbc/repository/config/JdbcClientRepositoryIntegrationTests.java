package io.github.idoly.jdbc.repository.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.Sample;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.SampleRepository;

class JdbcClientRepositoryIntegrationTests {

    private final ApplicationContextRunner runner = JdbcTestSupport.runner(RepositoryConfiguration.class);

    @Test
    void sharesDataBetweenCrudAndDefaultSqlMethods() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTestSupport.createTable(context.getBean(JdbcClient.class));
            UserRepository repository = context.getBean(UserRepository.class);

            Sample saved = repository.save(new Sample(null, "alpha"));

            assertThat(repository.findName(saved.id())).isEqualTo("alpha");
            assertThat(repository.countRows()).isEqualTo(1L);
        });
    }

    @Test
    void rollsBackCrudAndSqlTogether() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTestSupport.createTable(context.getBean(JdbcClient.class));
            UserRepository repository = context.getBean(UserRepository.class);

            assertThatThrownBy(repository::saveAndInsertThenFail)
                    .isInstanceOf(IllegalStateException.class).hasMessage("rollback");
            assertThat(repository.count()).isZero();
            assertThat(repository.countRows()).isZero();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJdbcRepositories(basePackageClasses = UserRepository.class, considerNestedRepositories = true)
    static class RepositoryConfiguration {
    }

    interface UserRepository extends SampleRepository {

        @Transactional
        default void saveAndInsertThenFail() {
            Sample saved = save(new Sample(null, "crud"));
            assertThat(findName(saved.id())).isEqualTo("crud");
            sql("INSERT INTO sample (name) VALUES ('sql')").update();
            assertThat(count()).isEqualTo(2L);
            throw new IllegalStateException("rollback");
        }
    }
}
