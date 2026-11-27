# ADR-0036: Android XML completion

Status: Accepted

## Context

LemMinX (ADR-0033) completes XML from schemas it can find; Android layouts have none, so it knows
neither the view tags nor the `android:` attributes. The framework describes them in attrs.xml of
every SDK platform.

## Decision

1. `domain:xmlassist` (pure Java, JVM tests) reads attrs.xml into styleables (`AttrCatalog`), knows
   which widget extends which (`ViewClasses`), finds where the caret is in a start tag (`XmlContext`)
   and proposes tags, `android:` attributes and values (`AndroidXmlCompleter`).
2. Attributes of a tag are those of its class and superclasses; `layout_*` attributes are those of
   the parent layout (`LinearLayout_Layout`...) and of ViewGroup. Attributes the tag already has are
   left out. An attribute is inserted as `android:name=""` with the caret between the quotes.
3. The editor (`feature:editor`) reads attrs.xml of the newest platform in `files/sdk/platforms` (the
   SDK manager installs it) and applies this only to files under `res/layout*`. Its proposals come
   first, the language server's follow; without the server they stand alone.

## Consequences

No platform installed means no proposals of this kind. `app:` attributes and library views are not
proposed yet.

## Resources of the project

`ResourceIndex` lists the names of the project's own resources: values files (`string`, `color`,
`dimen`, `style`...), the file names in the other `res` folders (`drawable`, `layout`...) and the ids
that layouts use. Value completion offers `@type/` roots and, after the slash, those names for any
attribute that accepts a reference, string, color or dimension. The editor reads the index again when
it is five seconds old; only `src/main/res` folders of the project count, not libraries or the
framework (`@android:`).
