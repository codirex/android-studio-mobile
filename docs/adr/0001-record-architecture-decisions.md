# ADR-0001: Record architecture decisions

Status: Accepted

## Context

The project is meant to be developed over many releases. Decisions made early (modules, concurrency, process model) are expensive to change and easy to forget.

## Decision

We record every significant architectural decision as a short ADR in `docs/adr/`, numbered in order. An accepted ADR is not edited; a later ADR supersedes it.

## Consequences

- The reasons behind the architecture stay discoverable.
- Each ADR costs a little time to write, so we only write one for decisions that are hard to reverse.
