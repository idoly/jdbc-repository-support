package io.github.idoly.jdbc.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Repository fragment that exposes {@link JdbcClient} to Spring Data repository
 * interfaces, typically consumed from {@code default} methods.
 *
 * @param <T>  domain type
 * @param <ID> id type
 * @see JdbcRepository
 * @see io.github.idoly.jdbc.repository.support.JdbcClientRepositoryImpl
 * @since 1.0.0
 */
@NoRepositoryBean
public interface JdbcClientRepository<T, ID> extends Repository<T, ID> {

    /**
     * Start a {@link JdbcClient} statement for the given SQL.
     *
     * @param sql SQL statement to execute
     * @return a statement specification for binding parameters and executing SQL
     * @throws IllegalArgumentException if {@code sql} is {@code null} or empty
     */
    JdbcClient.StatementSpec sql(String sql);
}
