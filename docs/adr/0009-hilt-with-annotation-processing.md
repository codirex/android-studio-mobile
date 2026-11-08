# ADR-0009: Hilt with annotation processing, Java first

Status: Proposed

## Context

Hilt 2.59 supports AGP 9 and requires Gradle 9.1 or newer. The modules are written in Java, so KSP and kapt are not needed, and their compatibility with AGP 9's built-in Kotlin was not verified.

## Decision

- Hilt is applied as `annotationProcessor` in Java modules.
- The Hilt Gradle plugin is applied in `:app` and in every module that contains an `@AndroidEntryPoint` class (the feature modules). Modules that only declare `@Module` or `@HiltViewModel` classes use `hilt-android` and the Hilt compiler without the plugin.
- Kotlin modules, when the first one appears, will use KSP. That decision gets its own record once it is tested.

## Consequences

- No Kotlin annotation processing infrastructure yet.
- Every Hilt-annotated module needs the `hilt-compiler` processor, which adds a little to compile time.
- Forgetting the plugin in a module with `@AndroidEntryPoint` fails the build, so the mistake cannot ship.

## Open items

- Confirm in CI that `feature:project` (plugin in a library module) aggregates correctly into `:app`.
