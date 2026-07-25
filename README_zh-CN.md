# JDBC Repository Support

[English](README.md) | **简体中文**

让 Spring Data JDBC Repository 可以直接使用 Spring Framework `JdbcClient` 编写 SQL。

## 引入依赖

```xml
<dependency>
    <groupId>io.github.idoly</groupId>
    <artifactId>jdbc-repository-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot 应用通常与 `spring-boot-starter-data-jdbc` 和已配置的数据源一起使用。

## 环境要求

| 组件 | 版本 |
| ---- | ---- |
| Java | 17+ |
| Spring Data JDBC | 4.x |
| Spring Boot 自动配置 | 4.0.x 或 4.1.x |

## `JdbcRepository<T, ID>`

`JdbcRepository<T, ID>` 组合了 `JdbcClientRepository<T, ID>`、
`ListCrudRepository<T, ID>` 和 `ListPagingAndSortingRepository<T, ID>`。

先定义实体：

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(@Id Long id, String name) {
}
```

Repository 继承 `JdbcRepository`，然后在 `default` 方法中调用 `sql(...)`。存在查询条件时，
同时添加 SQL 片段和对应参数。

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

动态值应通过 `param(...)` 或 `params(...)` 绑定，不要直接拼接到 SQL 中。

## JdbcClient 返回结果

`sql(...)` 返回 `JdbcClient.StatementSpec`，因此可以继续使用标准的 `JdbcClient` API。

| 需要的结果 | 调用方式 |
| ---------- | -------- |
| 列表 | `.query(User.class).list()` |
| 单条记录 | `.query(User.class).single()` |
| 可选记录 | `.query(User.class).optional()` |
| 单个值 | `.query(Long.class).single()` |
| 更新行数 | `.update()` |

## 自动配置

引入 `spring-boot-starter-data-jdbc` 并配置数据源后，Spring Boot 会使用自动创建的
`JdbcClient` Bean，为继承 `JdbcRepository` 或 `JdbcClientRepository` 的 Repository 提供
`sql(...)`，其他 Repository 不受影响。

## 不使用 Spring Boot

需要在 Spring 配置类中自行注册 `JdbcClient` 和
`JdbcClientRepositoryFragmentsConfigurer`：

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
