# Fonts

The app bundles two font families in `core/ui/src/main/res/font`. Both are under the SIL Open Font
License 1.1; the licence texts are next to this file.

| Family | Used for | Source | Licence |
| --- | --- | --- | --- |
| Inter 4.1 (Regular, Medium, SemiBold) | all interface text | https://github.com/rsms/inter | `OFL-Inter.txt` |
| JetBrains Mono 2.304 (Regular, Medium, Bold) | code in the editor, build output, status line | https://github.com/JetBrains/JetBrainsMono | `OFL-JetBrainsMono.txt` |

The files are fetched from the projects' own releases by a workflow, not edited. The terminal view
still draws with the platform monospace face.
