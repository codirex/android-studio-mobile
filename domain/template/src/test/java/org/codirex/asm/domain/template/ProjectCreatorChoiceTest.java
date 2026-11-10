package org.codirex.asm.domain.template;

import static org.junit.Assert.assertEquals;
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

public final class ProjectCreatorChoiceTest {

  private static final ProjectSpec SPEC =
      new ProjectSpec(
          ProjectName.of("Demo"), PackageName.of("org.codirex.demo"), ProjectType.JAVA_LIBRARY);

  private final Map<ProjectName, Project> projects = new LinkedHashMap<>();
  private final List<TemplateFile> written = new ArrayList<>();
  private List<Template> templates;
  private ProjectCreator creator;

  @Before
  public void setUp() {
    templates =
        Arrays.asList(
            template("first", "first.txt"), template("second", "second.txt"));
    ProjectRepository repository =
        new ProjectRepository() {
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
            Project project = new Project(spec, 1L);
            projects.put(spec.name(), project);
            return project;
          }

          @Override
          public void delete(ProjectName name) {
            projects.remove(name);
          }
        };
    ProjectScaffold scaffold = (project, files) -> written.addAll(files);
    creator = new ProjectCreator(repository, type -> templates, scaffold);
  }

  private static Template template(String id, String path) {
    return new Template(
        id,
        id,
        ProjectType.JAVA_LIBRARY,
        Collections.<String, String>emptyMap(),
        Collections.singletonList(new TemplateFile(path, "x")));
  }

  @Test
  public void listsTheTemplatesOfAType() {
    assertEquals(templates, creator.templatesFor(ProjectType.JAVA_LIBRARY));
  }

  @Test
  public void createsFromTheChosenTemplate() throws Exception {
    creator.create(SPEC, "second");

    assertEquals(1, written.size());
    assertEquals("second.txt", written.get(0).path());
  }

  @Test
  public void noIdMeansTheDefaultTemplate() throws Exception {
    creator.create(SPEC, null);

    assertEquals("first.txt", written.get(0).path());
  }

  @Test
  public void anUnknownTemplateCreatesNothing() {
    try {
      creator.create(SPEC, "nope");
      fail("expected STORAGE");
    } catch (ProjectException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
    assertTrue(projects.isEmpty());
    assertTrue(written.isEmpty());
  }
}
