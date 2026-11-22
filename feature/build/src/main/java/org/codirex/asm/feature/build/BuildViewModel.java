package org.codirex.asm.feature.build;

import android.app.Application;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.codirex.asm.domain.toolchain.GradleOutputParser;
import org.codirex.asm.terminal.protocol.ITerminalService;
import org.codirex.asm.terminal.view.TerminalConnection;

/**
 * Runs one Gradle task through the terminal service and turns its output into {@link BuildState}
 * (ADR-0032). The build lives as long as the screen: leaving it stops the build.
 */
public final class BuildViewModel extends AndroidViewModel {

  private static final int TAIL_LINES = 40;
  private static final long PUBLISH_INTERVAL_MS = 150;

  private final MutableLiveData<BuildState> state = new MutableLiveData<>();
  private final ExecutorService reader = Executors.newSingleThreadExecutor();
  private final TerminalConnection connection;
  private final Object lock = new Object();

  private String directory;
  private String task;
  private boolean started;
  private boolean stopped;
  private ParcelFileDescriptor output;

  public BuildViewModel(@NonNull Application application) {
    super(application);
    connection =
        new TerminalConnection(
            application,
            new TerminalConnection.Listener() {
              @Override
              public void onConnected(ITerminalService service) {
                reader.execute(() -> run(service));
              }

              @Override
              public void onDisconnected() {
                // The service process died, and the build with it.
                stop();
              }
            });
  }

  LiveData<BuildState> state() {
    return state;
  }

  /** Starts the build once; later calls (a rotated screen) change nothing. */
  void start(String projectDirectory, String gradleTask) {
    if (started) {
      return;
    }
    started = true;
    directory = projectDirectory;
    task = gradleTask;
    state.setValue(
        new BuildState(new GradleOutputParser().report(), new ArrayList<String>(), false, ""));
    connection.connect();
  }

  /** Stops the build: closing the output is how the service is told (ITerminalService.runGradle). */
  void stop() {
    synchronized (lock) {
      stopped = true;
      closeOutput();
    }
  }

  private void closeOutput() {
    if (output != null) {
      try {
        output.close();
      } catch (IOException ignored) {
        // Already closed.
      }
      output = null;
    }
  }

  private void run(ITerminalService service) {
    GradleOutputParser parser = new GradleOutputParser();
    List<String> tail = new ArrayList<>();
    String error = "";
    try {
      ParcelFileDescriptor pfd = service.runGradle(directory, task);
      synchronized (lock) {
        if (stopped) {
          pfd.close();
          publish(parser, tail, true, "");
          return;
        }
        output = pfd;
      }
      BufferedReader in =
          new BufferedReader(
              new InputStreamReader(
                  new ParcelFileDescriptor.AutoCloseInputStream(pfd), StandardCharsets.UTF_8));
      long last = 0;
      String line;
      while ((line = in.readLine()) != null) {
        parser.accept(line);
        tail.add(line);
        if (tail.size() > TAIL_LINES) {
          tail.remove(0);
        }
        long now = System.currentTimeMillis();
        if (now - last >= PUBLISH_INTERVAL_MS) {
          last = now;
          publish(parser, tail, false, "");
        }
      }
    } catch (IOException e) {
      // Closed by stop(), or the service went away: the output simply ends.
    } catch (Exception e) {
      error = String.valueOf(e.getMessage());
    }
    synchronized (lock) {
      closeOutput();
    }
    publish(parser, tail, true, error);
  }

  private void publish(GradleOutputParser parser, List<String> tail, boolean finished, String error) {
    state.postValue(new BuildState(parser.report(), new ArrayList<>(tail), finished, error));
  }

  @Override
  protected void onCleared() {
    stop();
    connection.disconnect();
    reader.shutdown();
  }
}
