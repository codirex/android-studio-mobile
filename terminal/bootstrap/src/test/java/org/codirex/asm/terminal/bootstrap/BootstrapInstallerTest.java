package org.codirex.asm.terminal.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.codirex.asm.core.fs.FileOperations;
import org.codirex.asm.terminal.bootstrap.BootstrapException.Reason;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class BootstrapInstallerTest {

  private static final String ARROW = "←";

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final SymlinkCreator links =
      (link, target) -> Files.createSymbolicLink(link.toPath(), new File(target).toPath());
  private File filesDir;
  private BootstrapInstaller installer;

  @Before
  public void setUp() {
    filesDir = new File(tmp.getRoot(), "files");
    installer = new BootstrapInstaller(FileOperations.local(), links);
  }

  /** Builds an archive like generate-bootstraps.sh does: files, folders and SYMLINKS.txt. */
  private static final class Archive {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final ZipOutputStream zip = new ZipOutputStream(bytes);

    Archive file(String name, String content) throws IOException {
      zip.putNextEntry(new ZipEntry(name));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      return this;
    }

    Archive dir(String name) throws IOException {
      zip.putNextEntry(new ZipEntry(name));
      zip.closeEntry();
      return this;
    }

    InputStream build() throws IOException {
      zip.close();
      return new ByteArrayInputStream(bytes.toByteArray());
    }
  }

  private static Archive valid() throws IOException {
    return new Archive()
        .dir("bin/")
        .file("bin/dash", "#!dash")
        .file("bin/bash", "#!bash")
        .file("etc/profile", "export A=1\n")
        .file("lib/apt/methods/https", "x")
        .file("share/doc/readme.txt", "hello عربي")
        .file("SYMLINKS.txt", "dash" + ARROW + "./bin/sh\n../bin/bash" + ARROW + "./lib/bash\n");
  }

  private static void assertInstallFails(Attempt action, Reason reason) {
    try {
      action.run();
      fail("expected " + reason);
    } catch (BootstrapException e) {
      assertEquals(reason, e.reason());
    }
  }

  private interface Attempt {
    void run() throws BootstrapException;
  }

  private void installOrFail(Archive archive, Reason reason) {
    assertInstallFails(
        () -> {
          try {
            installer.install(archive.build(), filesDir, null);
          } catch (IOException e) {
            throw new AssertionError(e);
          }
        },
        reason);
  }

  @Test
  public void extractsFilesAndCreatesTheHome() throws Exception {
    installer.install(valid().build(), filesDir, null);
    File prefix = BootstrapInstaller.prefixOf(filesDir);
    assertTrue(installer.isInstalled(filesDir));
    assertEquals("export A=1\n", read(new File(prefix, "etc/profile")));
    assertEquals("hello عربي", read(new File(prefix, "share/doc/readme.txt")));
    assertTrue(new File(prefix, "tmp").isDirectory());
    assertTrue(BootstrapInstaller.homeOf(filesDir).isDirectory());
  }

  @Test
  public void createsSymlinksWithTheirTargetsAsGiven() throws Exception {
    installer.install(valid().build(), filesDir, null);
    File prefix = BootstrapInstaller.prefixOf(filesDir);
    assertEquals("dash", Files.readSymbolicLink(new File(prefix, "bin/sh").toPath()).toString());
    assertEquals(
        "../bin/bash", Files.readSymbolicLink(new File(prefix, "lib/bash").toPath()).toString());
    assertTrue(new File(prefix, "bin/sh").canExecute());
  }

  @Test
  public void programsAreExecutableAndOtherFilesAreNot() throws Exception {
    installer.install(valid().build(), filesDir, null);
    File prefix = BootstrapInstaller.prefixOf(filesDir);
    assertTrue(new File(prefix, "bin/dash").canExecute());
    assertTrue(new File(prefix, "lib/apt/methods/https").canExecute());
    assertFalse(new File(prefix, "etc/profile").canExecute());
  }

  @Test
  public void leavesNoStagingFolderAndSecondInstallDoesNothing() throws Exception {
    installer.install(valid().build(), filesDir, null);
    assertFalse(new File(filesDir, "usr-staging").exists());
    File marker = new File(BootstrapInstaller.prefixOf(filesDir), "marker");
    assertTrue(marker.createNewFile());
    installer.install(valid().file("x", "y").build(), filesDir, null);
    assertTrue(marker.exists());
  }

  @Test
  public void aLeftoverStagingFolderFromACrashIsReplaced() throws Exception {
    File old = new File(filesDir, "usr-staging/old.txt");
    FileOperations.local().writeText(old, "stale");
    installer.install(valid().build(), filesDir, null);
    assertFalse(new File(BootstrapInstaller.prefixOf(filesDir), "old.txt").exists());
  }

  @Test
  public void reportsProgress() throws Exception {
    final List<Integer> entries = new ArrayList<>();
    final long[] last = {0};
    installer.install(
        valid().build(),
        filesDir,
        (count, bytes) -> {
          entries.add(count);
          last[0] = bytes;
        });
    assertFalse(entries.isEmpty());
    assertTrue(last[0] > 0);
  }

  @Test
  public void anEntryThatLeavesTheFolderIsRefusedAndNothingStays() throws Exception {
    installOrFail(valid().file("../escape.txt", "x"), Reason.INVALID_ARCHIVE);
    assertNothingInstalled();
    assertFalse(new File(filesDir, "escape.txt").exists());
  }

  @Test
  public void absoluteAndBackslashNamesAreRefused() throws Exception {
    installOrFail(valid().file("/etc/evil", "x"), Reason.INVALID_ARCHIVE);
    installOrFail(valid().file("a\\b", "x"), Reason.INVALID_ARCHIVE);
    assertNothingInstalled();
  }

  @Test
  public void aSymlinkPathThatLeavesTheFolderIsRefused() throws Exception {
    Archive archive =
        new Archive()
            .file("bin/dash", "x")
            .file("SYMLINKS.txt", "dash" + ARROW + "../outside\n");
    installOrFail(archive, Reason.INVALID_ARCHIVE);
    assertNothingInstalled();
    assertFalse(new File(filesDir, "outside").exists());
  }

  @Test
  public void badSymlinkLinesAreRefused() throws Exception {
    installOrFail(
        new Archive().file("bin/dash", "x").file("SYMLINKS.txt", "no arrow here\n"),
        Reason.INVALID_ARCHIVE);
    installOrFail(
        new Archive().file("bin/dash", "x").file("SYMLINKS.txt", ARROW + "./bin/sh\n"),
        Reason.INVALID_ARCHIVE);
    installOrFail(
        new Archive().file("bin/dash", "x").file("SYMLINKS.txt", "dash" + ARROW + "\n"),
        Reason.INVALID_ARCHIVE);
    assertNothingInstalled();
  }

  @Test
  public void anArchiveWithoutSymlinksOrProgramsIsNotABootstrap() throws Exception {
    installOrFail(new Archive().file("bin/dash", "x"), Reason.INVALID_ARCHIVE);
    installOrFail(
        new Archive().file("etc/a", "x").file("SYMLINKS.txt", ""), Reason.INVALID_ARCHIVE);
    assertNothingInstalled();
  }

  @Test
  public void aTruncatedArchiveFailsCleanly() throws Exception {
    byte[] bytes = readAll(valid().build());
    byte[] cut = new byte[bytes.length / 2];
    System.arraycopy(bytes, 0, cut, 0, cut.length);
    try {
      installer.install(new ByteArrayInputStream(cut), filesDir, null);
      fail();
    } catch (BootstrapException e) {
      assertTrue(
          e.reason() == Reason.INVALID_ARCHIVE || e.reason() == Reason.STORAGE);
    }
    assertNothingInstalled();
  }

  @Test
  public void aFailingSymlinkCreatorFailsTheInstallWithoutAPrefix() throws Exception {
    BootstrapInstaller failing =
        new BootstrapInstaller(
            FileOperations.local(),
            (link, target) -> {
              throw new IOException("no links here");
            });
    try {
      failing.install(valid().build(), filesDir, null);
      fail();
    } catch (BootstrapException e) {
      assertEquals(Reason.STORAGE, e.reason());
    }
    assertNothingInstalled();
  }

  @Test
  public void archiveNamesAndAbis() {
    assertEquals("bootstrap-aarch64.zip", BootstrapArch.AARCH64.archiveName());
    assertEquals("bootstrap-x86_64.zip", BootstrapArch.X86_64.archiveName());
    assertEquals(
        BootstrapArch.AARCH64,
        BootstrapArch.forAbis(new String[] {"arm64-v8a", "armeabi-v7a", "armeabi"}));
    assertEquals(BootstrapArch.ARM, BootstrapArch.forAbis(new String[] {"mips", "armeabi-v7a"}));
    assertEquals(BootstrapArch.I686, BootstrapArch.forAbis(new String[] {"x86"}));
    assertNull(BootstrapArch.forAbis(new String[] {"mips", "riscv64"}));
    assertNull(BootstrapArch.forAbis(new String[0]));
  }

  private void assertNothingInstalled() {
    assertFalse(installer.isInstalled(filesDir));
    assertFalse(new File(filesDir, "usr-staging").exists());
  }

  private static String read(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int read;
    while ((read = in.read(buffer)) > 0) {
      out.write(buffer, 0, read);
    }
    return out.toByteArray();
  }
}
