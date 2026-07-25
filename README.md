# JDBC Repository Support

**English** | [简体中文](README_zh-CN.md)

Use Spring Framework's `JdbcClient` directly in Spring Data JDBC repositories to execute SQL.

## Dependency

```xml
<dependency>
    <groupId>io.github.idoly</groupId>
    <artifactId>jdbc-repository-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot applications normally use this library with `spring-boot-starter-data-jdbc` and a
configured data source.

## Requirements

| Component | Version |
| --------- | ------- |
| Java | 17+ |
| Spring Data JDBC | 4.x |
| Spring Boot auto-configuration | 4.0.x or 4.1.x |

## `JdbcRepository<T, ID>`

`JdbcRepository<T, ID>` combines `JdbcClientRepository<T, ID>`, `ListCrudRepository<T, ID>`, and
`ListPagingAndSortingRepository<T, ID>`.

Define an entity:

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(@Id Long id, String name) {
}
```

Extend `JdbcRepository` and call `sql(...)` from a `default` method. When a filter is present, add
its SQL clause and parameter together.

```java
import io.github.idoly.jdbc.repository.JdbcRepository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UserRepository extends JdbcRepository<User, Long> {

    default List<User> search(String name) {
        StringBuilder query = new StringBuilder("SELECT id, name FROM users");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(name)) {
            query.append(" WHERE name LIKE :name");
            params.put("name", "%" + name + "%");
        }

        return sql(query.toString())
                .params(params)
                .query(User.class)
                .list();
    }
}
```

Bind dynamic values with `param(...)` or `params(...)` instead of appending them directly to SQL.

## JdbcClient Results

`sql(...)` returns `JdbcClient.StatementSpec`, so the standard `JdbcClient` API remains available.

| Result | Example |
| ------ | ------- |
| List | `.query(User.class).list()` |
| Single row | `.query(User.class).single()` |
| Optional row | `.query(User.class).optional()` |
| Scalar value | `.query(Long.class).single()` |
| Update count | `.update()` |

## Auto-Configuration

With `spring-boot-starter-data-jdbc` and a configured data source, Spring Boot uses the
auto-configured `JdbcClient` bean to provide `sql(...)` for repositories that extend
`JdbcRepository` or `JdbcClientRepository`. Other repositories are unchanged.

## Without Spring Boot

Register `JdbcClient` and `JdbcClientRepositoryFragmentsConfigurer` in a Spring configuration
class:

```java
@Bean
JdbcClient jdbcClient(DataSource dataSource) {
    return JdbcClient.create(dataSource);
}

@Bean
static JdbcClientRepositoryFragmentsConfigurer jdbcClientRepositoryFragmentsConfigurer() {
    return new JdbcClientRepositoryFragmentsConfigurer();
}
```
