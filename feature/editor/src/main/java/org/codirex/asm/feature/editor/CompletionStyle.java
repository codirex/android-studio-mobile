package org.codirex.asm.feature.editor;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * The look of the proposals popup (the design of the redesign, ADR-0034): a raised card with a
 * hairline border, one row per proposal with a coloured badge for its kind, the label in the code
 * font with the typed letters in the accent colour, and the type or signature on the right.
 */
final class CompletionStyle {

  private CompletionStyle() {}

  /** Gives the popup the colours of the app theme; call after the colour scheme is set. */
  static void colors(Context context, EditorColorScheme scheme) {
    scheme.setColor(
        EditorColorScheme.COMPLETION_WND_BACKGROUND,
        themeColor(context, "colorSurfaceContainerHigh", 0xFF161B22));
    scheme.setColor(
        EditorColorScheme.COMPLETION_WND_CORNER,
        themeColor(context, "colorOutlineVariant", 0xFF2A313B));
    scheme.setColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT, Color.TRANSPARENT);
    scheme.setColor(
        EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY,
        themeColor(context, "colorOnSurface", 0xFFE6EDF3));
    scheme.setColor(
        EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY,
        themeColor(context, "colorOnSurfaceVariant", 0xFF8B949E));
  }

  /** Replaces the rows of the popup. */
  static void install(CodeEditor editor) {
    editor.getComponent(EditorAutoCompletion.class).setAdapter(new Adapter());
  }

  private static int themeColor(Context context, String attribute, int fallback) {
    int id = context.getResources().getIdentifier(attribute, "attr", context.getPackageName());
    if (id == 0) {
      return fallback;
    }
    TypedValue value = new TypedValue();
    return context.getTheme().resolveAttribute(id, value, true) && value.type >= 0x1c
        ? value.data
        : fallback;
  }

  private static int dp(Resources resources, float value) {
    return (int)
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, resources.getDisplayMetrics());
  }

  /** The badge of a kind of the protocol: a letter and a colour. */
  static String letter(int kind) {
    switch (kind) {
      case 2:
      case 3:
      case 4:
        return "m";
      case 5:
      case 10:
        return "f";
      case 6:
        return "v";
      case 7:
      case 22:
        return "C";
      case 8:
        return "I";
      case 13:
      case 20:
        return "E";
      case 14:
        return "k";
      case 15:
        return "s";
      case 21:
        return "c";
      case 25:
        return "T";
      case 12:
        return "=";
      default:
        return "·";
    }
  }

  static int color(int kind) {
    switch (kind) {
      case 2:
      case 3:
      case 4:
        return 0xFF61AFEF;
      case 5:
      case 6:
      case 10:
      case 21:
        return 0xFF3DDC84;
      case 7:
      case 8:
      case 13:
      case 22:
        return 0xFFE5A84B;
      case 14:
      case 15:
        return 0xFFC792EA;
      case 12:
      case 20:
        return 0xFF4DD0E1;
      default:
        return 0xFF8B949E;
    }
  }

  private static final class Adapter extends EditorCompletionAdapter {

    @Override
    public int getItemHeight() {
      return dp(getContext().getResources(), 40);
    }

    @Override
    protected View getView(int position, View convertView, ViewGroup parent, boolean current) {
      Row row = convertView instanceof Row ? (Row) convertView : new Row(getContext());
      row.bind(
          getItem(position),
          current,
          getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY),
          getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY));
      return row;
    }
  }

  private static final class Row extends LinearLayout {

    private final TextView badge;
    private final TextView label;
    private final TextView detail;
    private final Typeface mono;

    Row(@NonNull Context context) {
      super(context);
      Resources resources = context.getResources();
      setOrientation(HORIZONTAL);
      setGravity(Gravity.CENTER_VERTICAL);
      setLayoutParams(new ViewGroup.LayoutParams(-1, dp(resources, 40)));
      setPadding(dp(resources, 10), 0, dp(resources, 12), 0);
      mono = ResourcesCompat.getFont(context, org.codirex.asm.core.ui.R.font.jetbrains_mono_regular);

      badge = new TextView(context);
      badge.setGravity(Gravity.CENTER);
      badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      badge.setTypeface(mono, Typeface.BOLD);
      addView(badge, new LayoutParams(dp(resources, 22), dp(resources, 22)));

      label = new TextView(context);
      label.setSingleLine(true);
      label.setEllipsize(TextUtils.TruncateAt.END);
      label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
      label.setTypeface(mono);
      LayoutParams labelParams = new LayoutParams(0, -2, 1f);
      labelParams.setMarginStart(dp(resources, 10));
      addView(label, labelParams);

      detail = new TextView(context);
      detail.setSingleLine(true);
      detail.setEllipsize(TextUtils.TruncateAt.START);
      detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
      detail.setTypeface(mono);
      detail.setMaxWidth(dp(resources, 150));
      LayoutParams detailParams = new LayoutParams(-2, -2);
      detailParams.setMarginStart(dp(resources, 12));
      addView(detail, detailParams);
    }

    void bind(CompletionItem item, boolean current, int primary, int secondary) {
      Resources resources = getResources();
      int kind = item instanceof CaretCompletionItem ? ((CaretCompletionItem) item).kind() : 0;
      String typed = item instanceof CaretCompletionItem ? ((CaretCompletionItem) item).typed() : "";
      int accent = color(kind);

      GradientDrawable chip = new GradientDrawable();
      chip.setCornerRadius(dp(resources, 6));
      chip.setColor(Color.argb(46, Color.red(accent), Color.green(accent), Color.blue(accent)));
      badge.setBackground(chip);
      badge.setTextColor(accent);
      badge.setText(letter(kind));

      label.setText(highlight(item.label, typed, primary, 0xFF3DDC84, item.deprecated));
      detail.setText(item.desc == null ? "" : item.desc);
      detail.setTextColor(secondary);

      if (current) {
        GradientDrawable fill = new GradientDrawable();
        fill.setCornerRadius(dp(resources, 8));
        fill.setColor(Color.argb(38, Color.red(0x3DDC84), Color.green(0x3DDC84), Color.blue(0x3DDC84)));
        setBackground(
            new InsetDrawable(
                fill, dp(resources, 4), dp(resources, 2), dp(resources, 4), dp(resources, 2)));
      } else {
        setBackground(null);
      }
    }

    /** The label with the typed letters, in order, in the accent colour and bold. */
    private static CharSequence highlight(
        CharSequence text, String typed, int primary, int accent, boolean deprecated) {
      SpannableString out = new SpannableString(text == null ? "" : text);
      out.setSpan(
          new ForegroundColorSpan(primary), 0, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      String lower = out.toString().toLowerCase(java.util.Locale.ROOT);
      String want = typed.toLowerCase(java.util.Locale.ROOT);
      int at = 0;
      for (int i = 0; i < want.length(); i++) {
        int found = lower.indexOf(want.charAt(i), at);
        if (found < 0) {
          break;
        }
        out.setSpan(
            new ForegroundColorSpan(accent), found, found + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(
            new StyleSpan(Typeface.BOLD), found, found + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        at = found + 1;
      }
      if (deprecated) {
        out.setSpan(
            new android.text.style.StrikethroughSpan(),
            0,
            out.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      return out;
    }
  }
}
