package org.codirex.asm.data.environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.codirex.asm.domain.environment.EnvironmentException;
import org.codirex.asm.domain.environment.EnvironmentStore;
import org.codirex.asm.domain.environment.EnvironmentVariable;

/**
 * Keeps the variables in a {@code .properties} file in UTF-8. The terminal service reads the same
 * file when a session starts (ADR-0028), so the format is a contract: one {@code NAME=value} per
 * variable, in the format of {@link Properties}.
 */
final class FileEnvironmentStore implements EnvironmentStore {

  private final File file;

  FileEnvironmentStore(File file) {
    this.file = file;
  }

  @Override
  public List<EnvironmentVariable> load() throws IOException {
    List<EnvironmentVariable> result = new ArrayList<>();
    if (!file.isFile()) {
      return result;
    }
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    for (String name : properties.stringPropertyNames()) {
      try {
        result.add(new EnvironmentVariable(name, properties.getProperty(name)));
      } catch (EnvironmentException e) {
        // A hand-edited entry that breaks the rules is ignored, like the terminal ignores it.
      }
    }
    return result;
  }

  @Override
  public void save(List<EnvironmentVariable> variables) throws IOException {
    Properties properties = new Properties();
    for (EnvironmentVariable variable : variables) {
      properties.setProperty(variable.name(), variable.value());
    }
    File parent = file.getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("cannot create " + parent);
    }
    // Write beside the target, then move over it: a crash never leaves half a file.
    File temporary = new File(file.getPath() + ".tmp");
    try (Writer writer =
        new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
      properties.store(writer, "Custom environment variables of the terminal");
    }
    if (!temporary.renameTo(file)) {
      if (file.exists() && !file.delete()) {
        throw new IOException("cannot replace " + file);
      }
      if (!temporary.renameTo(file)) {
        throw new IOException("cannot move " + temporary + " to " + file);
      }
    }
  }
}
