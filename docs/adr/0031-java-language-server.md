# ADR-0031: Java language server in the userland

Status: Proposed

## Context
v1.2.0 brings completion, diagnostics and navigation for Java. Writing a Java compiler front end for the phone is out of reach; Eclipse JDT LS (`jdt.ls`) is the language server behind Eclipse and VS Code Java and speaks the Language Server Protocol over stdio. The decision (taken by the owner) is to run it inside the userland, with the IDE holding only an LSP client.

## Decision
- **Packaging.** `asm-packages` builds two packages listed in `packages/lsp.txt`: `openjdk-21` and `jdtls`. jdt.ls 1.63.0 needs Java 21 to run; the userland's JDK 17 stays the default for Gradle. The `jdtls` package unpacks the official archive (SHA-256 checked) under `opt/jdtls` and installs a small `sh` launcher, `bin/jdtls`, which copies the configuration to a writable cache and starts Java with `-Xmx${JDTLS_XMX:-1g}`.
- **Client.** `domain:lsp` is the client: JSON reading and writing (`Json`), the framing (`MessageIo`) and `LspClient` (requests as futures, notifications, server requests, close handling) over two streams. It knows no server and no Android API.
- **Process.** The server runs in the `:terminal` process, which already owns the userland environment (`TerminalEnvironment`). A new method of the terminal service opens a *process* (not a PTY session) and returns a socket pair end as a `ParcelFileDescriptor`; the service pumps it to and from the process' stdin/stdout. Standard error is kept by the service for diagnostics. The UI process wraps the descriptor in streams and gives them to `LspClient`.
- **JDK for the server.** The `jdtls` launcher picks the JDK 21 of the userland itself (or `JDTLS_JAVA`). The terminal's `JAVA_HOME` is still the newest installed JDK (ADR-0027), so Gradle runs on JDK 21 once it is installed; the Android Gradle plugin 9 supports it.
- **Toolchain.** The toolchain screen offers JDK 21 (`openjdk-21`) next to JDK 17. The language server package is offered where it is needed: when a Java file is opened and `bin/jdtls` is missing, the editor offers to install `jdtls` (which depends on `openjdk-21`); the server starts only when it exists.

## Consequences
- A phone must hold the JDK 21 (about the size of JDK 17) and the server (about 50 MB) and give the server around 1 GB of heap. The heap is a setting through `JDTLS_XMX`.
- The client is testable on the JVM with pipes; the server side is verified on a device only.
- Other languages can reuse `domain:lsp` and the process method later (Kotlin, XML).

## Open items
- Workspace and project import for jdt.ls (Gradle projects, classpath of the Android platform).
- Editor integration: didOpen/didChange, completion popup, diagnostics, hover.
- Stopping the server when no Java file is open; restart on failure.
