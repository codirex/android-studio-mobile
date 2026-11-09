package org.codirex.asm.data.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codirex.asm.domain.project.ProjectType;
import org.codirex.asm.domain.template.Template;
import org.codirex.asm.domain.template.TemplateCatalog;
import org.codirex.asm.domain.template.TemplateFile;

/**
 * The templates that ship with the IDE, written as plain strings so that unit tests can render and
 * check them (ADR-0017). Each makes a small project that Gradle can build. In the text, {@code
 * ${name}} is a placeholder and {@code $$} is one dollar sign.
 */
@Singleton
final class BuiltInTemplates implements TemplateCatalog {

  private final List<Template> all;

  @Inject
  BuiltInTemplates() {
    all =
        Collections.unmodifiableList(
            Arrays.asList(application(), androidLibrary(), javaLibrary(), gradlePlugin()));
  }

  @Override
  public List<Template> templatesFor(ProjectType type) {
    List<Template> result = new ArrayList<>();
    for (Template template : all) {
      if (template.type() == type) {
        result.add(template);
      }
    }
    return result;
  }

  // --- shared pieces ---------------------------------------------------------------------------

  /** Tool versions the templates use. They age and are updated with the IDE. */
  private static Map<String, String> defaults() {
    Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("agpVersion", "9.3.1");
    defaults.put("compileSdk", "37");
    defaults.put("minSdk", "24");
    defaults.put("targetSdk", "36");
    defaults.put("appcompatVersion", "1.7.1");
    defaults.put("materialVersion", "1.14.0");
    defaults.put("junitVersion", "4.13.2");
    return defaults;
  }

  private static String lines(String... lines) {
    StringBuilder out = new StringBuilder();
    for (String line : lines) {
      out.append(line).append('\n');
    }
    return out.toString();
  }

  private static TemplateFile file(String path, String... lines) {
    return new TemplateFile(path, lines(lines));
  }

  private static TemplateFile gitignore() {
    return file(
        ".gitignore",
        "*.iml",
        ".gradle/",
        "/local.properties",
        "build/",
        ".idea/",
        ".asm/",
        ".DS_Store");
  }

  /** The settings of a Gradle project that has Android modules. */
  private static TemplateFile androidSettings(String module) {
    return file(
        "settings.gradle.kts",
        "pluginManagement {",
        "    repositories {",
        "        google()",
        "        mavenCentral()",
        "        gradlePluginPortal()",
        "    }",
        "}",
        "",
        "dependencyResolutionManagement {",
        "    repositories {",
        "        google()",
        "        mavenCentral()",
        "    }",
        "}",
        "",
        "rootProject.name = \"${projectNameKotlin}\"",
        "include(\":" + module + "\")");
  }

  /** The settings of a plain JVM Gradle project. */
  private static TemplateFile jvmSettings() {
    return file(
        "settings.gradle.kts",
        "pluginManagement {",
        "    repositories {",
        "        mavenCentral()",
        "        gradlePluginPortal()",
        "    }",
        "}",
        "",
        "rootProject.name = \"${projectNameKotlin}\"");
  }

  private static TemplateFile gradleProperties() {
    return file(
        "gradle.properties",
        "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8",
        "android.useAndroidX=true");
  }

  private static TemplateFile androidRootBuild(String plugin) {
    return file(
        "build.gradle.kts",
        "plugins {",
        "    id(\"" + plugin + "\") version \"${agpVersion}\" apply false",
        "}");
  }

  // --- Android application ---------------------------------------------------------------------

