package org.codirex.asm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.databinding.DialogPaletteBinding;
import org.codirex.asm.databinding.ItemPaletteRowBinding;
import org.codirex.asm.domain.palette.FileIndex;
import org.codirex.asm.domain.palette.FuzzyMatcher;

/**
 * The command palette: one field to search the files of the open project and the commands of the
 * IDE (ADR-0035). Typing {@code >} first limits the search to commands. An empty field lists the
 * commands. The host (the activity) says what the commands are, so the palette needs no feature.
 */
public final class CommandPaletteFragment extends DialogFragment {

  /** What the palette needs from the screen that shows it. */
  interface Host {
    List<PaletteItem> paletteCommands();

    File paletteProjectDirectory();

    void paletteOpenFile(String relativePath);
  }

  private static final int MAX_RESULTS = 40;
  private static final int MAX_FILES = 5000;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private DialogPaletteBinding binding;
  private PaletteAdapter adapter;
  private List<PaletteItem> commands = Collections.emptyList();
  private List<PaletteItem> fileItems = Collections.emptyList();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.Theme_Asm_Palette);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = DialogPaletteBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    final Host host = (Host) requireActivity();
    commands = host.paletteCommands();
    adapter = new PaletteAdapter(getLayoutInflater());
    binding.paletteList.setAdapter(adapter);
    binding.paletteList.setOnItemClickListener((parent, row, position, id) -> run(position));
    // A tap outside the panel closes the palette; the panel itself swallows its taps.
    binding.paletteRoot.setOnClickListener(v -> dismissAllowingStateLoss());
    binding.palettePanel.setOnClickListener(v -> {});
    binding.paletteQuery.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            refresh();
          }
        });
    binding.paletteQuery.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
            run(0);
            return true;
          }
          return false;
        });
    binding.paletteQuery.requestFocus();
    loadFiles(host);
    refresh();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getDialog() != null) {
      Window window = getDialog().getWindow();
      if (window != null) {
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
      }
    }
  }

  @Override
  public void onDestroyView() {
    binding = null;
    adapter = null;
    super.onDestroyView();
  }

  /** Runs the item at a position of the list, if there is one, and closes the palette. */
  private void run(int position) {
    if (adapter == null || position < 0 || position >= adapter.getCount()) {
      return;
    }
    PaletteItem item = (PaletteItem) adapter.getItem(position);
    dismissAllowingStateLoss();
    item.action.run();
  }

  /** Lists the files of the project on a worker thread; the palette shows them when they are in. */
  private void loadFiles(final Host host) {
    final File root = host.paletteProjectDirectory();
    if (root == null) {
      return;
    }
    new Thread(
            () -> {
              List<String> paths = FileIndex.list(root, MAX_FILES);
              List<PaletteItem> built = new ArrayList<>(paths.size());
              for (final String path : paths) {
                int slash = path.lastIndexOf('/');
                String name = path.substring(slash + 1);
                String folder = slash < 0 ? "" : path.substring(0, slash);
                built.add(
                    PaletteItem.file(
                        name,
                        folder,
                        path,
                        R.drawable.ic_palette_file,
                        () -> host.paletteOpenFile(path)));
              }
              handler.post(
                  () -> {
                    if (binding != null) {
                      fileItems = built;
                      refresh();
                    }
                  });
            },
            "palette-files")
        .start();
  }

  /** Shows what matches the field: commands only after a {@code >}, otherwise files and commands. */
  private void refresh() {
    if (binding == null) {
      return;
    }
    String query = binding.paletteQuery.getText().toString().trim();
    List<PaletteItem> shown;
    if (query.startsWith(">")) {
      shown = FuzzyMatcher.rank(query.substring(1).trim(), commands, item -> item.matchText, MAX_RESULTS);
    } else if (query.isEmpty()) {
      shown = FuzzyMatcher.rank("", commands, item -> item.matchText, MAX_RESULTS);
    } else {
      List<PaletteItem> all = new ArrayList<>(commands.size() + fileItems.size());
      all.addAll(commands);
      all.addAll(fileItems);
      shown = FuzzyMatcher.rank(query, all, item -> item.matchText, MAX_RESULTS);
    }
    adapter.setItems(shown);
    binding.paletteEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
  }

  /** The rows of the palette. */
  private static final class PaletteAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private List<PaletteItem> items = Collections.emptyList();

    PaletteAdapter(LayoutInflater inflater) {
      this.inflater = inflater;
    }

    void setItems(List<PaletteItem> items) {
      this.items = items;
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public Object getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ItemPaletteRowBinding row;
      if (convertView == null) {
        row = ItemPaletteRowBinding.inflate(inflater, parent, false);
        row.getRoot().setTag(row);
      } else {
        row = (ItemPaletteRowBinding) convertView.getTag();
      }
      PaletteItem item = items.get(position);
      row.rowIcon.setImageResource(item.icon);
      row.rowTitle.setText(item.title);
      row.rowSubtitle.setText(item.subtitle);
      row.rowSubtitle.setVisibility(item.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
      return row.getRoot();
    }
  }
}
