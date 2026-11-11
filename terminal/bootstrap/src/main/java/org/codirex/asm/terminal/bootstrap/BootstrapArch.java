package org.codirex.asm.terminal.bootstrap;

/** The processor architectures the userland is built for, with the Android ABI of each. */
public enum BootstrapArch {
  AARCH64("aarch64", "arm64-v8a"),
  ARM("arm", "armeabi-v7a"),
  X86_64("x86_64", "x86_64"),
  I686("i686", "x86");

  private final String id;
  private final String abi;

  BootstrapArch(String id, String abi) {
    this.id = id;
    this.abi = abi;
  }

  /** The name the package build uses: {@code aarch64}, {@code arm}, {@code x86_64}, {@code i686}. */
  public String id() {
    return id;
  }

  /** The file name of this architecture's archive, for example {@code bootstrap-aarch64.zip}. */
  public String archiveName() {
    return "bootstrap-" + id + ".zip";
  }

  /**
   * The best architecture for a device: the first of its ABIs, in the order Android prefers them
   * ({@code Build.SUPPORTED_ABIS}), that a bootstrap exists for.
   *
   * @return null if the device supports none of them
   */
  public static BootstrapArch forAbis(String[] abis) {
    for (String candidate : abis) {
      for (BootstrapArch arch : values()) {
        if (arch.abi.equals(candidate)) {
          return arch;
        }
      }
    }
    return null;
  }
}