  private static Template application() {
    return new Template(
        "application-empty-activity",
        "Empty Activity",
        ProjectType.APPLICATION,
        defaults(),
        Arrays.asList(
            androidSettings("app"),
            androidRootBuild("com.android.application"),
            gradleProperties(),
            gitignore(),
            file(
                "app/build.gradle.kts",
                "plugins {",
                "    id(\"com.android.application\")",
                "}",
                "",
                "android {",
                "    namespace = \"${packageName}\"",
                "    compileSdk = ${compileSdk}",
                "",
                "    defaultConfig {",
                "        applicationId = \"${packageName}\"",
                "        minSdk = ${minSdk}",
                "        targetSdk = ${targetSdk}",
                "        versionCode = 1",
                "        versionName = \"1.0\"",
                "    }",
                "",
                "    compileOptions {",
                "        sourceCompatibility = JavaVersion.VERSION_17",
                "        targetCompatibility = JavaVersion.VERSION_17",
                "    }",
                "}",
                "",
                "dependencies {",
                "    implementation(\"androidx.appcompat:appcompat:${appcompatVersion}\")",
                "    implementation(\"com.google.android.material:material:${materialVersion}\")",
                "}"),
            file(
                "app/src/main/AndroidManifest.xml",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">",
                "",
                "    <application",
                "        android:allowBackup=\"true\"",
                "        android:label=\"@string/app_name\"",
                "        android:supportsRtl=\"true\"",
                "        android:theme=\"@style/Theme.App\">",
                "        <activity",
                "            android:name=\".MainActivity\"",
                "            android:exported=\"true\">",
                "            <intent-filter>",
                "                <action android:name=\"android.intent.action.MAIN\" />",
                "                <category android:name=\"android.intent.category.LAUNCHER\" />",
                "            </intent-filter>",
                "        </activity>",
                "    </application>",
                "",
                "</manifest>"),
            file(
                "app/src/main/java/${packagePath}/MainActivity.java",
                "package ${packageName};",
                "",
                "import android.os.Bundle;",
                "import androidx.appcompat.app.AppCompatActivity;",
                "",
                "public class MainActivity extends AppCompatActivity {",
                "",
                "    @Override",
                "    protected void onCreate(Bundle savedInstanceState) {",
                "        super.onCreate(savedInstanceState);",
                "        setContentView(R.layout.activity_main);",
                "    }",
                "}"),
            file(
                "app/src/main/res/layout/activity_main.xml",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"",
                "    android:layout_width=\"match_parent\"",
                "    android:layout_height=\"match_parent\">",
                "",
                "    <TextView",
                "        android:layout_width=\"wrap_content\"",
                "        android:layout_height=\"wrap_content\"",
                "        android:layout_gravity=\"center\"",
                "        android:text=\"@string/hello\" />",
                "",
                "</FrameLayout>"),
            file(
                "app/src/main/res/values/strings.xml",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<resources>",
                "    <string name=\"app_name\">${projectNameAndroid}</string>",
                "    <string name=\"hello\">Hello, world!</string>",
                "</resources>"),
            file(
                "app/src/main/res/values/themes.xml",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<resources>",
                "    <style name=\"Theme.App\" parent=\"Theme.Material3.DayNight.NoActionBar\" />",
                "</resources>")));
  }

  // --- Android library -------------------------------------------------------------------------

  private static Template androidLibrary() {
    return new Template(
        "android-library-basic",
        "Android Library",
        ProjectType.ANDROID_LIBRARY,
        defaults(),
        Arrays.asList(
            androidSettings("library"),
            androidRootBuild("com.android.library"),
            gradleProperties(),
            gitignore(),
            file(
                "library/build.gradle.kts",
                "plugins {",
                "    id(\"com.android.library\")",
                "}",
                "",
                "android {",
                "    namespace = \"${packageName}\"",
                "    compileSdk = ${compileSdk}",
                "",
                "    defaultConfig {",
                "        minSdk = ${minSdk}",
                "    }",
                "",
                "    compileOptions {",
                "        sourceCompatibility = JavaVersion.VERSION_17",
                "        targetCompatibility = JavaVersion.VERSION_17",
                "    }",
                "}",
                "",
                "dependencies {",
                "    testImplementation(\"junit:junit:${junitVersion}\")",
                "}"),
            file("library/src/main/AndroidManifest.xml", "<manifest />"),
            file(
                "library/src/main/java/${packagePath}/Greeter.java",
                "package ${packageName};",
                "",
                "/** An example class of the library. */",
                "public final class Greeter {",
                "",
                "    private Greeter() {}",
                "",
                "    public static String greet(String name) {",
                "        return \"Hello, \" + name + \"!\";",
                "    }",
                "}"),
            file(
                "library/src/test/java/${packagePath}/GreeterTest.java",
                "package ${packageName};",
                "",
                "import static org.junit.Assert.assertEquals;",
                "",
                "import org.junit.Test;",
                "",
                "public class GreeterTest {",
                "",
                "    @Test",
                "    public void greetsByName() {",
                "        assertEquals(\"Hello, world!\", Greeter.greet(\"world\"));",
                "    }",
                "}")));
  }

