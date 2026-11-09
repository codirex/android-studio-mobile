# ADR-0016: Syntax highlighting

Status: Proposed

## Context

v1.0.0 wants highlighting in the editor, and later suggestions, formatters and parsers (ADR-0005 chose Sora). Sora brings highlighting through a `Language` object per editor. It offers a ready Java language (`language-java`), TextMate grammars (`language-textmate`), Monarch grammars (`language-monarch`) and Tree-sitter (`language-treesitter`).

## Decision

- `domain:editor` holds `SourceLanguage`, a small enum with the languages the IDE knows, and `SourceLanguage.forPath(path)` which picks one from the file name (case does not matter; a name that only starts with a dot, such as `.java`, has no extension). The mapping is pure and tested, and the editor module does not decide what a file is.
- `feature:editor` turns a `SourceLanguage` into a Sora `Language`. The first step knows `JAVA`, which uses `language-java` (highlighting, indent after braces, bracket and quote pairs, basic completion), and `PLAIN`, which uses Sora's empty language.
- The language is set before the text of a document, and is set again when a renamed file changes language (for example `A.txt` to `A.java`), without resetting the text.
- Only `feature:editor` depends on Sora, including `language-java` (ADR-0005).

## Consequences

- XML, Kotlin, Gradle (Groovy and Kotlin DSL), JSON, Markdown and properties files stay plain for now. They need TextMate or Monarch grammars with extra dependencies; the choice among them is a separate decision, made when the first of these languages is added, and it only changes the mapping in `feature:editor` and the enum.
- The Java language of `language-java` is a lexer, not a parser. It does not know about types or errors. Real analysis comes with the Java language server in v1.2.0.

## Open items

- Licence: Sora is LGPL-2.1. The IDE is Apache-2.0 and uses Sora as an unmodified library. A licence review before the first release must confirm what the notices must say and whether users must be able to replace the library.
- Choose the grammar engine for the other languages.
