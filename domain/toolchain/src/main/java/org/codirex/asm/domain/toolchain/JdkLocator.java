package org.codirex.asm.domain.toolchain;

/**
 * Finds the JDKs that are installed. The domain defines it; a data module implements it.
 *
 * <p><b>Every method may block</b> (it reads files). Callers run it in a background job.
 */
public interface JdkLocator {

  /** The JDK installed for {@code offer}, or {@code null} if it is not installed (completely). */
  InstalledJdk find(JdkOffer offer);
}
