package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.codirex.asm.domain.project.PackageName;
import org.codirex.asm.domain.project.Project;
import org.codirex.asm.domain.project.ProjectException;
import org.codirex.asm.domain.project.ProjectException.Reason;
import org.codirex.asm.domain.project.ProjectName;
import org.codirex.asm.domain.project.ProjectRepository;
import org.codirex.asm.domain.project.ProjectSpec;
import org.codirex.asm.domain.project.ProjectType;
import org.junit.Before;
import org.junit.Test;

public final class ProjectCreatorTest {

  private static final ProjectSpec SPEC =
      new ProjectSpec(
          ProjectName.of("Demo"), PackageName.of("org.codirex.demo"), ProjectType.JAVA_LIBRARY);

  private static final class FakeRepository implements ProjectRepository {
    final Map<ProjectName, Project> projects = new LinkedHashMap<>();

    @Override
    public List<Project> list() {
      return new ArrayList<>(projects.values());
    }

    @Override
    public Optional<Project> find(ProjectName name) {
      return Optional.ofNullable(projects.get(name));
    }

    @Override
    public Project create(ProjectSpec spec) throws ProjectException {
      if (projects.containsKey(spec.name())) {
        throw new ProjectException(Reason.ALREADY_EXISTS, "exists");
      }
      Project project = new Project(spec, 1L);
      projects.put(spec.name(), project);
      return project;
    }

    @Override
    public void delete(ProjectName name) {
      projects.remove(name);
    }
  }

  private static final class FakeScaffold implements ProjectScaffold {
    final List<TemplateFile> written = new ArrayList<>();
    boolean fail;

    @Override
    public void write(ProjectName project, List<TemplateFile> files) throws TemplateException {
      if (fail) {
        throw new TemplateException(TemplateException.Reason.STORAGE, "disk full");
      }
      written.addAll(files);
    }
  }

  private FakeRepository repository;
  private FakeScaffold scaffold;
  private List<Template> templates;

  @Before
  public void setUp() {
    repository = new FakeRepository();
    scaffold = new FakeScaffold();
    templates =
        Collections.singletonList(
            new Template(
                "t",
                "T",
                ProjectType.JAVA_LIBRARY,
                Collections.<String, String>emptyMap(),
                Arrays.asList(
                    new TemplateFile("README.md", "# ${projectName}\n"),
                    new TemplateFile("src/${packagePath}/Lib.java", "package ${packageName};\n"))));
  }

  private ProjectCreator creator() {
    return new ProjectCreator(repository, type -> templates, scaffold);
  }

  @Test
  public void createsTheProjectAndWritesTheRenderedFiles() throws Exception {
    Project project = creator().create(SPEC);

    assertEquals(SPEC, project.spec());
    assertTrue(repository.projects.containsKey(SPEC.name()));
    assertEquals(2, scaffold.written.size());
    assertEquals("README.md", scaffold.written.get(0).path());
    assertEquals("# Demo\n", scaffold.written.get(0).content());
    assertEquals("src/org/codirex/demo/Lib.java", scaffold.written.get(1).path());
  }

  @Test
  public void usesTheFirstTemplateOfTheType() throws Exception {
    templates =
        Arrays.asList(
            new Template(
                "first",
                "First",
                ProjectType.JAVA_LIBRARY,
                Collections.<String, String>emptyMap(),
                Collections.singletonList(new TemplateFile("first.txt", "1"))),
            new Template(
                "second",
                "Second",
                ProjectType.JAVA_LIBRARY,
                Collections.<String, String>emptyMap(),
                Collections.singletonList(new TemplateFile("second.txt", "2"))));

    creator().create(SPEC);

    assertEquals(1, scaffold.written.size());
    assertEquals("first.txt", scaffold.written.get(0).path());
  }

  @Test
  public void aTypeWithoutATemplateGivesAnEmptyProject() throws Exception {
    templates = Collections.emptyList();

    creator().create(SPEC);

    assertTrue(repository.projects.containsKey(SPEC.name()));
    assertTrue(scaffold.written.isEmpty());
  }

  @Test
  public void aTakenNameFailsAndWritesNothing() throws Exception {
    creator().create(SPEC);
    scaffold.written.clear();

    try {
      creator().create(SPEC);
      fail("expected ALREADY_EXISTS");
    } catch (ProjectException e) {
      assertEquals(Reason.ALREADY_EXISTS, e.reason());
    }
    assertTrue(scaffold.written.isEmpty());
  }

  @Test
  public void aFailedWriteRemovesTheProject() {
    scaffold.fail = true;

    try {
      creator().create(SPEC);
      fail("expected STORAGE");
    } catch (ProjectException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
    assertFalse(repository.projects.containsKey(SPEC.name()));
  }

  @Test
  public void aTemplateThatCannotBeRenderedCreatesNothing() {
    templates =
        Collections.singletonList(
            new Template(
                "bad",
                "Bad",
                ProjectType.JAVA_LIBRARY,
                Collections.<String, String>emptyMap(),
                Collections.singletonList(new TemplateFile("a.txt", "${nobody}"))));

    try {
      creator().create(SPEC);
      fail("expected STORAGE");
    } catch (ProjectException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
    assertTrue(repository.projects.isEmpty());
    assertTrue(scaffold.written.isEmpty());
  }
}
