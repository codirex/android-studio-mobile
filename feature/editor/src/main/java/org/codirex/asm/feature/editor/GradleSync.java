package org.codirex.asm.feature.editor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.codirex.asm.terminal.protocol.ITerminalService;
import org.codirex.asm.terminal.view.TerminalConnection;

/**
 * The Gradle sync of a project (ADR-0037): runs the task {@code asmSync} through the terminal
 * service. The task comes from an init script in the user home and writes {@code .project} and
 * {@code .classpath} files per module, which the Java language server reads to know the Android
 * framework and the libraries of the project. The sync is judged done by what the script prints, and
 * remembered by a stamp file that later syncs are compared with the Gradle files against.
 */
final class GradleSync {

  /** Called on a worker thread. */
  interface Listener {
    void onFinished(boolean ok, String message);
  }

  private static final String TAG = "GradleSync";
  private static final String ASSET = "asm-sync.gradle";
  private static final String TASK = "asmSync";
  private static final long CONNECT_TIMEOUT_SECONDS = 30;

  private static final Set<String> RUNNING = new HashSet<>();
  private static final List<String> SKIPPED =
      Arrays.asList(".git", ".gradle", ".idea", ".asm", ".cxx", "build", "node_modules");
  private static final List<String> INPUTS =
      Arrays.asList(
          "build.gradle",
          "build.gradle.kts",
          "settings.gradle",
          "settings.gradle.kts",
          "gradle.properties",
          "libs.versions.toml");

  private GradleSync() {}

  /** Whether a sync of this project is running now. */
  static boolean isRunning(File project) {
    synchronized (RUNNING) {
      return project != null && RUNNING.contains(project.getAbsolutePath());
    }
  }

  /** Whether the project is a Gradle project that has not been synced since its Gradle files changed. */
  static boolean needed(File project) {
    boolean gradle =
        new File(project, "build.gradle").isFile()
            || new File(project, "build.gradle.kts").isFile()
            || new File(project, "settings.gradle").isFile()
            || new File(project, "settings.gradle.kts").isFile();
    if (!gradle) {
      return false;
    }
    File stamp = new File(project, ".asm/sync.stamp");
    return !stamp.isFile() || newestInput(project, 0) > stamp.lastModified();
  }

  private static long newestInput(File dir, int depth) {
    long newest = 0;
    File[] children = dir.listFiles();
    if (children == null || depth > 3) {
      return newest;
    }
    for (File child : children) {
      if (child.isDirectory()) {
        if (!SKIPPED.contains(child.getName())) {
          newest = Math.max(newest, newestInput(child, depth + 1));
        }
      } else if (INPUTS.contains(child.getName())) {
        newest = Math.max(newest, child.lastModified());
      }
    }
    return newest;
  }

  /** Starts the sync on a worker thread; does nothing if one runs already. */
  static void start(Context context, final File project, final Listener listener) {
    final String key = project.getAbsolutePath();
    synchronized (RUNNING) {
      if (!RUNNING.add(key)) {
        return;
      }
    }
    final Context app = context.getApplicationContext();
    new Thread(
            () -> {
              Result result = new Result();
              try {
                run(app, project, result);
              } catch (Throwable e) {
                Log.w(TAG, "the sync failed", e);
                result.message = String.valueOf(e.getMessage());
              } finally {
                synchronized (RUNNING) {
                  RUNNING.remove(key);
                }
              }
              listener.onFinished(result.ok, result.message);
            },
            "gradle-sync")
        .start();
  }

  private static final class Result {
    boolean ok;
    String message = "";
  }

  private static void run(Context app, File project, Result result) throws Exception {
    installScript(app);
    final ITerminalService[] service = new ITerminalService[1];
    final CountDownLatch connected = new CountDownLatch(1);
    final TerminalConnection connection =
        new TerminalConnection(
            app,
            new TerminalConnection.Listener() {
              @Override
              public void onConnected(ITerminalService connectedService) {
                service[0] = connectedService;
                connected.countDown();
              }

              @Override
              public void onDisconnected() {
                connected.countDown();
              }
            });
    Handler main = new Handler(Looper.getMainLooper());
    main.post(connection::connect);
    try {
      if (!connected.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS) || service[0] == null) {
        result.message = "the terminal service is not available";
        return;
      }
      read(service[0], project, result);
    } finally {
      main.post(connection::disconnect);
    }
  }

  private static void read(ITerminalService service, File project, Result result)
      throws IOException {
    ParcelFileDescriptor pfd;
    try {
      pfd = service.runGradle(project.getPath(), TASK);
    } catch (IllegalStateException | IllegalArgumentException e) {
      // Gradle is not installed, or the folder is not a project folder: nothing ran, so no stamp.
      result.message = String.valueOf(e.getMessage());
      return;
    } catch (RemoteException e) {
      result.message = "the terminal service stopped";
      return;
    }
    boolean wrote = false;
    boolean succeeded = false;
    List<String> tail = new ArrayList<>();
    try (BufferedReader in =
        new BufferedReader(
            new InputStreamReader(
                new ParcelFileDescriptor.AutoCloseInputStream(pfd), StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("ASM_SYNC_OK")) {
          wrote = true;
        } else if (line.startsWith("BUILD SUCCESSFUL")) {
          succeeded = true;
        }
        if (line.startsWith("ASM_SYNC")) {
          Log.i(TAG, line);
        }
        if (!line.trim().isEmpty()) {
          tail.add(line);
          if (tail.size() > 6) {
            tail.remove(0);
          }
        }
      }
    }
    result.ok = wrote && succeeded;
    if (!result.ok) {
      result.message = tail.isEmpty() ? "no output" : tail.get(tail.size() - 1);
      Log.w(TAG, "sync output ended with: " + tail);
    }
    writeStamp(project, result.ok);
  }

  private static void writeStamp(File project, boolean ok) {
    File folder = new File(project, ".asm");
    if (!folder.isDirectory() && !folder.mkdirs()) {
      return;
    }
    try (FileOutputStream out = new FileOutputStream(new File(folder, "sync.stamp"))) {
      out.write((ok ? "ok\n" : "failed\n").getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      Log.w(TAG, "cannot write the stamp", e);
    }
  }

  /** Puts the init script into the user home of the userland, if it is not there as it should be. */
  private static void installScript(Context app) throws IOException {
    byte[] wanted = readAll(app.getAssets().open(ASSET));
    File target = new File(app.getFilesDir(), "home/.gradle/init.d/asm-sync.gradle");
    if (target.isFile() && Arrays.equals(wanted, readAll(new FileInputStream(target)))) {
      return;
    }
    File folder = target.getParentFile();
    if (!folder.isDirectory() && !folder.mkdirs()) {
      throw new IOException("cannot create " + folder);
    }
    try (FileOutputStream out = new FileOutputStream(target)) {
      out.write(wanted);
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    try (InputStream stream = in) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = stream.read(buffer)) > 0) {
        bytes.write(buffer, 0, read);
      }
      return bytes.toByteArray();
    }
  }
}
