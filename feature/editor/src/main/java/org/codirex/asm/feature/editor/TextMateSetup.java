package org.codirex.asm.feature.editor;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import org.codirex.asm.domain.editor.SourceLanguage;
import org.eclipse.tm4e.core.registry.IThemeSource;

/**
 * Loads the TextMate grammars and themes of the editor from the assets (ADR-0026) and creates the
 * Sora language of a {@link SourceLanguage}. Everything here runs on the main thread.
 */
final class TextMateSetup {

  private static final String TAG = "TextMateSetup";
  private static final String DARK_THEME = "darcula";
  private static final String LIGHT_THEME = "quietlight";

  private static boolean ready;

  private TextMateSetup() {}

  /**
   * Registers the assets and loads the themes and the grammar list. Does it once; returns whether
   * TextMate can be used.
   */
  static synchronized boolean ensure(Context context) {
    if (ready) {
      return true;
    }
    try {
      FileProviderRegistry registry = FileProviderRegistry.getInstance();
      registry.addFileProvider(new AssetsFileResolver(context.getApplicationContext().getAssets()));
      ThemeRegistry themes = ThemeRegistry.getInstance();
      for (String name : new String[] {DARK_THEME, LIGHT_THEME}) {
        String path = "textmate/" + name + ".json";
        ThemeModel model =
            new ThemeModel(IThemeSource.fromInputStream(registry.tryGetInputStream(path), path, null), name);
        model.setDark(name.equals(DARK_THEME));
        themes.loadTheme(model);
      }
      GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
      ready = true;
    } catch (Exception | LinkageError e) {
      Log.e(TAG, "TextMate could not be set up; files will be shown without colours", e);
    }
    return ready;
  }

  /** Makes the theme of the registry the dark or the light one. */
  static void useTheme(boolean night) {
    ThemeRegistry.getInstance().setTheme(night ? DARK_THEME : LIGHT_THEME);
  }

  /** The Sora language of {@code language}, or null (plain text) if it has none or it fails. */
  @Nullable
  static Language languageFor(SourceLanguage language) {
    String scope = scopeOf(language);
    if (scope == null || !ready) {
      return null;
    }
    try {
      return TextMateLanguage.create(scope, true);
    } catch (Exception | LinkageError e) {
      Log.e(TAG, "No TextMate language for " + scope, e);
      return null;
    }
  }

  @Nullable
  private static String scopeOf(SourceLanguage language) {
    switch (language) {
      case JAVA:
        return "source.java";
      case KOTLIN:
        return "source.kotlin";
      case GROOVY:
        return "source.groovy";
      case XML:
        return "text.xml";
      case JSON:
        return "source.json";
      case MARKDOWN:
        return "text.html.markdown";
      case HTML:
        return "text.html.basic";
      case JAVASCRIPT:
        return "source.js";
      case CSS:
        return "source.css";
      case PYTHON:
        return "source.python";
      case SHELL:
        return "source.shell";
      case PROPERTIES:
        return "source.ini";
      case YAML:
        return "source.yaml";
      case C:
        return "source.c";
      case CPP:
        return "source.cpp";
      case PLAIN:
      default:
        return null;
    }
  }
}
