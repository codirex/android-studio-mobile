package org.codirex.asm.core.concurrency;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * Marks the realm that lives as long as the process. Use it for work that must outlive any screen:
 * indexing, builds, the terminal client. Screen work belongs in a {@link RealmViewModel} realm.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationRealm {}
