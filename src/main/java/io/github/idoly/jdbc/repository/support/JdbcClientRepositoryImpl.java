package io.github.idoly.jdbc.repository.support;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;

import io.github.idoly.jdbc.repository.JdbcClientRepository;

/**
 * Fragment implementation of {@link JdbcClientRepository}.
 *
 * <p>Spring Boot applications receive this fragment through
 * {@link io.github.idoly.jdbc.repository.config.JdbcClientRepositoryAutoConfiguration}.
 * It can also be composed manually when using Spring Data JDBC without Boot.
 *
 * @param <T>  domain type
 * @param <ID> id type
 * @since 1.0.0
 */
public final class JdbcClientRepositoryImpl<T, ID> implements JdbcClientRepository<T, ID> {

    private final JdbcClient jdbcClient;

    /**
     * Creates a repository fragment backed by the given client.
     *
     * @param jdbcClient client used to create SQL statements
     */
    public JdbcClientRepositoryImpl(JdbcClient jdbcClient) {
        Assert.notNull(jdbcClient, "JdbcClient must not be null");
        this.jdbcClient = jdbcClient;
    }

    @Override
    public JdbcClient.StatementSpec sql(String sql) {
        return jdbcClient.sql(sql);
    }
}
