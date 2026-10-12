# JDBC Repository Support

**English** | [简体中文](README_zh-CN.md)

Use Spring Framework's `JdbcClient` directly in Spring Data JDBC repositories to execute SQL.

## Installation

For Spring Boot 4.0.x / 4.1.x projects using `spring-boot-starter-data-jdbc`:

```xml
<dependency>
    <groupId>io.github.idoly</groupId>
    <artifactId>jdbc-repository-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

Extend `JdbcRepository` for CRUD, paging, and sorting, and call `sql(...)` from default methods.

For example:

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(@Id Long id, String name) {}
```

Add a name filter dynamically and bind its parameter:

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

## References

`JdbcClient` and `JdbcTransactionManager` share the same `AbstractRoutingDataSource`. A custom annotation switches the route before a transaction and restores it afterward. See the [dynamic data source tests](src/test/java/io/github/idoly/jdbc/repository/routing/JdbcClientRepositoryRoutingIntegrationTests.java) for the implementation.
