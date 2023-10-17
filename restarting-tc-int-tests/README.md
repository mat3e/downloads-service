# Integration Tests with restarting Testcontainers

Similar to [`tc-int-tests`](../tc-int-tests), but here each test class introduces its own Testcontainers as `static`
fields, so containers start once per class and are used just for all test methods there. At the end of class run, they
stop.
