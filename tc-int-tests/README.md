# Integration Tests with Testcontainers

Same as in [`int-tests`](../int-tests), but with Testcontainers (using
[JDBC support](https://java.testcontainers.org/modules/databases/jdbc/))
and [Service Connection](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections).

Containers start just once per `test` execution (shared per all tests).
