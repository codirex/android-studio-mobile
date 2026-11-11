package org.codirex.asm.data.template;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/** The directory that holds the templates the user made. */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface TemplatesRoot {}
