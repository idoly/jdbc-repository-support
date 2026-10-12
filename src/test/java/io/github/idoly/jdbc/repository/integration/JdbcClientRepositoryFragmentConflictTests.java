package io.github.idoly.jdbc.repository.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.idoly.jdbc.repository.JdbcClientRepository;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.Sample;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.SampleRepository;

class JdbcClientRepositoryFragmentConflictTests {

    @Test
    void explicitFragmentTakesPrecedenceOverAutomaticallyAddedSql() {
        JdbcTestSupport.runner(RepositoryConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ConflictRepository.class)
                    .sql("SELECT 'automatic'").query(String.class).single()).isEqualTo("custom");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJdbcRepositories(basePackageClasses = ConflictRepository.class, considerNestedRepositories = true,
            repositoryFactoryBeanClass = CustomSqlFactoryBean.class)
    static class RepositoryConfiguration {
    }

    interface ConflictRepository extends SampleRepository {
    }

    public static class CustomSqlFactoryBean extends JdbcRepositoryFactoryBean<ConflictRepository, Sample, Long> {

        public CustomSqlFactoryBean(Class<? extends ConflictRepository> repositoryInterface) {
            super(repositoryInterface);
            setRepositoryFragmentsFunction((beans, context) -> {
                JdbcClient client = beans.getBean(JdbcClient.class);
                JdbcClientRepository<Sample, Long> fragment = sql -> client.sql("SELECT 'custom'");
                return RepositoryFragments.just(fragment);
            });
        }
    }
}
