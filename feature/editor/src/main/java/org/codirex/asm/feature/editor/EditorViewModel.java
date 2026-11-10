package org.codirex.asm.feature.editor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.codirex.asm.core.concurrency.RealmFactory;
import org.codirex.asm.core.concurrency.RealmViewModel;
import org.codirex.asm.core.concurrency.UiStateHolder;
import org.codirex.asm.domain.editor.EditorTabs;
import org.codirex.asm.domain.editor.ProjectDocuments;
import org.codirex.asm.domain.editor.WorkspaceStore;
import org.codirex.asm.domain.project.ProjectName;

/**
 * Opens, tracks and saves the documents of one project. All methods are called on the main thread;
 * files are read and written in background jobs. The project name comes from the fragment
 * arguments.
 *
 * <p>The text of every open document is kept here, as last loaded or handed over by {@link
 * #snapshot}. The editor widget holds the live text of the shown document only. The open tabs are
 * restored from the project when this view model is created, and saved after every change (ADR-0018).
 */
@HiltViewModel
final class EditorViewModel extends RealmViewModel {

  /** The fragment argument that carries the project name. */
  static final String ARG_PROJECT = "project";

  private final ProjectDocuments documents;
  private final WorkspaceStore workspace;
  private final ProjectName project;
  private final UiStateHolder<EditorState> state = new UiStateHolder<>(EditorState.initial());

  /** The text of each open document that is loaded. */
  private final Map<String, String> buffers = new HashMap<>();

  /** How many edits each open document has had, to know if a finished save is up to date. */
  private final Map<String, Integer> edits = new HashMap<>();

  /** Documents with a save running, and the newest text waiting behind it. */
  private final Set<String> saving = new HashSet<>();

  private final Map<String, String> queued = new HashMap<>();

  /** Whether a save of the tab list is running, and the newest list waiting behind it. */
  private boolean savingTabs;

  private EditorTabs queuedTabs;

  @Inject
  EditorViewModel(
      RealmFactory factory,
      ProjectDocuments documents,
      WorkspaceStore workspace,
      SavedStateHandle arguments) {
    super(factory);
    this.documents = documents;
    this.workspace = workspace;
    String name = arguments.get(ARG_PROJECT);
    this.project = ProjectName.of(name);
    restoreTabs();
  }

  LiveData<EditorState> state() {
    return state.observable();
  }

  /** Opens a file in a tab, or shows its tab if it is open already. */
  void open(String path) {
    EditorState s = state.get();
    boolean alreadyOpen = s.tabs().isOpen(path);
    boolean wasActive = path.equals(s.tabs().activePath());
    s = s.withTabs(s.tabs().open(path));
    if (!alreadyOpen) {
      s = s.loadingStarted(path);
    }
    if (!wasActive) {
      s = show(s, path);
    }
    setState(s);
    if (!alreadyOpen) {
      load(path, false);
    }
  }

  /** Shows the tab of an open file. */
  void select(String path) {
    EditorState s = state.get();
    if (!s.tabs().isOpen(path) || path.equals(s.tabs().activePath())) {
      return;
    }
    setState(show(s.withTabs(s.tabs().select(path)), path));
  }

  /** Closes a tab. The screen hands over the text with {@link #snapshot} first, which saves it. */
  void close(String path) {
    EditorState s = state.get();
    if (s.tabs().isOpen(path)) {
      setState(removeTab(s, path));
    }
  }

  /**
   * Closes the tab of a deleted file, and of every file inside a deleted folder, without saving: the
   * text must not bring the files back.
   */
  void discardUnder(String path) {
    EditorState s = state.get();
    String prefix = path + "/";
    boolean changed = false;
    for (String open : new ArrayList<>(s.tabs().paths())) {
      if (open.equals(path) || open.startsWith(prefix)) {
        queued.remove(open);
        s = removeTab(s, open);
        changed = true;
      }
    }
    if (changed) {
      setState(s);
    }
  }

  /**
   * {@code from} was renamed to {@code to} on disk: the tabs of that file, or of the files inside
   * that folder, follow it with their text and unsaved marks. A document that was still loading is
   * loaded again from its new path.
   */
  void renameUnder(String from, String to) {
    EditorState before = state.get();
    List<String> reload = new ArrayList<>();
    for (String path : before.tabs().paths()) {
      String moved = EditorTabs.remapPath(path, from, to);
      if (!moved.equals(path) && before.isLoading(path)) {
        reload.add(moved);
      }
    }
    remapKeys(buffers, from, to);
    remapKeys(edits, from, to);
    remapKeys(queued, from, to);
    setState(before.renamed(from, to));
    for (String path : reload) {
      load(path, false);
    }
  }

  /** The user changed the shown document. Cheap: it only marks the tab. */
  void onEdited(String path) {
    EditorState s = state.get();
    if (!s.tabs().isOpen(path) || s.isLoading(path)) {
      return;
    }
    edits.put(path, edits.containsKey(path) ? edits.get(path) + 1 : 1);
    if (!s.isDirty(path)) {
      setState(s.dirtied(path));
    }
  }

