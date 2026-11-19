package org.codirex.asm.domain.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The packages the repository lists. Immutable. */
public final class SdkCatalog {

  private final List<SdkPackage> packages;

  public SdkCatalog(List<SdkPackage> packages) {
    this.packages = Collections.unmodifiableList(new ArrayList<>(packages));
  }

  public List<SdkPackage> packages() {
    return packages;
  }

  /** The package with this path, or {@code null}. */
  public SdkPackage find(String path) {
    for (SdkPackage pkg : packages) {
      if (pkg.path().equals(path)) {
        return pkg;
      }
    }
    return null;
  }
}
