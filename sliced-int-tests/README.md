# "Sliced" Integration Tests

Spring Context can start with a limited set of beans in tests. E.g. just DB-related ones.

It allows faster startup and more focused tests. But it's not
like app itself work, so might give a false impression about its condition and state.

In addition, many small, but not reusable contexts in tests can result in a slower overall execution time comparing to
one, big context, starting 5 seconds, but just once and reused later by each test.
