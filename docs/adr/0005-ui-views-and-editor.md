# ADR-0005: UI toolkit and editor

Status: Accepted

## Decision

- The UI is built with XML layouts and Views, not Jetpack Compose.
- Components come from Material Components 1.14.0.
- The code editor is Sora Editor, wrapped by `feature:editor` so that no other module imports it directly.

## Consequences

- Sora Editor is a View, so it fits the XML stack without interop.
- Compose can still be introduced later for isolated screens, but that needs a new ADR.
- The design targets developers. Detailed UX decisions are written down when feature work starts.
