#!/usr/bin/env python3
"""Downloads the TextMate grammars, language configurations and themes of the editor (ADR-0026).

The files are third party data. Every URL names an exact commit or tag, so the result is the same
every time. Run it through the "Vendor grammars" workflow, which commits what it downloads.
"""
import json
import pathlib
import sys
import time
import urllib.request

VSCODE = "0fbab1451fed17c969feb57b0ab596adf895ba0b"  # microsoft/vscode, MIT
SORA = "0.24.7"  # Rosemoe/sora-editor sample assets (grammars and themes taken from VS Code)

VS = f"https://raw.githubusercontent.com/microsoft/vscode/{VSCODE}/extensions/"
SO = f"https://raw.githubusercontent.com/Rosemoe/sora-editor/{SORA}/app/src/main/assets/textmate/"
ROOT = pathlib.Path(__file__).resolve().parent.parent / "feature/editor/src/main/assets/textmate"

# destination (below the textmate folder) -> source URL
FILES = {
    # Languages whose files the Sora sample app already carries in the layout we use.
    "java/syntaxes/java.tmLanguage.json": SO + "java/syntaxes/java.tmLanguage.json",
    "java/language-configuration.json": SO + "java/language-configuration.json",
    "kotlin/syntaxes/Kotlin.tmLanguage": SO + "kotlin/syntaxes/Kotlin.tmLanguage",
    "kotlin/language-configuration.json": SO + "kotlin/language-configuration.json",
    "python/syntaxes/python.tmLanguage.json": SO + "python/syntaxes/python.tmLanguage.json",
    "python/language-configuration.json": SO + "python/language-configuration.json",
    "xml/syntaxes/xml.tmLanguage.json": SO + "xml/syntaxes/xml.tmLanguage.json",
    "xml/language-configuration.json": SO + "xml/language-configuration.json",
    "html/syntaxes/html.tmLanguage.json": SO + "html/syntaxes/html.tmLanguage.json",
    "html/language-configuration.json": SO + "html/language-configuration.json",
    "javascript/syntaxes/JavaScript.tmLanguage.json": SO + "javascript/syntaxes/JavaScript.tmLanguage.json",
    "javascript/language-configuration.json": SO + "javascript/language-configuration.json",
    "markdown/syntaxes/markdown.tmLanguage.json": SO + "markdown/syntaxes/markdown.tmLanguage.json",
    "markdown/language-configuration.json": SO + "markdown/language-configuration.json",
    # Themes.
    "darcula.json": SO + "darcula.json",
    "quietlight.json": SO + "quietlight.json",
    # Languages taken from VS Code.
    "groovy/syntaxes/groovy.tmLanguage.json": VS + "groovy/syntaxes/groovy.tmLanguage.json",
    "groovy/language-configuration.json": VS + "groovy/language-configuration.json",
    "json/syntaxes/JSON.tmLanguage.json": VS + "json/syntaxes/JSON.tmLanguage.json",
    "json/language-configuration.json": VS + "json/language-configuration.json",
    "css/syntaxes/css.tmLanguage.json": VS + "css/syntaxes/css.tmLanguage.json",
    "css/language-configuration.json": VS + "css/language-configuration.json",
    "shellscript/syntaxes/shell-unix-bash.tmLanguage.json": VS + "shellscript/syntaxes/shell-unix-bash.tmLanguage.json",
    "shellscript/language-configuration.json": VS + "shellscript/language-configuration.json",
    "ini/syntaxes/ini.tmLanguage.json": VS + "ini/syntaxes/ini.tmLanguage.json",
}


def download(url):
    for attempt in range(1, 4):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                return response.read()
        except Exception as error:  # noqa: BLE001 - any failure is retried, then reported
            print(f"  attempt {attempt} failed: {error}")
            time.sleep(5 * attempt)
    raise SystemExit(f"Could not download {url}")


def main():
    for destination, url in FILES.items():
        print(f"{destination}  <-  {url}")
        data = download(url)
        if destination.endswith(".json"):
            # VS Code language configurations and grammars may have comments; themes and
            # grammars of Sora do not. Only check that the file is not an error page.
            if data.lstrip()[:1] not in (b"{", b"["):
                raise SystemExit(f"{destination} does not look like JSON")
        path = ROOT / destination
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    languages = json.loads((ROOT / "languages.json").read_text(encoding="utf-8"))
    missing = []
    for language in languages["languages"]:
        for key in ("grammar", "languageConfiguration"):
            if key in language and not (ROOT.parent / language[key]).exists():
                missing.append(language[key])
    if missing:
        print("languages.json refers to files that are not downloaded:", *missing, sep="\n  ")
        return 1
    print(f"Done: {len(FILES)} files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
