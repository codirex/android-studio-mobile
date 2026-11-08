# ADR-0010: UI foundation

Status: Proposed

## Context

[ADR-0005](0005-ui-views-and-editor.md) chose XML Views. [ADR-0003](0003-module-layering.md) left open how screens are navigated and whether View Binding is mandatory. Every feature will build on these answers, so they are settled before the first feature.

## Decision

1. **Theme.** `core:ui` owns `Theme.Asm`, a Material 3 DayNight theme. Dynamic color is applied on Android 12 and newer; older versions use the Material 3 baseline. A brand palette and the editor color schemes come later and will not change the theme name.
2. **View Binding is mandatory.** The convention plugins enable it in every Android module. `findViewById` is not used.
3. **Navigation.** No Navigation component yet. `app` hosts a single activity and owns a small navigator over `FragmentManager`. Features expose a `Fragment` and a contract interface; they never reference each other (ADR-0003 rule 2). The navigator can be replaced by the Navigation component later without touching features, because they only see the contract.
4. **Preferences.** `core:preferences` provides `PreferenceStore` and typed `Pref<T>` keys, backed by the default shared preferences file so the settings screens built with AndroidX Preference share the same data. Features declare their own keys.

## Consequences

- Material types are visible to every module that depends on `core:ui`.
- Reading preferences can touch disk on first use. If startup time suffers, the store gets an asynchronous warm-up through androidx.startup.
- The navigator is not written yet. It is added with the first feature that needs a second screen.

## Open items

- Brand palette and the editor color scheme format.
- Tablet and foldable layouts: when the file tree and the editor sit side by side.
- Whether `SharedPreferences` stays or moves to DataStore. DataStore has no Java-friendly synchronous API, so it is deferred.
