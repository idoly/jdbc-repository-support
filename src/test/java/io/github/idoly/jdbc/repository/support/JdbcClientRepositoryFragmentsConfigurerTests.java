package io.github.idoly.jdbc.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.FragmentCreationContext;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.RepositoryFragmentsFunction;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.idoly.jdbc.repository.JdbcClientRepository;

class JdbcClientRepositoryFragmentsConfigurerTests {

    @Test
    void configuresJdbcRepositoryFactoryBeans() {
        CapturingFactoryBean factoryBean = new CapturingFactoryBean();

        Object result = new JdbcClientRepositoryFragmentsConfigurer()
                .postProcessBeforeInitialization(factoryBean, "sampleRepository");

        assertThat(result).isSameAs(factoryBean);
        assertThat(factoryBean.fragmentsFunction)
                .isSameAs(JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS);
    }

    @Test
    void leavesOtherBeansUntouched() {
        Object bean = new Object();

        Object result = new JdbcClientRepositoryFragmentsConfigurer()
                .postProcessBeforeInitialization(bean, "otherBean");

        assertThat(result).isSameAs(bean);
    }

    @Test
    void createsFragmentOnlyForOptedInRepositories() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:fragments-configurer;DB_CLOSE_DELAY=-1");
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("jdbcClient", jdbcClient);

        RepositoryFragments fragments = JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS
                .getRepositoryFragments(beanFactory, contextFor(ClientRepository.class));
        RepositoryFragments plainFragments = JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS
                .getRepositoryFragments(new DefaultListableBeanFactory(), contextFor(PlainRepository.class));

        assertThat(fragments).hasSize(1);
        assertThat(fragments.iterator().next().getImplementation())
                .containsInstanceOf(JdbcClientRepositoryImpl.class);
        assertThat(plainFragments).isEmpty();
    }

    @Test
    void reportsMissingJdbcClientForOptedInRepository() {
        assertThatThrownBy(() -> JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS
                .getRepositoryFragments(new DefaultListableBeanFactory(), contextFor(ClientRepository.class)))
                .isInstanceOf(NoSuchBeanDefinitionException.class)
                .hasMessageContaining(JdbcClient.class.getName());
    }

    private static FragmentCreationContext contextFor(Class<?> repositoryInterface) {
        RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(repositoryInterface);
        return new FragmentCreationContext() {
            @Override
            public RepositoryMetadata getRepositoryMetadata() {
                return metadata;
            }

            @Override
            public ValueExpressionDelegate getValueExpressionDelegate() {
                return null;
            }

            @Override
            public ProjectionFactory getProjectionFactory() {
                return null;
            }
        };
    }

    private interface ClientRepository extends JdbcClientRepository<Object, Long> {
    }

    private interface PlainRepository extends Repository<Object, Long> {
    }

    private static final class CapturingFactoryBean
            extends JdbcRepositoryFactoryBean<ClientRepository, Object, Long> {

        private RepositoryFragmentsFunction fragmentsFunction;

        private CapturingFactoryBean() {
            super(ClientRepository.class);
        }

        @Override
        public void setRepositoryFragmentsFunction(RepositoryFragmentsFunction fragmentsFunction) {
            this.fragmentsFunction = fragmentsFunction;
            super.setRepositoryFragmentsFunction(fragmentsFunction);
        }
    }
}
