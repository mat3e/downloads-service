# Integration Tests

Same as in [`sliced-int-tests`](../sliced-int-tests), but with one Spring Context reused by all tests.

Simply by not using `@MockBean`, tests started to better relect the actual interactions with the app.

It's not as good as in [`core`](../core), but definitely a good direction.
