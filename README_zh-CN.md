# JDBC Repository Support

[English](README.md) | **简体中文**

让 Spring Data JDBC Repository 可以直接使用 Spring Framework `JdbcClient` 编写 SQL。

## 安装

Spring Boot 4.0.x / 4.1.x 项目搭配 `spring-boot-starter-data-jdbc` 使用：

```xml
<dependency>
    <groupId>io.github.idoly</groupId>
    <artifactId>jdbc-repository-support</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 使用

继承 `JdbcRepository`，即可使用 CRUD、分页和排序，并在默认方法中调用 `sql(...)`。

以用户查询为例：

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(@Id Long id, String name) {}
```

根据姓名动态添加查询条件，并绑定参数：

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

## 参考

`JdbcClient` 和 `JdbcTransactionManager` 共用同一个 `AbstractRoutingDataSource`，自定义注解负责在事务开始前切换、结束后恢复。实现见[动态数据源测试](src/test/java/io/github/idoly/jdbc/repository/routing/JdbcClientRepositoryRoutingIntegrationTests.java)。
