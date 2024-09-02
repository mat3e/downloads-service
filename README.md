# Example Downloads Limiting app

Example service storing information about `Account`s and their downloaded `Asset`s with some further configuration like
what is the `Account`'s limit.

Main part and the target solution is the [`core`](core) module and around there are various ways of testing it:

* [`unit-tests`](unit-tests) - unit tests with mocks
* [`int-tests`](int-tests) - integration tests sharing the context
* [`sliced-int-tests`](sliced-int-tests) - integration tests with dedicated, small contexts
* [`tc-int-tests`](tc-int-tests) - integration tests with shared Testcontainers (start once per test run)
* [`restarting-tc-int-tests`](restarting-tc-int-tests) - integration tests with Testcontainers starting and stopping in each test class
* [`reused-tc-int-tests`](reused-tc-int-tests) - integration tests with Testcontainers running between test executions (but context not shared)
* [`shared-reused-tc-int-tests`](shared-reused-tc-int-tests) - integration tests with shared Testcontainers running between testexecutions
