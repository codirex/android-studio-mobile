# ADR-0021: Custom templates

Status: Proposed

## Context

ADR-0017 left the template manager open: the user should create, edit and delete templates. The built-in templates are code. Custom templates are data the user owns and can break, so they need checks before they are stored and a way into the same create flow.

## Decision

- `domain:template` gets the port `CustomTemplateStore` (load, save, delete). A data module will keep each template as a folder under the app's private files; that module is the next step.
- `TemplateValidator` runs before every save: the id is 1 to 64 characters of `a-z`, `0-9`, `.`, `_`, `-` and starts with a letter or digit; the name is 1 to 80 characters; variable names are a letter followed by letters, digits or `_`, and must not shadow the variables every project sets; at most 500 files and 2,000,000 characters; and the template is rendered once with a sample project, so an unknown placeholder, an open `${` or an unsafe path fails when the template is saved, not when a project is created. Two files that render to the same path are refused.
- `TemplateManager` is the use case: `duplicate(source, name)` copies any template, a built-in one too, into a custom template whose id is `custom-` plus a slug of the name, numbered when taken (also against the built-in ids); `save` and `delete` accept only ids that start with `custom-`, so a built-in template can never be overwritten or removed.
- `LayeredTemplateCatalog` is the `TemplateCatalog` the create screen will use: built-in templates first (the first stays the default), then custom templates of the type. A custom template with an id already taken is left out. A store that cannot be read gives only the built-in templates.
- `TemplateException.Reason` gets `INVALID`.

## Consequences

- Starting from a copy avoids a blank editor and keeps custom templates valid by construction. Creating one from scratch is the same call with an empty template, and comes with the editor screen.
- The custom ids all start with `custom-`, so the prefix is part of the stored format.
- The validator renders with one sample project; a template that fails only for an unusual name or package is not caught.

## Open items

- The folder format and `data:template` implementation of `CustomTemplateStore`, bound in Hilt behind `LayeredTemplateCatalog`.
- The template manager screens: list, copy, rename, edit files and variables, delete; export and import.
- Localized names for built-in templates.
