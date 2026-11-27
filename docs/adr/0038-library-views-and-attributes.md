# ADR-0038: Library views and `app:` attributes in layouts

Status: Accepted

## Context

ADR-0036 completes layouts from the framework's attrs.xml and the project's resources. A layout also
uses views and attributes of libraries (ConstraintLayout, Material, AppCompat) that the framework
does not know: `app:layout_constraintTop_toTopOf`, `app:cornerRadius`, `<com.google...MaterialButton>`.

## Decision

- The Gradle sync (ADR-0037) writes `<module>/.asm/libs.txt`: `res <folder>` for the resource folder of
  every library (artifact type `android-res`, plus `src/main/res` of the module and of project
  dependencies) and `jar <file>` for every classes jar.
- `LibraryIndex` (domain:xmlassist) reads the `values/*.xml` of the folders (declare-styleable and
  attr, framework mentions skipped, bare mentions resolved across libraries) and finds the views: a
  jar class whose simple name is a styleable (`ConstraintLayout_Layout` counts for `ConstraintLayout`).
- A tag takes the styleable of its simple name, the `AppCompat`/`Material` versions of a framework
  widget, and, for a child, the `<Parent>_Layout` styleables. `app:` is assumed for the namespace.
- `LibraryCache` keeps one index per module until `libs.txt` changes. A layout of a module that was not
  synced gets what it got before.

## Consequences

- Custom views of the project itself are not proposed as tags (their classes are not in a jar); their
  attributes are, once their `attrs.xml` is in `src/main/res`.
- A styleable that does not follow the naming convention (view class and styleable named differently)
  is missed. `tools:` and other namespaces are not completed.
- Needs a sync after a dependency changes, like the Java classpath.
