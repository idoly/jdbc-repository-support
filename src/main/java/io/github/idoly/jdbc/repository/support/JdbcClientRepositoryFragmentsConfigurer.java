package io.github.idoly.jdbc.repository.support;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.FragmentCreationContext;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.RepositoryFragmentsFunction;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;

import io.github.idoly.jdbc.repository.JdbcClientRepository;

/**
 * Configures Spring Data JDBC factory beans with a per-repository
 * {@link JdbcClientRepository} fragment.
 *
 * @since 1.0.0
 */
public final class JdbcClientRepositoryFragmentsConfigurer implements BeanPostProcessor {

    static final RepositoryFragmentsFunction FRAGMENTS =
            JdbcClientRepositoryFragmentsConfigurer::createFragments;

    /**
     * Creates a repository factory bean post-processor.
     */
    public JdbcClientRepositoryFragmentsConfigurer() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof JdbcRepositoryFactoryBean<?, ?, ?> factoryBean) {
            factoryBean.setRepositoryFragmentsFunction(FRAGMENTS);
        }
        return bean;
    }

    private static RepositoryFragments createFragments(
            @Nullable BeanFactory beanFactory, FragmentCreationContext context) {
        Class<?> repositoryInterface = context.getRepositoryMetadata().getRepositoryInterface();
        if (!JdbcClientRepository.class.isAssignableFrom(repositoryInterface)) {
            return RepositoryFragments.empty();
        }

        Assert.state(beanFactory != null, "BeanFactory is required to resolve JdbcClient");
        JdbcClient jdbcClient = beanFactory.getBean(JdbcClient.class);
        return RepositoryFragments.just(new JdbcClientRepositoryImpl<>(jdbcClient));
    }
}
