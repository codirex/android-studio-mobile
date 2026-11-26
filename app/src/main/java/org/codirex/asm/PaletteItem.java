package org.codirex.asm;

/** One row of the command palette: a command to run, or a file to open. */
final class PaletteItem {

  final String title;
  final String subtitle;

  /** What the typed text is matched against: the title of a command, the path of a file. */
  final String matchText;

  final int icon;
  final Runnable action;
  final boolean command;

  /** A command. */
  PaletteItem(String title, String subtitle, int icon, Runnable action) {
    this(title, subtitle, title, icon, action, true);
  }

  private PaletteItem(
      String title,
      String subtitle,
      String matchText,
      int icon,
      Runnable action,
      boolean command) {
    this.title = title;
    this.subtitle = subtitle;
    this.matchText = matchText;
    this.icon = icon;
    this.action = action;
    this.command = command;
  }

  /** A file: the name is the title, the folder the subtitle, the whole path what is matched. */
  static PaletteItem file(String name, String folder, String path, int icon, Runnable action) {
    return new PaletteItem(name, folder, path, icon, action, false);
  }
}
