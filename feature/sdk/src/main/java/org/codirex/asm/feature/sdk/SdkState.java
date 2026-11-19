package org.codirex.asm.feature.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codirex.asm.domain.sdk.SdkEntry;

/** Everything the SDK screen shows. Immutable. */
final class SdkState {

  private final List<SdkEntry> entries;
  private final boolean loading;
  private final boolean loadedOnce;
  private final String busyName;
  private final SdkFailure failure;

  private SdkState(
      List<SdkEntry> entries,
      boolean loading,
      boolean loadedOnce,
      String busyName,
      SdkFailure failure) {
    this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    this.loading = loading;
    this.loadedOnce = loadedOnce;
    this.busyName = busyName;
    this.failure = failure;
  }

  static SdkState initial() {
    return new SdkState(Collections.<SdkEntry>emptyList(), true, false, null, null);
  }

  SdkState withLoading() {
    return new SdkState(entries, true, loadedOnce, busyName, failure);
  }

  SdkState loaded(List<SdkEntry> newEntries) {
    return new SdkState(newEntries, false, true, busyName, failure);
  }

  /** An install or removal of the package with this display name started. */
  SdkState busy(String name) {
    return new SdkState(entries, loading, loadedOnce, name, failure);
  }

  SdkState idle() {
    return new SdkState(entries, loading, loadedOnce, null, failure);
  }

  SdkState failed(SdkFailure newFailure) {
    return new SdkState(entries, false, loadedOnce, busyName, newFailure);
  }

  SdkState failureHandled() {
    return new SdkState(entries, loading, loadedOnce, busyName, null);
  }

  List<SdkEntry> entries() {
    return entries;
  }

  boolean isLoading() {
    return loading;
  }

  /** The list is known and has nothing in it. */
  boolean isEmpty() {
    return loadedOnce && entries.isEmpty();
  }

  /** The display name of the package being installed or removed, or {@code null}. */
  String busyName() {
    return busyName;
  }

  boolean isBusy() {
    return busyName != null;
  }

  SdkFailure failure() {
    return failure;
  }
}
