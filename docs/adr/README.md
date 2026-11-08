# Architecture decision records

Each record states one decision: the context, the decision and its consequences. An accepted record is not edited. A later record supersedes it.

Status is one of: Proposed, Accepted, Superseded by ADR-NNNN.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-toolchain-and-platform.md) | Toolchain and platform | Accepted |
| [0003](0003-module-layering.md) | Module layering | Accepted |
| [0004](0004-concurrency-clotho.md) | Concurrency with Clotho | Accepted, scoping details proposed |
| [0005](0005-ui-views-and-editor.md) | UI toolkit and editor | Accepted |
| [0006](0006-terminal-separate-process.md) | The terminal runs in a separate process | Accepted |
| [0007](0007-bootstrap-from-termux-packages.md) | Userland built from termux-packages | Accepted |
| [0008](0008-ci-on-github-actions.md) | Builds and releases run on GitHub Actions | Accepted |
| [0009](0009-hilt-with-annotation-processing.md) | Hilt with annotation processing, Java first | Proposed |
| [0010](0010-ui-foundation.md) | UI foundation | Proposed |
| [0011](0011-ports-and-file-access.md) | Domain ports and low-level file access | Proposed |
| [0012](0012-project-storage.md) | Where projects live and how they are identified | Proposed |

## Template

```
# ADR-NNNN: Title

Status: Proposed | Accepted | Superseded by ADR-NNNN

## Context
## Decision
## Consequences
## Open items (optional)
```
