package org.codirex.asm.feature.editor;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.codirex.asm.domain.lsp.CompletionItem;
import org.codirex.asm.domain.lsp.Diagnostic;
import org.codirex.asm.domain.lsp.DocumentSymbol;
import org.codirex.asm.domain.lsp.LanguageSession;
import org.codirex.asm.domain.lsp.Location;
import org.codirex.asm.domain.lsp.TextEdit;
import org.codirex.asm.domain.lsp.WorkspaceSymbol;
import org.codirex.asm.terminal.protocol.ITerminalService;
import org.codirex.asm.terminal.view.TerminalConnection;

/**
 * The language server of one language of one project (ADR-0031, ADR-0033: jdt.ls for Java, LemMinX for XML), as the editor uses it: it connects to the
 * terminal service, asks it to run jdt.ls, keeps the open documents in step and answers
 * completion. All methods may be called from any thread and never block for long, except {@link
 * #complete}, which waits for the server for a few seconds on the caller's (worker) thread.
 */
final class JavaLanguageServer {

  private static final String TAG = "JavaLanguageServer";
  private static final long COMPLETION_TIMEOUT_SECONDS = 4;
  private static final long ANSWER_TIMEOUT_SECONDS = 10;

  enum State {
    /** Not started. */
    IDLE,
    /** Connecting, or the server is starting up. */
    STARTING,
    /** Documents can be opened and completion asked. */
    READY,
    /** The language server package is not installed. */
    MISSING,
    /** It could not start or its connection broke. */
    FAILED
  }

  /** Reports, on some background thread. */
  interface Listener {
    void onState(State state);

    void onDiagnostics(String uri, List<Diagnostic> diagnostics);
  }

