package io.github.idoly.jdbc.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Convenience base repository: {@link JdbcClientRepository} plus list-based CRUD,
 * paging, and sorting.
 *
 * @param <T>  domain type
 * @param <ID> id type
 * @since 1.0.0
 */
@NoRepositoryBean
public interface JdbcRepository<T, ID>
        extends JdbcClientRepository<T, ID>, ListCrudRepository<T, ID>, ListPagingAndSortingRepository<T, ID> {
}
