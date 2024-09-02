# Core

App itself and tests I'd normally write in the project.

## App

* `limiting` - "core of the core" responsible for storing the configuration and validating downloads.
    * `LimitingFacade` - main entry point, for REST APIs and for overriding `Account`'s limit (not used yet).
* `reporting` - generic component, consuming events from other parts of the system.
    * For now just for collecting suspicious `Account`'s activity, like downloading the same `Asset` in a different
      country.
    * Should probably include more visualization and data analysis in the future (e.g. what are the most
      suspicious `Accounts`).
* `eventhandling` - technical component, including base `DomainEvent`.
* `exceptionhandling` - another technical component, for making user-facing errors more readable.

Potentially, there should be another business component (like `assignment`) for storing `Account` subscription
information and translating it into events, consumed as `Account` configuration changes by `limiting`.

## Testing

* `testFixtures` - source set with helper classes for tests (e.g. `BusinessAssertions`).
* `LimitingTest` - unit tests where the whole package is the unit (a.k.a. "component tests").
    * Whole package needs to be set up, ideally as close to production version as possible (e.g. by
      using `LimitingConfiguration`, similar as Spring does it).
    * Setup and assertions should happen just with package public APIs. The rest is treated as black box, so refactoring
      is heavily encouraged.
    * BDD.
* `LimitingIntTest` - integration tests, starting the whole app with Spring.
    * Dedicated properties (`application-test.yaml`), building on top of production ones.
    * Setting up with facade (in the future it should probably be a message broker), asserting with REST APIs.
