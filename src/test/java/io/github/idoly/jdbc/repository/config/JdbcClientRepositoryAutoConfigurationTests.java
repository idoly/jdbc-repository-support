package io.github.idoly.jdbc.repository.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.RepositoryFragmentsFunction;

import io.github.idoly.jdbc.repository.support.JdbcClientRepositoryFragmentsConfigurer;

class JdbcClientRepositoryAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JdbcClientRepositoryAutoConfiguration.class));

    @Test
    void registersFragmentsConfigurer() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(JdbcClientRepositoryFragmentsConfigurer.class));
    }

    @Test
    void backsOffForUserConfigurer() {
        JdbcClientRepositoryFragmentsConfigurer userConfigurer =
                new JdbcClientRepositoryFragmentsConfigurer();

        contextRunner.withBean(JdbcClientRepositoryFragmentsConfigurer.class, () -> userConfigurer)
                .run(context -> assertThat(context)
                        .getBean(JdbcClientRepositoryFragmentsConfigurer.class)
                        .isSameAs(userConfigurer));
    }

    @Test
    void backsOffWithoutSpringDataFourFragmentApi() {
        contextRunner.withClassLoader(new FilteredClassLoader(RepositoryFragmentsFunction.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(JdbcClientRepositoryFragmentsConfigurer.class));
    }
}
