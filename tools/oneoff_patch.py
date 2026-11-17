import json

def patch(path, edits):
    with open(path, encoding='utf-8') as f:
        s = f.read()
    for old, new in edits:
        assert s.count(old) == 1, (path, old)
        s = s.replace(old, new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(s)

patch('tools/vendor_grammars.py', [(
'    "ini/syntaxes/ini.tmLanguage.json": VS + "ini/syntaxes/ini.tmLanguage.json",\n}\n',
'''    "ini/syntaxes/ini.tmLanguage.json": VS + "ini/syntaxes/ini.tmLanguage.json",
    # YAML: the main grammar includes the 1.2 grammar and the embedded one, so all three are kept.
    "yaml/syntaxes/yaml.tmLanguage.json": VS + "yaml/syntaxes/yaml.tmLanguage.json",
    "yaml/syntaxes/yaml-1.2.tmLanguage.json": VS + "yaml/syntaxes/yaml-1.2.tmLanguage.json",
    "yaml/syntaxes/yaml-embedded.tmLanguage.json": VS + "yaml/syntaxes/yaml-embedded.tmLanguage.json",
    "yaml/language-configuration.json": VS + "yaml/language-configuration.json",
    # C and C++ share one language configuration; the C++ grammar includes the macro grammar.
    "cpp/syntaxes/c.tmLanguage.json": VS + "cpp/syntaxes/c.tmLanguage.json",
    "cpp/syntaxes/cpp.tmLanguage.json": VS + "cpp/syntaxes/cpp.tmLanguage.json",
    "cpp/syntaxes/cpp.embedded.macro.tmLanguage.json": VS + "cpp/syntaxes/cpp.embedded.macro.tmLanguage.json",
    "cpp/language-configuration.json": VS + "cpp/language-configuration.json",
}
''')])

patch('feature/editor/src/main/assets/textmate/languages.json', [(
'''      "grammar": "textmate/ini/syntaxes/ini.tmLanguage.json"
    }
  ]
}''',
'''      "grammar": "textmate/ini/syntaxes/ini.tmLanguage.json"
    },
    {
      "name": "yaml",
      "scopeName": "source.yaml",
      "grammar": "textmate/yaml/syntaxes/yaml.tmLanguage.json",
      "languageConfiguration": "textmate/yaml/language-configuration.json"
    },
    {
      "name": "yaml-1.2",
      "scopeName": "source.yaml.1.2",
      "grammar": "textmate/yaml/syntaxes/yaml-1.2.tmLanguage.json"
    },
    {
      "name": "yaml-embedded",
      "scopeName": "source.yaml.embedded",
      "grammar": "textmate/yaml/syntaxes/yaml-embedded.tmLanguage.json"
    },
    {
      "name": "c",
      "scopeName": "source.c",
      "grammar": "textmate/cpp/syntaxes/c.tmLanguage.json",
      "languageConfiguration": "textmate/cpp/language-configuration.json"
    },
    {
      "name": "cpp",
      "scopeName": "source.cpp",
      "grammar": "textmate/cpp/syntaxes/cpp.tmLanguage.json",
      "languageConfiguration": "textmate/cpp/language-configuration.json"
    },
    {
      "name": "cpp-embedded-macro",
      "scopeName": "source.cpp.embedded.macro",
      "grammar": "textmate/cpp/syntaxes/cpp.embedded.macro.tmLanguage.json"
    }
  ]
}''')])
json.load(open('feature/editor/src/main/assets/textmate/languages.json', encoding='utf-8'))

patch('feature/editor/src/main/assets/textmate/NOTICE.md', [(
'- `groovy`, `json`, `css`, `shellscript`, `ini`: the built-in',
'- `groovy`, `json`, `css`, `shellscript`, `ini`, `yaml`, `cpp` (the C and C++ grammars): the built-in')])

patch('domain/editor/src/main/java/org/codirex/asm/domain/editor/SourceLanguage.java', [
('  PROPERTIES;\n', '  PROPERTIES,\n  YAML,\n  /** C sources and headers ({@code .h} is C). */\n  C,\n  /** C++ sources and headers. */\n  CPP;\n'),
('      default:\n        return PLAIN;\n',
 '      case "yaml":\n      case "yml":\n        return YAML;\n'
 '      case "c":\n      case "h":\n        return C;\n'
 '      case "cpp":\n      case "cc":\n      case "cxx":\n      case "hpp":\n      case "hh":\n      case "hxx":\n        return CPP;\n'
 '      default:\n        return PLAIN;\n'),
])

patch('feature/editor/src/main/java/org/codirex/asm/feature/editor/TextMateSetup.java', [
('      case PLAIN:\n      default:\n        return null;\n',
 '      case YAML:\n        return "source.yaml";\n      case C:\n        return "source.c";\n      case CPP:\n        return "source.cpp";\n'
 '      case PLAIN:\n      default:\n        return null;\n'),
])
