package org.codirex.asm.data.project;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** The directory that holds every project. */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface ProjectsRoot {}
