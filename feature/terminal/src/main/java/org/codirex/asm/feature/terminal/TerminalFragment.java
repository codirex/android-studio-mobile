package org.codirex.asm.feature.terminal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.codirex.asm.feature.terminal.databinding.FragmentTerminalBinding;
import org.codirex.asm.terminal.protocol.IInstallListener;
import org.codirex.asm.terminal.protocol.ITerminalService;
import org.codirex.asm.terminal.view.TerminalConnection;
import org.codirex.asm.terminal.view.TerminalView;

/**
 * A terminal screen: installs the userland on first use, then shows shell sessions in the given
 * directory, one per tab. The sessions live in the terminal service, so leaving the screen does not
 * end them; the next visit shows them again.
 */
public final class TerminalFragment extends Fragment {

  private static final String ARG_DIRECTORY = "directory";

  /** A terminal that starts in {@code directory}. */
  public static TerminalFragment newInstance(String directory) {
    TerminalFragment fragment = new TerminalFragment();
    Bundle args = new Bundle();
    args.putString(ARG_DIRECTORY, directory);
    fragment.setArguments(args);
    return fragment;
  }

  private final Handler main = new Handler(Looper.getMainLooper());
  private ExecutorService calls;
  private FragmentTerminalBinding binding;
  private ExtraKeys extraKeys;
  private TerminalConnection connection;
  private ITerminalService service;
  private int lastRows = 24;
  private int lastColumns = 80;
  private int currentSession = -1;
  private final List<Integer> sessionIds = new ArrayList<>();
  private boolean updatingTabs;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
    binding = FragmentTerminalBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
    calls = Executors.newSingleThreadExecutor();
    extraKeys = new ExtraKeys(binding.extraKeys, binding.terminal);
    binding.terminal.setCallbacks(
        new TerminalView.Callbacks() {
          @Override
          public void onTitleChanged(String title) {}

          @Override
          public void onBell() {}

          @Override
          public void onSessionExited(int exitCode) {
            if (binding != null) {
              binding.endedText.setText(getString(R.string.terminal_session_ended, exitCode));
              binding.endedBanner.setVisibility(View.VISIBLE);
            }
          }

          @Override
          public void onCopied() {
            if (binding != null) {
              android.widget.Toast.makeText(
                      requireContext(), R.string.terminal_copied, android.widget.Toast.LENGTH_SHORT)
                  .show();
            }
          }

          @Override
          public void onModifiersChanged(boolean ctrlLatched, boolean altLatched) {
            if (extraKeys != null) {
              extraKeys.showModifiers(ctrlLatched, altLatched);
            }
          }
        });
    binding.newSession.setOnClickListener(v -> loadSessions(currentSession, true));
    binding.addSession.setOnClickListener(v -> loadSessions(-1, true));
    binding.closeSession.setOnClickListener(v -> loadSessions(currentSession, false));
    binding.sessionTabs.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            if (!updatingTabs && position >= 0 && position < sessionIds.size()) {
              showSession(sessionIds.get(position));
            }
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {}

