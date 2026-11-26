# ADR-0033: XML language server (LemMinX)

Status: Accepted

## Context

ADR-0031 runs the Java language server (jdt.ls) in the terminal process and talks LSP over a socket
pair. XML files (layouts, manifests, resources) are most of what an Android project edits besides
Java.

## Decision

1. The XML server is Eclipse LemMinX, built as the package `lemminx` in asm-packages (it runs on
   openjdk-17, which the userland already has). The terminal service starts it like jdt.ls:
   `LanguageServerLauncher` maps the language id `xml` to `bin/lemminx`; it needs no workspace data.
2. The editor keeps one `JavaLanguageServer` object per language id (the class name stays; it is the
   generic client of one server). `LanguageServerHolder` keeps them across configuration changes.
3. The file's language picks the server: `.java` to jdt.ls, `.xml` to LemMinX. Completion, error
   marks, definition, info and format work the same way for both.
4. The editor offers to install `lemminx` when it is missing, like `jdtls`.

## Consequences

Attribute completion that knows the Android schema is not there yet: LemMinX completes from the
schemas it can find. Android resource awareness is a later step.
