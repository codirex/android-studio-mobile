# ADR-0009: Hilt with annotation processing, Java first

Status: Proposed

## Context

Hilt 2.59 supports AGP 9 and requires Gradle 9.1 or newer. The modules are written in Java, so KSP and kapt are not needed, and their compatibility with AGP 9's built-in Kotlin was not verified.

## Decision

- Hilt is applied as `annotationProcessor` in Java modules.
- The Hilt Gradle plugin is applied only in `:app`. Library modules that only declare `@Module` classes use `hilt-android` and the Hilt compiler without the plugin.
- Kotlin modules, when the first one appears, will use KSP. That decision gets its own record once it is tested.

## Consequences

- No Kotlin annotation processing infrastructure yet.
- Every Hilt-annotated module needs the `hilt-compiler` processor, which adds a little to compile time.

## Open items

- Confirm in CI that the plugin-less library modules and the app aggregate correctly.
