# ADR-0034: Design system and theme

Status: Accepted

## Context

The UI was redesigned in Figma (dark-first, ASM green accent, layered surfaces, JetBrains Mono for
code). ADR-0010 left the brand palette as a later decision and applied wallpaper colors on
Android 12+.

## Decision

1. `core:ui` owns the palette (`colors.xml`) and `Theme.Asm` in `values/` (light) and
   `values-night/` (dark). Components read Material 3 theme attributes only; features never
   hard-code the hex values.
2. Surfaces map to the design layers: base = `colorSurface`, surface = `colorSurfaceContainer`,
   raised = `colorSurfaceContainerHigh`, overlay = `colorSurfaceContainerHighest`.
3. Wallpaper (dynamic) colors are not applied, so the look is the same on every device.
4. Shapes use a rounded 8/12/16 dp scale. Code uses `TextAppearance.Asm.Code` (platform monospace
   for now; bundling JetBrains Mono is a later step because the font files cannot be fetched from
   the build sandbox).
5. The light palette is derived from the dark one and has no Figma screens yet.

## Consequences

Screens are rebuilt one by one on these tokens: editor chrome, build, projects, command palette,
toolchain.
