"""One-off: wires the bottom dock into WorkspaceFragment and opens two MainActivity methods to it."""
import pathlib


def patch(path, pairs):
    p = pathlib.Path(path)
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert s.count(old) == 1, (path, old[:70])
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


DOCK = '''    binding.dockFiles.setOnClickListener(v -> showFiles());
    binding.dockBuild.setOnClickListener(
        v -> ((MainActivity) requireActivity()).showGradleTasks());
    binding.dockTerminal.setOnClickListener(v -> ((MainActivity) requireActivity()).showTerminal());
    // The dock gives its room back to the code while the keyboard is open.
    keyboardListener =
        () -> {
          if (binding == null) {
            return;
          }
          android.graphics.Rect frame = new android.graphics.Rect();
          binding.getRoot().getWindowVisibleDisplayFrame(frame);
          int covered = binding.getRoot().getRootView().getHeight() - frame.bottom;
          boolean keyboard = covered > Math.round(150 * getResources().getDisplayMetrics().density);
          int visibility = keyboard ? View.GONE : View.VISIBLE;
          binding.dock.setVisibility(visibility);
          binding.dockDivider.setVisibility(visibility);
        };
    binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);

'''

REMOVE = '''    if (keyboardListener != null) {
      android.view.ViewTreeObserver observer = binding.getRoot().getViewTreeObserver();
      if (observer.isAlive()) {
        observer.removeOnGlobalLayoutListener(keyboardListener);
      }
      keyboardListener = null;
    }
'''

patch("app/src/main/java/org/codirex/asm/WorkspaceFragment.java", [
    ("  private FragmentWorkspaceBinding binding;\n",
     "  private FragmentWorkspaceBinding binding;\n  private android.view.ViewTreeObserver.OnGlobalLayoutListener keyboardListener;\n"),
    ("    // The back button closes the drawer first", DOCK + "    // The back button closes the drawer first"),
    ("  public void onDestroyView() {\n    binding = null;", "  public void onDestroyView() {\n" + REMOVE + "    binding = null;"),
])
patch("app/src/main/java/org/codirex/asm/MainActivity.java", [
    ("  private void showGradleTasks() {", "  void showGradleTasks() {"),
    ("  private void showTerminal() {", "  void showTerminal() {"),
])
print("done")
