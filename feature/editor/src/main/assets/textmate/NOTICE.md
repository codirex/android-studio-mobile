# Third party grammars and themes

The files in this folder are not written by this project. They are downloaded by
`tools/vendor_grammars.py` from the sources below, each at a fixed commit or tag.

- `groovy`, `json`, `css`, `shellscript`, `ini`: the built-in extensions of
  [microsoft/vscode](https://github.com/microsoft/vscode) (MIT licence, Copyright Microsoft
  Corporation). Several of them were converted from TextMate bundles and name the original in the
  `information_for_contributors` header of the file.
- `java`, `kotlin`, `python`, `xml`, `html`, `javascript`, `markdown`, `darcula.json` and
  `quietlight.json`: the assets of the sample app of
  [Rosemoe/sora-editor](https://github.com/Rosemoe/sora-editor) 0.24.7, which took them from VS Code
  and TextMate bundles. See the headers of the files and the upstream repositories for their licences.

A licence review is due before the first release (ADR-0026).
