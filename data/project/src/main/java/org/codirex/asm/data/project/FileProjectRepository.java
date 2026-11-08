package org.codirex.asm.data.project;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.core.fs.FileEntry;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;

/**
 * Keeps each project in its own folder under one root, named after the project. A folder is a
 * project only if it holds {@code .asm/project.properties}, which stores the format version, the
 * package name, the type and the creation time. The project name is the folder name, so the two
 * can never disagree.
 *
 * <p>Operations are serialized with a lock: one process owns the projects directory.
 */
@Singleton
final class FileProjectRepository implements ProjectRepository {

  static final String METADATA_PATH = ".asm/project.properties";
  private static final String FORMAT = "1";

  private static final Comparator<Project> NEWEST_FIRST =
      (a, b) -> {
        int byTime = Long.compare(b.createdAt(), a.createdAt());
        return byTime != 0 ? byTime : a.name().value().compareTo(b.name().value());
      };

  private final FileOperations files;
  private final File root;
  private final LongSupplier clock;

  @Inject
  FileProjectRepository(FileOperations files, @ProjectsRoot File root) {
    this(files, root, System::currentTimeMillis);
  }

  FileProjectRepository(FileOperations files, File root, LongSupplier clock) {
    this.files = files;
    this.root = root;
    this.clock = clock;
  }

  @Override
  public synchronized List<Project> list() throws ProjectException {
    if (!root.isDirectory()) {
      return Collections.emptyList();
    }
    List<FileEntry> entries;
    try {
      entries = files.list(root);
    } catch (IOException e) {
      throw storage("cannot list projects", e);
    }
    List<Project> projects = new ArrayList<>();
    for (FileEntry entry : entries) {
      if (!entry.isDirectory()) {
        continue;
      }
      try {
        Project project = read(entry.file());
        if (project != null) {
          projects.add(project);
        }
      } catch (ProjectException | IOException broken) {
        // One broken folder must not hide the other projects.
      }
    }
    Collections.sort(projects, NEWEST_FIRST);
    return projects;
  }

  @Override
  public synchronized Optional<Project> find(ProjectName name) throws ProjectException {
    File dir = directoryOf(name);
    if (!dir.isDirectory()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(read(dir));
    } catch (IOException e) {
      throw storage("cannot read project " + name, e);
    }
  }

  @Override
  public synchronized Project create(ProjectSpec spec) throws ProjectException {
    File dir = directoryOf(spec.name());
    if (dir.exists()) {
      throw new ProjectException(Reason.ALREADY_EXISTS, "already exists: " + spec.name());
    }
    Project project = new Project(spec, clock.getAsLong());
    try {
      files.createDirectories(root);
      files.createDirectories(dir);
      files.writeText(new File(dir, METADATA_PATH), serialize(project));
    } catch (IOException e) {
      try {
        files.delete(dir);
      } catch (IOException ignored) {
        // Nothing more can be done; report the original failure.
      }
      throw storage("cannot create project " + spec.name(), e);
    }
    return project;
  }

  @Override
  public synchronized void delete(ProjectName name) throws ProjectException {
    File dir = directoryOf(name);
    // Only folders that are projects may be deleted, never an arbitrary folder.
    if (!new File(dir, METADATA_PATH).isFile()) {
      throw new ProjectException(Reason.NOT_FOUND, "no such project: " + name);
    }
    try {
      files.delete(dir);
    } catch (IOException e) {
      throw storage("cannot delete project " + name, e);
    }
  }

  private File directoryOf(ProjectName name) throws ProjectException {
    try {
      return files.resolve(root, name.value());
    } catch (IOException e) {
      throw storage("cannot locate project " + name, e);
    }
  }

  private static String serialize(Project project) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("format", FORMAT);
    properties.setProperty("package", project.packageName().value());
    properties.setProperty("type", project.type().id());
    properties.setProperty("created", Long.toString(project.createdAt()));
    StringWriter out = new StringWriter();
    properties.store(out, null);
    return out.toString();
  }

  /** Returns the project in {@code dir}, or null if the folder has no metadata (not a project). */
  private Project read(File dir) throws IOException, ProjectException {
    File metadata = new File(dir, METADATA_PATH);
    if (!metadata.isFile()) {
      return null;
    }
    String text = files.readText(metadata);
    try {
      Properties properties = new Properties();
      properties.load(new StringReader(text));
      if (!FORMAT.equals(properties.getProperty("format"))) {
        throw new ProjectException(Reason.INVALID_METADATA, "unsupported format in " + dir.getName());
      }
      ProjectName name = ProjectName.of(dir.getName());
      PackageName packageName = PackageName.of(properties.getProperty("package"));
      ProjectType type = ProjectType.fromId(properties.getProperty("type"));
      long created = Long.parseLong(properties.getProperty("created"));
      return new Project(new ProjectSpec(name, packageName, type), created);
    } catch (IllegalArgumentException e) {
      throw new ProjectException(Reason.INVALID_METADATA, "invalid metadata in " + dir.getName(), e);
    }
  }

  private static ProjectException storage(String message, IOException cause) {
    return new ProjectException(Reason.STORAGE, message, cause);
  }
}
