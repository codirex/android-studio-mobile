# Architecture

Status: draft, under discussion. Decisions are recorded as ADRs in [docs/adr](adr/README.md); this page is the overview.

## Principles

1. Separation of responsibilities: every module has one reason to change.
2. Dependencies point one way, from the edge inward.
3. The domain knows nothing about Android.
4. Heavy work is owned, cancellable and testable ([ADR-0004](adr/0004-concurrency-clotho.md)).
5. Slow or risky subsystems run in their own process ([ADR-0006](adr/0006-terminal-separate-process.md)).
6. Each module has a small public API; everything else is internal.

## Layers

| Layer | Contains | May depend on | Android APIs |
|---|---|---|---|
| `app` | Hilt root, navigation, process wiring | everything | yes |
| `feature:*` | screens: Fragments, XML layouts, ViewModels | `domain:*`, `core:ui`, `core:concurrency`, `core:common` | yes |
| `domain:*` | models, use cases, repository interfaces | other `domain:*`, Clotho core | no |
| `data:*` | implementations of domain interfaces | `domain:*`, `core:*` | yes |
| `core:*` | shared infrastructure | other `core:*` | yes |
| `terminal:*` | the terminal subsystem (separate process) | `terminal:*`, `core:common` | yes |

Pure types shared by several domain modules live in `domain:common`.

## Rules

1. Dependencies point inward only. `data` implements `domain`; `domain` never sees `data`.
2. A feature never depends on another feature.
3. `domain:*` has no Android imports. It is plain JVM and may mix Java and Kotlin.
4. Only `core:concurrency` depends on `clotho-android`.
5. `feature:terminal` talks to the terminal service only through `terminal:protocol`.
6. Implementation classes are package-private (Java) or `internal` (Kotlin).

## Processes

| Process | Runs |
|---|---|
| main | UI, editor, project and template features |
| `:terminal` | terminal sessions and the child processes they start |

Builds (v1.3.0) and language servers (v1.2.0) start as child processes. Which process owns them is an open question in [ADR-0006](adr/0006-terminal-separate-process.md).

## Planned modules for v1.0.0

| Module | Responsibility |
|---|---|
| `app` | entry point, Hilt root, navigation |
| `core:common` | Android utilities |
| `core:concurrency` | realm provision, main lane setup, UI bridge |
| `core:ui` | theme and shared views |
| `core:preferences` | typed preferences over AndroidX Preferences |
| `core:fs` | file access abstraction |
| `domain:common` | pure shared types |
| `domain:project` | project model and creation use cases |
| `domain:template` | template model, validation and rendering |
| `data:project`, `data:template` | storage for the domain interfaces |
| `feature:project` | new project wizard, open project |
| `feature:templates` | template manager and editor |
| `feature:filetree` | project file tree |
| `feature:editor` | Sora Editor, tabs, auto save, highlighting |
| `feature:terminal` | terminal screen |
| `terminal:protocol` | IPC contract |
| `terminal:service` | the `:terminal` process |
| `terminal:emulator` | terminal emulation and rendering |
| `terminal:bootstrap` | userland installer |

The list is a plan, not a contract. A module is created when its first real code arrives.

## Open questions

- Navigation approach.
- Whether View Binding is mandatory.
- How the dependency rules are enforced (convention plugins plus a CI check).
- Whether builds and language servers share the terminal process.
