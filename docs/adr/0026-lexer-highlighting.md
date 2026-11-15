# ADR-0026: Highlighting with our own lexers

Status: Proposed

## Context

ADR-0016 left the choice of a grammar engine for XML, JSON, Markdown and Gradle open. Sora offers TextMate (`language-textmate`, needs a native regular expression library and grammar and theme files) and Monarch (`language-monarch`, needs its own grammar libraries and two regular expression engines, one of them native). Both add dependencies and files that cannot be checked in the sandbox where most of the code is written, and both bring weight to every start of the app.

These four languages are small and regular. A hand written scanner colours them well, runs in a few milliseconds on a project file, and is plain Java that runs in unit tests.

## Decision

- `domain:editor` has the package `highlight` with `Token`, `TokenKind`, the `Lexer` interface and one lexer per language: `XmlLexer`, `JsonLexer` (comments allowed), `MarkdownLexer` and `GradleLexer` (Groovy and Kotlin DSL). `Lexers.forLanguage` finds the lexer of a `SourceLanguage`.
- A lexer takes the whole text and returns ordered tokens that do not overlap and may span lines. It never fails: text it does not understand, or a half typed construct, simply has no token, and a string or tag that is not closed ends with its line so that typing does not colour the rest of the file.
- `SourceLanguage` gains `XML`, `JSON`, `MARKDOWN` and `GRADLE` (`.xml`, `.json`, `.md` and `.markdown`, `.gradle` and `.gradle.kts`). Other `.kts` files stay plain.
- `feature:editor` has `LexerLanguage`, a Sora `EmptyLanguage` whose analyzer, `LexerAnalyzeManager` (a `SimpleAnalyzeManager`), runs the lexer on a background thread after every change and turns the tokens into Sora spans. The colour of each `TokenKind` comes from the colour scheme.
- Java keeps `language-java`.

## Consequences

- No new dependencies, and the lexers are covered by JVM tests.
- The whole text is analysed again after every change. That is fine for the files of a project; a very large file would need an incremental analyzer, which can replace `LexerAnalyzeManager` without changing the lexers.
- Highlighting only: no indent rules, bracket pairs or completion for these languages yet. XML completion comes with the XML language server in v1.4.0.
- The colours come from the standard scheme slots (keyword, comment, literal, HTML tag, attribute); a custom theme can change them later.
