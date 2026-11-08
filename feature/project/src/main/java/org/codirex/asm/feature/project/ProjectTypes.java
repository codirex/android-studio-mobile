package org.codirex.asm.feature.project;

import androidx.annotation.StringRes;
import org.codirex.asm.domain.project.ProjectType;

/** The user-facing name of each project type. */
final class ProjectTypes {

  private ProjectTypes() {}

  @StringRes
  static int label(ProjectType type) {
    switch (type) {
      case APPLICATION:
        return R.string.type_application;
      case ANDROID_LIBRARY:
        return R.string.type_android_library;
      case JAVA_LIBRARY:
        return R.string.type_java_library;
      case GRADLE_PLUGIN:
        return R.string.type_gradle_plugin;
      default:
        throw new IllegalArgumentException("no label for " + type);
    }
  }
}