  // --- Java library ----------------------------------------------------------------------------

  private static Template javaLibrary() {
    return new Template(
        "java-library-basic",
        "Java Library",
        ProjectType.JAVA_LIBRARY,
        defaults(),
        Arrays.asList(
            jvmSettings(),
            gitignore(),
            file(
                "build.gradle.kts",
                "plugins {",
                "    id(\"java-library\")",
                "}",
                "",
                "group = \"${packageName}\"",
                "version = \"1.0\"",
                "",
                "java {",
                "    sourceCompatibility = JavaVersion.VERSION_17",
                "    targetCompatibility = JavaVersion.VERSION_17",
                "}",
                "",
                "repositories {",
                "    mavenCentral()",
                "}",
                "",
                "dependencies {",
                "    testImplementation(\"junit:junit:${junitVersion}\")",
                "}"),
            file(
                "src/main/java/${packagePath}/Greeter.java",
                "package ${packageName};",
                "",
                "/** An example class of the library. */",
                "public final class Greeter {",
                "",
                "    private Greeter() {}",
                "",
                "    public static String greet(String name) {",
                "        return \"Hello, \" + name + \"!\";",
                "    }",
                "}"),
            file(
                "src/test/java/${packagePath}/GreeterTest.java",
                "package ${packageName};",
                "",
                "import static org.junit.Assert.assertEquals;",
                "",
                "import org.junit.Test;",
                "",
                "public class GreeterTest {",
                "",
                "    @Test",
                "    public void greetsByName() {",
                "        assertEquals(\"Hello, world!\", Greeter.greet(\"world\"));",
                "    }",
                "}")));
  }

  // --- Gradle plugin ---------------------------------------------------------------------------

  private static Template gradlePlugin() {
    return new Template(
        "gradle-plugin-basic",
        "Gradle Plugin",
        ProjectType.GRADLE_PLUGIN,
        defaults(),
        Arrays.asList(
            jvmSettings(),
            gitignore(),
            file(
                "build.gradle.kts",
                "plugins {",
                "    id(\"java-gradle-plugin\")",
                "}",
                "",
                "group = \"${packageName}\"",
                "version = \"1.0\"",
                "",
                "java {",
                "    sourceCompatibility = JavaVersion.VERSION_17",
                "    targetCompatibility = JavaVersion.VERSION_17",
                "}",
                "",
                "repositories {",
                "    mavenCentral()",
                "}",
                "",
                "gradlePlugin {",
                "    plugins {",
                "        create(\"main\") {",
                "            id = \"${packageName}\"",
                "            implementationClass = \"${packageName}.ExamplePlugin\"",
                "        }",
                "    }",
                "}"),
            file(
                "src/main/java/${packagePath}/ExamplePlugin.java",
                "package ${packageName};",
                "",
                "import org.gradle.api.Plugin;",
                "import org.gradle.api.Project;",
                "",
                "/** An example plugin: it adds the task {@code hello}. */",
                "public class ExamplePlugin implements Plugin<Project> {",
                "",
                "    @Override",
                "    public void apply(Project project) {",
                "        project.getTasks().register(\"hello\", task -> {",
                "            task.setGroup(\"example\");",
                "            task.doLast(t -> System.out.println(\"Hello from \" + project.getName()));",
                "        });",
                "    }",
                "}")));
  }
}
