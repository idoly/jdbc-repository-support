package io.github.idoly.jdbc.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport.FragmentCreationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import io.github.idoly.jdbc.repository.JdbcClientRepository;

class JdbcClientRepositorySupportTests {

    @Test
    void ignoresRepositoriesWithoutJdbcClientSupport() {
        assertThat(JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS
                .getRepositoryFragments(null, contextFor(PlainRepository.class))).isEmpty();
    }

    @Test
    void reportsMissingJdbcClient() {
        assertThatThrownBy(() -> JdbcClientRepositoryFragmentsConfigurer.FRAGMENTS
                .getRepositoryFragments(new DefaultListableBeanFactory(), contextFor(ClientRepository.class)))
                .isInstanceOf(NoSuchBeanDefinitionException.class).hasMessageContaining(JdbcClient.class.getName());
    }

    @Test
    void rejectsNullJdbcClient() {
        assertThatThrownBy(() -> new JdbcClientRepositoryImpl<>(null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("JdbcClient must not be null");
    }

    private static FragmentCreationContext contextFor(Class<?> repositoryType) {
        FragmentCreationContext context = mock(FragmentCreationContext.class);
        when(context.getRepositoryMetadata()).thenReturn(AbstractRepositoryMetadata.getMetadata(repositoryType));
        return context;
    }

    private interface ClientRepository extends JdbcClientRepository<Object, Long> {
    }

    private interface PlainRepository extends Repository<Object, Long> {
    }
}
