# Integration Tests with shared reusable Testcontainers

Mix of [`reused-tc-int-tests`](../reused-tc-int-tests) and [`tc-int-tests`](../tc-int-tests), so not only containers
don't stop between test executions, but they're also shared within the execution (setup just once).