  private final Context context;
  private final String rootUri;
  private final String workspaceKey;
  private final String language;
  private volatile Listener listener;
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread thread = new Thread(r, "java-language-server");
            thread.setDaemon(true);
            return thread;
          });

  /** Guards the fields below. */
  private final Object lock = new Object();

  private State state = State.IDLE;
  private TerminalConnection connection;
  private LanguageSession session;
  private final Map<String, String> wanted = new HashMap<>();

  /** Serialises sending, so that a document's versions go out in order. */
  private final Object syncLock = new Object();

  private final Map<String, String> sent = new HashMap<>();
  private final Map<String, Integer> versions = new HashMap<>();

  /** @param language the LSP language id the service knows: {@code java} or {@code xml} */
  JavaLanguageServer(
      Context context, File projectDirectory, String workspaceKey, String language) {
    this.context = context.getApplicationContext();
    this.rootUri = uriOf(projectDirectory);
    this.workspaceKey = workspaceKey;
    this.language = language;
  }

  /** The {@code file:///...} address of a file. */
  static String uriOf(File file) {
    String path = file.getAbsolutePath().replace(File.separatorChar, '/');
    StringBuilder uri = new StringBuilder("file://");
    for (int i = 0; i < path.length(); i++) {
      char c = path.charAt(i);
      boolean plain =
          (c >= 'a' && c <= 'z')
              || (c >= 'A' && c <= 'Z')
              || (c >= '0' && c <= '9')
              || "/-._~".indexOf(c) >= 0;
      if (plain) {
        uri.append(c);
      } else {
        for (byte b : String.valueOf(c).getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
          uri.append('%').append(String.format("%02X", b & 0xff));
        }
      }
    }
    return uri.toString();
  }

  /**
   * What jdt.ls is told at the start. It does not import Gradle or Maven builds: the IDE builds in
   * the terminal, and the import would cost memory and fetch dependencies over the network.
   */
  static Map<String, Object> initializationOptions() {
    Map<String, Object> gradle = new LinkedHashMap<>();
    gradle.put("enabled", false);
    Map<String, Object> maven = new LinkedHashMap<>();
    maven.put("enabled", false);
    Map<String, Object> imports = new LinkedHashMap<>();
    imports.put("gradle", gradle);
    imports.put("maven", maven);
    Map<String, Object> java = new LinkedHashMap<>();
    java.put("import", imports);
    Map<String, Object> completion = new LinkedHashMap<>();
    completion.put("enabled", true);
    completion.put("overwrite", true);
    completion.put("maxResults", 0);
    completion.put("guessMethodArguments", "off");
    completion.put("matchCase", "off");
    java.put("completion", completion);
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("java", java);
    Map<String, Object> options = new LinkedHashMap<>();
    options.put("settings", settings);
    return options;
  }

  /** Who hears of state and diagnostics; the screen sets it when it appears and clears it. */
  void setListener(Listener listener) {
    this.listener = listener;
  }

  private void notifyState(State state) {
    Listener current = listener;
    if (current != null) {
      current.onState(state);
    }
  }

  private void notifyDiagnostics(String uri, List<Diagnostic> found) {
    Listener current = listener;
    if (current != null) {
      current.onDiagnostics(uri, found);
    }
  }

  State state() {
    synchronized (lock) {
      return state;
    }
  }

  /** Starts the server if it is not running. Call on the main thread. */
  void start() {
    synchronized (lock) {
      if (state == State.STARTING || state == State.READY) {
        return;
      }
      state = State.STARTING;
    }
    notifyState(State.STARTING);
    TerminalConnection created =
        new TerminalConnection(
            context,
            new TerminalConnection.Listener() {
              @Override
              public void onConnected(ITerminalService service) {
                executor.execute(() -> open(service));
              }

              @Override
              public void onDisconnected() {
                fail(State.FAILED);
              }
            });
    synchronized (lock) {
      connection = created;
    }
    created.connect();
  }

  /** Ends the server and lets go of the service. */
  void stop() {
    LanguageSession ending;
    TerminalConnection leaving;
    synchronized (lock) {
      ending = session;
      leaving = connection;
      session = null;
      connection = null;
      state = State.IDLE;
    }
    synchronized (syncLock) {
      sent.clear();
      versions.clear();
    }
    if (ending != null) {
      ending.shutdown();
    }
    if (leaving != null) {
      leaving.disconnect();
    }
    executor.shutdown();
  }

  /** The text of a document is now this: open it, or send the change. */
  void documentChanged(String uri, String text) {
    synchronized (lock) {
      wanted.put(uri, text);
      if (state != State.READY) {
        return;
      }
    }
    executor.execute(() -> sync(uri));
  }

  /** A document is no longer shown. */
  void documentClosed(String uri) {
    synchronized (lock) {
      wanted.remove(uri);
    }
    executor.execute(
        () -> {
          LanguageSession current;
          synchronized (lock) {
            current = session;
          }
          boolean wasOpen;
          synchronized (syncLock) {
            wasOpen = sent.remove(uri) != null;
            versions.remove(uri);
          }
          if (current != null && wasOpen) {
            try {
              current.close(uri);
            } catch (IOException ignored) {
              // The connection ended; the state follows from the read loop.
            }
          }
        });
  }

  /**
   * The proposals at a position of a document, or null if the server is not ready or does not
   * answer in time. The server first gets the text as it is now.
   */
  List<CompletionItem> complete(String uri, String text, int line, int column) {
    LanguageSession current;
    synchronized (lock) {
      if (state != State.READY || session == null) {
        return null;
      }
      current = session;
      wanted.put(uri, text);
    }
    sync(uri);
    try {
      return current.completion(uri, line, column).get(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Asks the server something about a document, after it has the text as it is now. Null if the
   * server is not ready or does not answer in time. Waits, so call it from a worker thread.
   */
  private <T> T ask(
      String uri,
      String text,
      java.util.function.Function<LanguageSession, java.util.concurrent.CompletableFuture<T>>
          question) {
    LanguageSession current;
    synchronized (lock) {
      if (state != State.READY || session == null) {
        return null;
      }
      current = session;
      wanted.put(uri, text);
    }
    sync(uri);
    try {
      return question.apply(current).get(ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /** What the server says about the symbol at a position (zero based): text, empty, or null. */
  String hover(String uri, String text, int line, int column) {
    return ask(uri, text, s -> s.hover(uri, line, column));
  }

  /** Where the symbol at a position (zero based) is defined; null if the server did not answer. */
  List<Location> definition(String uri, String text, int line, int column) {
    return ask(uri, text, s -> s.definition(uri, line, column));
  }

  /** The edits that format the document, last position first; null if the server did not answer. */
  List<TextEdit> format(String uri, String text, int tabSize) {
    return ask(uri, text, s -> s.format(uri, tabSize));
  }

  /** The symbols of the document in the order they are declared; null if the server did not answer. */
  List<DocumentSymbol> documentSymbols(String uri, String text) {
    return ask(uri, text, s -> s.documentSymbols(uri));
  }

  /** The symbols of the whole workspace that fit a query; null if the server did not answer. */
  List<WorkspaceSymbol> workspaceSymbols(String uri, String text, String query) {
    return ask(uri, text, s -> s.workspaceSymbols(query));
  }

  // ---- inside ----

  private void open(ITerminalService service) {
    ParcelFileDescriptor descriptor;
    try {
      descriptor = service.openLanguageServer(language, workspaceKey);
    } catch (IllegalStateException e) {
      Log.w(TAG, "the server did not start: " + e.getMessage());
      boolean missing = e.getMessage() != null && e.getMessage().contains("not installed");
      fail(missing ? State.MISSING : State.FAILED);
      return;
    } catch (RemoteException | RuntimeException e) {
      Log.w(TAG, "the terminal service failed", e);
      fail(State.FAILED);
      return;
    }
    final LanguageSession created =
        new LanguageSession(
            new ParcelFileDescriptor.AutoCloseInputStream(descriptor),
            new ParcelFileDescriptor.AutoCloseOutputStream(descriptor),
            rootUri,
            "java".equals(language) ? initializationOptions() : null,
            this::notifyDiagnostics);
    synchronized (lock) {
      if (state != State.STARTING) {
        created.abort();
        return;
      }
      session = created;
    }
    Thread reader =
        new Thread(
            () -> {
              created.runReadLoop();
              boolean current;
              synchronized (lock) {
                current = session == created;
              }
              if (current) {
                fail(State.FAILED);
              }
            },
            "java-language-server-reader");
    reader.setDaemon(true);
    reader.start();
    created
        .start()
        .whenComplete(
            (ignored, failure) -> {
              if (failure != null) {
                Log.w(TAG, "the handshake failed", failure);
                fail(State.FAILED);
                return;
              }
              synchronized (lock) {
                if (session != created) {
                  return;
                }
                state = State.READY;
              }
              notifyState(State.READY);
              synchronized (lock) {
                for (String uri : wanted.keySet()) {
                  executor.execute(() -> sync(uri));
                }
              }
            });
  }

  /** Sends the wanted text of a document if the server does not have it yet. */
  private void sync(String uri) {
    LanguageSession current;
    String text;
    synchronized (lock) {
      current = session;
      text = wanted.get(uri);
      if (state != State.READY || current == null || text == null) {
        return;
      }
    }
    synchronized (syncLock) {
      String old = sent.get(uri);
      try {
        if (old == null) {
          current.open(uri, language, 1, text);
          versions.put(uri, 1);
        } else if (!old.equals(text)) {
          int version = versions.get(uri) + 1;
          current.change(uri, version, text);
          versions.put(uri, version);
        }
        sent.put(uri, text);
      } catch (IOException e) {
        Log.w(TAG, "could not send " + uri, e);
      }
    }
  }

  private void fail(State to) {
    LanguageSession ending;
    TerminalConnection leaving;
    synchronized (lock) {
      if (state == State.IDLE) {
        return;
      }
      ending = session;
      leaving = connection;
      session = null;
      connection = null;
      state = to;
    }
    synchronized (syncLock) {
      sent.clear();
      versions.clear();
    }
    if (ending != null) {
      ending.abort();
    }
    if (leaving != null) {
      leaving.disconnect();
    }
    notifyState(to);
  }
}
