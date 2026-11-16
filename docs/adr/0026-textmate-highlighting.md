# ADR-0026: Highlighting with TextMate grammars

Status: Proposed

## Context

ADR-0016 left the choice of a grammar engine open. Sora offers a ready Java language (`language-java`, a hand written lexer), TextMate (`language-textmate`), Monarch and Tree-sitter. The IDE is meant to handle many languages well, so highlighting should come from the same grammars that VS Code uses, not from one scanner per language.

## Decision

- Highlighting is done by `language-textmate`, Sora's TextMate engine, with the native regular expression engine of `oniguruma-native` (it declares it as `compileOnly`, so `feature:editor` depends on both).
- Grammars, language configurations and the two themes (`darcula` for the dark UI, `quietlight` for the light one) are assets of `feature:editor` under `textmate/`. `textmate/languages.json` lists the languages: Java, Kotlin (also `build.gradle.kts`), Groovy (also `build.gradle`), XML, JSON, Markdown, HTML, JavaScript, CSS, Python, shell and properties/ini. Markdown and HTML embed the others.
- The grammar files are third party data. `tools/vendor_grammars.py` names each source by an exact commit of `microsoft/vscode` or the tag of the Sora sample app, and the "Vendor grammars" workflow downloads and commits them, so builds need no network for them. To add a language: add its files to the script and an entry to `languages.json`, and run the workflow.
- `domain:editor` keeps `SourceLanguage` and the mapping from a file name to it. It does not know TextMate. `feature:editor` has `TextMateSetup`, which loads the registries once (grammars and themes, on first use of the editor), maps a `SourceLanguage` to a scope name and creates the Sora language. If TextMate cannot be set up, or a language fails to load, the file is shown as plain text and the error is logged.
- The colour scheme of the editor is a `TextMateColorScheme` that follows the theme of the registry, chosen by the night mode of the system when the editor opens.
- `language-java` is no longer used; Java is highlighted by its TextMate grammar like the rest. Indentation and brackets come from the language configuration of each language, and words in the file are offered as completion (`collectIdentifiers`). Real analysis of Java comes with the language server in v1.2.0.

## Consequences

- Many languages with one mechanism; a new one is a grammar file and a line of JSON.
- The APK carries the grammars (about 1 MB) and the native regular expression library.
- Grammars are loaded on the main thread the first time a language is used. If that is noticeable on a device, the load moves to a background thread.
- Licences: the grammars come from VS Code (MIT) and, through the Sora sample, from TextMate bundles and VS Code extensions with their own permissive licences; the header of each file names its origin. `assets/textmate/NOTICE.md` lists them. A licence review before the first release must confirm the notices, together with the review of Sora itself (LGPL-2.1, ADR-0016).
- YAML, C and C++ are not included yet: the VS Code YAML grammar is split into several files, and the C++ grammar is very large. They can be added with the steps above.