          @Override
          public void onTabReselected(TabLayout.Tab tab) {}
        });
    connection =
        new TerminalConnection(
            requireContext(),
            new TerminalConnection.Listener() {
              @Override
              public void onConnected(ITerminalService connected) {
                service = connected;
                begin();
              }

              @Override
              public void onDisconnected() {
                service = null;
                if (binding != null) {
                  showMessage(getString(R.string.terminal_service_lost));
                  binding.installAction.setText(R.string.terminal_retry);
                  binding.installAction.setOnClickListener(v -> reconnect());
                  binding.installAction.setVisibility(View.VISIBLE);
                  binding.installProgress.setVisibility(View.GONE);
                }
              }
            });
    showMessage(getString(R.string.terminal_preparing));
    connection.connect();
  }

  private void reconnect() {
    connection.disconnect();
    showMessage(getString(R.string.terminal_preparing));
    connection.connect();
  }

  /** Installs the userland if needed, then opens or reuses a session. */
  private void begin() {
    final ITerminalService target = service;
    calls.execute(
        () -> {
          try {
            if (target.isUserlandInstalled()) {
              ui(this::startSession);
            } else {
              ui(this::install);
            }
          } catch (RemoteException e) {
            ui(() -> fail(getString(R.string.terminal_start_failed)));
          }
        });
  }

  private void install() {
    final ITerminalService target = service;
    if (target == null || binding == null) {
      return;
    }
    showMessage(getString(R.string.terminal_preparing));
    binding.installProgress.setVisibility(View.VISIBLE);
    binding.installProgress.setIndeterminate(true);
    binding.installAction.setVisibility(View.VISIBLE);
    binding.installAction.setText(R.string.terminal_cancel);
    binding.installAction.setOnClickListener(v -> cancelInstall());
    calls.execute(
        () -> {
          try {
            target.installUserland(
                new IInstallListener.Stub() {
                  @Override
                  public void onProgress(int stage, long done, long total) {
                    ui(() -> showProgress(stage, done, total));
                  }

                  @Override
                  public void onDone() {
                    ui(TerminalFragment.this::startSession);
                  }

                  @Override
                  public void onFailed(String reason, String message) {
                    ui(() -> failInstall(reason, message));
                  }
                });
          } catch (RemoteException e) {
            ui(() -> fail(getString(R.string.terminal_start_failed)));
          }
        });
  }

  private void cancelInstall() {
    final ITerminalService target = service;
    if (target == null) {
      return;
    }
    calls.execute(
        () -> {
          try {
            target.cancelInstall();
          } catch (RemoteException e) {
            // The service is gone; onDisconnected tells the person.
          }
        });
  }

  private void showProgress(int stage, long done, long total) {
    if (binding == null) {
      return;
    }
    if (stage == 0) {
      int percent = InstallProgress.percent(done, total);
      String size = InstallProgress.megabytes(done);
      if (percent < 0) {
        binding.installMessage.setText(getString(R.string.terminal_downloading_size, size));
        binding.installProgress.setIndeterminate(true);
      } else {
        binding.installMessage.setText(
            getString(R.string.terminal_downloading_percent, percent, size));
        binding.installProgress.setIndeterminate(false);
        binding.installProgress.setProgress(percent);
      }
    } else {
      binding.installMessage.setText(R.string.terminal_extracting);
      binding.installProgress.setIndeterminate(true);
    }
  }

  private void failInstall(String reason, String message) {
    if (binding == null) {
      return;
    }
    int text;
    switch (reason == null ? "" : reason) {
      case "NETWORK":
        text = R.string.terminal_error_network;
        break;
      case "CHECKSUM":
        text = R.string.terminal_error_checksum;
        break;
      case "UNAVAILABLE":
        text = R.string.terminal_error_unavailable;
        break;
      case "INVALID_ARCHIVE":
        text = R.string.terminal_error_invalid_archive;
        break;
      case "STORAGE":
        text = R.string.terminal_error_storage;
        break;
      case "CANCELLED":
        text = R.string.terminal_error_cancelled;
        break;
      default:
        binding.installMessage.setText(getString(R.string.terminal_error_unknown, message));
        text = 0;
        break;
    }
    showPanel();
    if (text != 0) {
      binding.installMessage.setText(text);
    }
    binding.installProgress.setVisibility(View.GONE);
    binding.installAction.setText(R.string.terminal_retry);
    binding.installAction.setOnClickListener(v -> install());
    binding.installAction.setVisibility(View.VISIBLE);
  }

  /** Shows the sessions of the service, opening one if there is none. */
  private void startSession() {
    loadSessions(-1, false);
  }

  /**
   * Refreshes the tabs from the service. {@code closeId} (if not negative) is closed first;
   * {@code forceNew} opens a new session; a new session is also opened when none is left.
   */
  private void loadSessions(final int closeId, final boolean forceNew) {
    final ITerminalService target = service;
    if (target == null || binding == null) {
      return;
    }
    final String directory = requireArguments().getString(ARG_DIRECTORY, "");
    showTerminal();
    binding.terminal.post(
        () -> {
          if (binding == null) {
            return;
          }
          final int rows = binding.terminal.rows() > 0 ? binding.terminal.rows() : lastRows;
          final int columns = binding.terminal.columns() > 0 ? binding.terminal.columns() : lastColumns;
          lastRows = rows;
          lastColumns = columns;
          calls.execute(
              () -> {
                try {
                  if (closeId >= 0) {
                    target.closeSession(closeId);
                  }
                  int[] ids = target.sessionIds();
                  int select = -1;
                  if (forceNew || ids.length == 0) {
                    select = target.openSession(directory, rows, columns);
                    ids = target.sessionIds();
                  } else {
                    for (int id : ids) {
                      if (id == currentSession) {
                        select = id;
                      }
                    }
                    if (select < 0) {
                      select = ids[ids.length - 1];
                    }
                  }
                  final int[] shown = ids;
                  final int chosen = select;
                  ui(() -> showSessions(target, shown, chosen));
                } catch (RemoteException | RuntimeException e) {
                  ui(() -> fail(getString(R.string.terminal_start_failed)));
                }
              });
        });
  }

  /** Rebuilds the tabs and shows the chosen session. */
  private void showSessions(ITerminalService target, int[] ids, int selected) {
    if (binding == null || service != target) {
      return;
    }
    updatingTabs = true;
    sessionIds.clear();
    binding.sessionTabs.removeAllTabs();
    for (int i = 0; i < ids.length; i++) {
      sessionIds.add(ids[i]);
      TabLayout.Tab tab = binding.sessionTabs.newTab();
      tab.setText(getString(R.string.terminal_tab_title, i + 1));
      binding.sessionTabs.addTab(tab, ids[i] == selected);
    }
    updatingTabs = false;
    showSession(selected);
  }

  /** Attaches the view to a session of the service. */
  private void showSession(int id) {
    if (binding == null || service == null) {
      return;
    }
    binding.endedBanner.setVisibility(View.GONE);
    attach(service, id);
  }

  private void attach(ITerminalService target, int id) {
    if (binding == null || service != target) {
      return;
    }
    currentSession = id;
    binding.terminal.attach(target, id);
    binding.terminal.requestFocus();
  }

  private void fail(String text) {
    if (binding == null) {
      return;
    }
    showMessage(text);
    binding.installProgress.setVisibility(View.GONE);
    binding.installAction.setText(R.string.terminal_retry);
    binding.installAction.setOnClickListener(v -> reconnect());
    binding.installAction.setVisibility(View.VISIBLE);
  }

  private void showMessage(String text) {
    if (binding == null) {
      return;
    }
    showPanel();
    binding.installMessage.setText(text);
    binding.installProgress.setVisibility(View.VISIBLE);
    binding.installProgress.setIndeterminate(true);
    binding.installAction.setVisibility(View.GONE);
  }

  private void showPanel() {
    binding.installPanel.setVisibility(View.VISIBLE);
    binding.tabsBar.setVisibility(View.GONE);
    binding.terminal.setVisibility(View.GONE);
    binding.extraKeysScroll.setVisibility(View.GONE);
  }

  private void showTerminal() {
    binding.installPanel.setVisibility(View.GONE);
    binding.tabsBar.setVisibility(View.VISIBLE);
    binding.terminal.setVisibility(View.VISIBLE);
    binding.extraKeysScroll.setVisibility(View.VISIBLE);
  }

  /** Runs on the main thread, unless the view is gone by then. */
  private void ui(Runnable action) {
    main.post(
        () -> {
          if (binding != null) {
            action.run();
          }
        });
  }

  @Override
  public void onDestroyView() {
    // The session keeps running in the service; only this screen lets go of it.
    binding.terminal.detach();
    connection.disconnect();
    calls.shutdown();
    service = null;
    extraKeys = null;
    binding = null;
    super.onDestroyView();
  }
}