  /**
   * The screen hands over the live text of a document. It is remembered, and saved if the document
   * has unsaved changes.
   */
  void snapshot(String path, String text) {
    EditorState s = state.get();
    // A document that is still loading shows a placeholder, which must not overwrite anything.
    if (!s.tabs().isOpen(path) || s.isLoading(path)) {
      return;
    }
    buffers.put(path, text);
    if (s.isDirty(path)) {
      save(path, text);
    }
  }

  /** The screen showed the error; do not show it again. */
  void errorHandled() {
    setState(state.get().errorHandled());
  }

  /**
   * Publishes a new state. When the tab list changed, the new list is saved to the project, so the
   * next time the project is opened it shows the same tabs.
   */
  private void setState(EditorState next) {
    EditorTabs before = state.get().tabs();
    state.set(next);
    if (!next.tabs().equals(before)) {
      saveTabs(next.tabs());
    }
  }

  /** Saves of the tab list never overlap; while one runs only the newest list waits. */
  private void saveTabs(EditorTabs tabs) {
    if (savingTabs) {
      queuedTabs = tabs;
      return;
    }
    savingTabs = true;
    this.<Boolean>runAsync(
        checkpoint -> {
          workspace.save(project, tabs);
          return Boolean.TRUE;
        },
        outcome -> {
          // A failed save is ignored: the next change of the tabs saves again.
          savingTabs = false;
          EditorTabs next = queuedTabs;
          queuedTabs = null;
          if (next != null) {
            saveTabs(next);
          }
        });
  }

  /**
   * Reads the saved tabs and opens them, unless the user opened something in the meantime. A file
   * that cannot be opened any more is dropped without a message.
   */
  private void restoreTabs() {
    this.<EditorTabs>runAsync(
        checkpoint -> workspace.load(project),
        outcome -> {
          EditorTabs saved = outcome.isSuccess() ? outcome.valueOrNull() : null;
          EditorState s = state.get();
          if (saved == null || saved.paths().isEmpty() || !s.tabs().paths().isEmpty()) {
            return;
          }
          s = s.withTabs(saved);
          for (String path : saved.paths()) {
            s = s.loadingStarted(path);
          }
          setState(show(s, saved.activePath()));
          for (String path : saved.paths()) {
            load(path, true);
          }
        });
  }

  private static <V> void remapKeys(Map<String, V> map, String from, String to) {
    Map<String, V> moved = new HashMap<>();
    for (Map.Entry<String, V> entry : map.entrySet()) {
      moved.put(EditorTabs.remapPath(entry.getKey(), from, to), entry.getValue());
    }
    map.clear();
    map.putAll(moved);
  }

  private EditorState show(EditorState s, String path) {
    if (path == null) {
      return s.shown(null, "");
    }
    String text = buffers.get(path);
    return s.shown(path, text == null ? "" : text);
  }

  private EditorState removeTab(EditorState s, String path) {
    boolean wasActive = path.equals(s.tabs().activePath());
    buffers.remove(path);
    edits.remove(path);
    EditorState result = s.removed(path);
    if (wasActive) {
      result = show(result, result.tabs().activePath());
    }
    return result;
  }

  /**
   * Reads a document in the background. A failure closes its tab; it is reported to the user unless
   * {@code quiet}, which is the case for tabs restored from an earlier session.
   */
  private void load(String path, boolean quiet) {
    this.<String>runAsync(
        checkpoint -> documents.read(project, path),
        outcome -> {
          EditorState s = state.get();
          if (!s.tabs().isOpen(path)) {
            return;
          }
          if (outcome.isSuccess()) {
            String text = outcome.valueOrNull();
            buffers.put(path, text);
            s = s.loadingDone(path);
            if (path.equals(s.tabs().activePath())) {
              s = s.shown(path, text);
            }
            setState(s);
          } else if (outcome.isFailure()) {
            EditorState closed = removeTab(s, path);
            setState(quiet ? closed : closed.failed(Failures.reasonOf(outcome.failureOrNull())));
          } else {
            setState(removeTab(s, path));
          }
        });
  }

  /** Writes the text in the background; saves of one file run one after the other. */
  private void save(String path, String text) {
    if (saving.contains(path)) {
      queued.put(path, text);
      return;
    }
    saving.add(path);
    final int version = edits.containsKey(path) ? edits.get(path) : 0;
    this.<Boolean>runAsync(
        checkpoint -> {
          documents.write(project, path, text);
          return Boolean.TRUE;
        },
        outcome -> {
          saving.remove(path);
          EditorState s = state.get();
          if (outcome.isSuccess()) {
            int now = edits.containsKey(path) ? edits.get(path) : 0;
            // Clean only if nothing was typed while the write ran.
            if (now == version && s.isDirty(path)) {
              setState(s.cleaned(path));
            }
          } else if (outcome.isFailure()) {
            setState(s.failed(Failures.reasonOf(outcome.failureOrNull())));
          }
          String next = queued.remove(path);
          if (next != null) {
            save(path, next);
          }
        });
  }
}
