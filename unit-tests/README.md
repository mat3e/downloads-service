# Unit Tests

"Typical" unit tests with mocks.

The risk of such tests is they cement the solution - no matter what, you must call method X from class Y in this part of
the code (and it must consume parameters `a`, `b`). Such tests don't enable changes and require changing both tests and
production code at the same time.

Sometimes in such tests the only way of asserting is to check what arguments are passed to mocked dependencies. The risk
is the actual dependency in the production code does something undesirable even though arguments are passed correctly.

There are "ordinary" `LimitingFacadeTest` and more "fancy" tests, using `BDDMockito`, `BDDAssertions`
and `MockitoExtension` - `MockitoExtensionLimitingFacadeTest`.
