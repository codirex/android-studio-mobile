package org.codirex.asm.data.template;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileEntry;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.CustomTemplateStore;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateException;
import org.codirex.asm.domain.template.TemplateException.Reason;
import org.codirex.asm.domain.template.TemplateFile;
import org.codirex.asm.domain.template.TemplateValidator;

/**
 * Keeps each custom template in its own folder named by the template id (ADR-0021). The folder
 * holds {@code template.properties} (the format version, the name, the project type, the default
 * variables as {@code default.<name>}, the number of files and the path of each as {@code
 * file.<i>.path}) and the content of file {@code i} in {@code files/<i>}. Contents are stored as
 * files of their own, so the paths a template writes, which may contain placeholders, never become
 * names on the disk.
 *
 * <p>A save writes the new folder beside the old one and swaps them, keeping the old one until the
 * new one is in place. A folder that cannot be read, or does not pass {@link TemplateValidator}, is
 * left out of {@link #load}.
 */
@Singleton
final class FileCustomTemplateStore implements CustomTemplateStore {

  static final String PROPERTIES = "template.properties";
  static final String FILES = "files";
  private static final String VERSION = "1";
  private static final String NEW_PREFIX = ".new-";
  private static final String OLD_PREFIX = ".old-";

  private final FileOperations files;
  private final File root;

  @Inject
  FileCustomTemplateStore(FileOperations files, @TemplatesRoot File root) {
    this.files = files;
    this.root = root;
  }

  @Override
  public List<Template> load() throws TemplateException {
    if (!root.exists()) {
      return Collections.emptyList();
    }
    List<FileEntry> entries = list();
    boolean recovered = false;
    for (FileEntry entry : entries) {
      if (entry.isDirectory() && entry.name().startsWith(OLD_PREFIX)) {
        recover(entry.name().substring(OLD_PREFIX.length()));
        recovered = true;
      }
    }
    if (recovered) {
      entries = list();
    }
    List<Template> templates = new ArrayList<>();
    for (FileEntry entry : entries) {
      String name = entry.name();
      if (!entry.isDirectory() || name.startsWith(".")) {
        continue;
      }
      Template template = read(entry.file(), name);
      if (template != null) {
        templates.add(template);
      }
    }
    return templates;
  }

  private List<FileEntry> list() throws TemplateException {
    try {
      return files.list(root);
    } catch (IOException e) {
      throw new TemplateException(Reason.STORAGE, "cannot list the templates", e);
    }
  }

  @Override
  public void save(Template template) throws TemplateException {
    TemplateValidator.validate(template);
    String id = template.id();
    File target = new File(root, id);
    File fresh = new File(root, NEW_PREFIX + id);
    File old = new File(root, OLD_PREFIX + id);
    try {
      files.delete(fresh);
      files.createDirectories(new File(fresh, FILES));
      files.writeText(new File(fresh, PROPERTIES), propertiesOf(template));
      for (int i = 0; i < template.files().size(); i++) {
        files.writeText(new File(fresh, FILES + "/" + i), template.files().get(i).content());
      }
      files.delete(old);
      if (target.exists()) {
        files.move(target, old);
      }
      files.move(fresh, target);
      files.delete(old);
    } catch (IOException e) {
      throw new TemplateException(Reason.STORAGE, "cannot save template " + id, e);
    }
  }

  @Override
  public void delete(String id) throws TemplateException {
    if (!TemplateValidator.isValidId(id)) {
      throw new TemplateException(Reason.INVALID, "bad template id: " + id);
    }
    try {
      files.delete(new File(root, id));
      files.delete(new File(root, NEW_PREFIX + id));
      files.delete(new File(root, OLD_PREFIX + id));
    } catch (IOException e) {
      throw new TemplateException(Reason.STORAGE, "cannot delete template " + id, e);
    }
  }

  /** A save that stopped between its two moves leaves the old folder; put it back. */
  private void recover(String id) {
    File target = new File(root, id);
    File old = new File(root, OLD_PREFIX + id);
    try {
      if (target.exists()) {
        files.delete(old);
      } else {
        files.move(old, target);
      }
    } catch (IOException ignored) {
      // The folder stays where it is; the next load tries again.
    }
  }

  private static String propertiesOf(Template template) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("version", VERSION);
    properties.setProperty("name", template.name());
    properties.setProperty("type", template.type().id());
    for (Map.Entry<String, String> entry : template.defaults().entrySet()) {
      properties.setProperty("default." + entry.getKey(), entry.getValue());
    }
    properties.setProperty("files", Integer.toString(template.files().size()));
    for (int i = 0; i < template.files().size(); i++) {
      properties.setProperty("file." + i + ".path", template.files().get(i).path());
    }
    StringWriter out = new StringWriter();
    properties.store(out, null);
    return out.toString();
  }

  /** Reads one template folder; null if it is damaged or does not pass the rules. */
  private Template read(File directory, String id) {
    try {
      Properties properties = new Properties();
      properties.load(new StringReader(files.readText(new File(directory, PROPERTIES))));
      if (!VERSION.equals(properties.getProperty("version"))) {
        return null;
      }
      Map<String, String> defaults = new LinkedHashMap<>();
      for (String key : properties.stringPropertyNames()) {
        if (key.startsWith("default.")) {
          defaults.put(key.substring("default.".length()), properties.getProperty(key));
        }
      }
      int count = Integer.parseInt(properties.getProperty("files", "").trim());
      if (count < 0 || count > TemplateValidator.MAX_FILES) {
        return null;
      }
      List<TemplateFile> templateFiles = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        String path = properties.getProperty("file." + i + ".path");
        if (path == null) {
          return null;
        }
        templateFiles.add(
            new TemplateFile(path, files.readText(new File(directory, FILES + "/" + i))));
      }
      Template template =
          new Template(
              id,
              properties.getProperty("name", ""),
              ProjectType.fromId(properties.getProperty("type", "")),
              defaults,
              templateFiles);
      TemplateValidator.validate(template);
      return template;
    } catch (IOException | RuntimeException | TemplateException damaged) {
      return null;
    }
  }
}
