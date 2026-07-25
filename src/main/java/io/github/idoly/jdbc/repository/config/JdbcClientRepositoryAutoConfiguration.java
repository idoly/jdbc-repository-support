package io.github.idoly.jdbc.repository.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.RepositoryFragmentsFunction;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.idoly.jdbc.repository.JdbcClientRepository;
import io.github.idoly.jdbc.repository.support.JdbcClientRepositoryFragmentsConfigurer;

/**
 * Auto-configuration that adds the {@link JdbcClientRepository} fragment to
 * matching Spring Data JDBC repositories.
 *
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration"
})
@ConditionalOnClass({
        JdbcClient.class,
        JdbcRepositoryFactoryBean.class,
        RepositoryFragmentsFunction.class,
        JdbcClientRepository.class
})
public class JdbcClientRepositoryAutoConfiguration {

    /**
     * Creates the auto-configuration.
     */
    public JdbcClientRepositoryAutoConfiguration() {
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(JdbcClientRepositoryFragmentsConfigurer.class)
    static JdbcClientRepositoryFragmentsConfigurer jdbcClientRepositoryFragmentsConfigurer() {
        return new JdbcClientRepositoryFragmentsConfigurer();
    }
}
