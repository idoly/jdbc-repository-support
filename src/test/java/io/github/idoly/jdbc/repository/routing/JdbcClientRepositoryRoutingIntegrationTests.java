package io.github.idoly.jdbc.repository.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.Sample;
import io.github.idoly.jdbc.repository.testsupport.JdbcTestSupport.SampleRepository;

class JdbcClientRepositoryRoutingIntegrationTests {

    private final ApplicationContextRunner runner = JdbcTestSupport.runner(RoutingConfiguration.class);

    @Test
    void routesCrudAndSqlBeforeStartingTheTransaction() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RoutedRepository repository = context.getBean(RoutedRepository.class);
            Sample saved = repository.saveAndRead("order");

            assertThat(saved.name()).isEqualTo("order");
            assertThat(JdbcClient.create(context.getBean("B", DataSource.class))
                    .sql("SELECT name FROM sample WHERE id = :id").param("id", saved.id())
                    .query(String.class).single()).isEqualTo("order");
            assertThat(repository.count()).isEqualTo(1L);
            assertThat(repository.findName(100L)).isEqualTo("main");
        });
    }

    @Test
    void rollsBackCrudAndSqlAndRestoresTheDefaultRoute() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RoutedRepository repository = context.getBean(RoutedRepository.class);

            assertThatThrownBy(repository::insertThenFail)
                    .isInstanceOf(IllegalStateException.class).hasMessage("rollback");
            assertThat(JdbcClient.create(context.getBean("B", DataSource.class))
                    .sql("SELECT COUNT(*) FROM sample").query(Long.class).single()).isEqualTo(1L);
            assertThat(repository.count()).isEqualTo(1L);
            assertThat(repository.findName(100L)).isEqualTo("main");
        });
    }

    @Test
    void restoresTheOuterRouteAfterANestedRepositoryCall() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            RoutedRepository repository = context.getBean(RoutedRepository.class);
            assertThat(repository.nested(context.getBean(MainRepository.class))).containsExactly("main", "other");
            assertThat(repository.findName(100L)).isEqualTo("main");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJdbcRepositories(basePackageClasses = RoutedRepository.class, considerNestedRepositories = true)
    static class RoutingConfiguration {

        @Bean("A") DataSource mainDataSource() { return JdbcTestSupport.dataSource("main"); }
        @Bean("B") DataSource otherDataSource() { return JdbcTestSupport.dataSource("other"); }

        @Bean
        @Primary
        RoutingDataSource routingDataSource(@Qualifier("A") DataSource a, @Qualifier("B") DataSource b) {
            return new RoutingDataSource(a, Map.of("a", a, "b", b));
        }

        @Bean
        JdbcClient jdbcClient(RoutingDataSource source) { return JdbcClient.create(source); }

        @Bean
        JdbcTransactionManager transactionManager(RoutingDataSource source) { return new JdbcTransactionManager(source); }

        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        static Advisor routingAdvisor(ObjectProvider<RoutingDataSource> source) {
            MethodInterceptor interceptor = invocation -> {
                Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass());
                UseDataSource route = AnnotatedElementUtils.findMergedAnnotation(method, UseDataSource.class);
                try (AutoCloseable ignored = source.getObject().use(route.value())) {
                    return invocation.proceed();
                }
            };
            DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(
                    AnnotationMatchingPointcut.forMethodAnnotation(UseDataSource.class), interceptor);
            advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return advisor;
        }
    }

    static class RoutingDataSource extends AbstractRoutingDataSource {

        private final ThreadLocal<String> currentKey = new ThreadLocal<>();

        RoutingDataSource(DataSource fallback, Map<Object, Object> targets) {
            setTargetDataSources(targets);
            setDefaultTargetDataSource(fallback);
            setLenientFallback(false);
        }

        @Override
        protected Object determineCurrentLookupKey() { return currentKey.get(); }

        AutoCloseable use(String key) {
            String previous = currentKey.get();
            currentKey.set(key);
            return () -> {
                if (previous == null) {
                    currentKey.remove();
                } else {
                    currentKey.set(previous);
                }
            };
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface UseDataSource {
        String value();
    }

    interface RoutedRepository extends SampleRepository {

        @UseDataSource("b")
        @Transactional
        default Sample saveAndRead(String name) {
            Sample saved = save(new Sample(null, name));
            return new Sample(saved.id(), findName(saved.id()));
        }

        @UseDataSource("b")
        @Transactional
        default void insertThenFail() {
            save(new Sample(null, "crud"));
            sql("INSERT INTO sample (name) VALUES ('sql')").update();
            assertThat(count()).isEqualTo(3L);
            assertThat(countRows()).isEqualTo(3L);
            throw new IllegalStateException("rollback");
        }

        @UseDataSource("b")
        default List<String> nested(MainRepository main) { return List.of(main.mainName(), findName(100L)); }
    }

    interface MainRepository extends SampleRepository {

        @UseDataSource("a")
        default String mainName() { return findName(100L); }
    }
}
