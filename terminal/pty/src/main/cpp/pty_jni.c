/* JNI bindings of pty_core for org.codirex.asm.terminal.pty.PtyNative (ADR-0023). */
#define _POSIX_C_SOURCE 200809L
#include <errno.h>
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "pty_core.h"

#define IO_BUFFER 16384

static void throw_io(JNIEnv *env, const char *what, int error) {
  char message[160];
  jclass io = (*env)->FindClass(env, "java/io/IOException");
  if (io == NULL) {
    return;
  }
  snprintf(message, sizeof(message), "%s: %s", what, strerror(error));
  (*env)->ThrowNew(env, io, message);
}

/* Copies a Java String[] into a NULL terminated array of C strings; free it with free_strings. */
static char **copy_strings(JNIEnv *env, jobjectArray array) {
  jsize count = array == NULL ? 0 : (*env)->GetArrayLength(env, array);
  char **result = calloc((size_t)count + 1, sizeof(char *));
  jsize i;
  if (result == NULL) {
    return NULL;
  }
  for (i = 0; i < count; i++) {
    jstring item = (jstring)(*env)->GetObjectArrayElement(env, array, i);
    const char *utf;
    if (item == NULL) {
      continue;
    }
    utf = (*env)->GetStringUTFChars(env, item, NULL);
    if (utf != NULL) {
      result[i] = strdup(utf);
      (*env)->ReleaseStringUTFChars(env, item, utf);
    }
    (*env)->DeleteLocalRef(env, item);
    if (result[i] == NULL) {
      /* Out of memory or an exception: stop; the caller sees the NULL entry. */
      while (i-- > 0) {
        free(result[i]);
      }
      free(result);
      return NULL;
    }
  }
  return result;
}

static void free_strings(char **strings) {
  size_t i;
  if (strings == NULL) {
    return;
  }
  for (i = 0; strings[i] != NULL; i++) {
    free(strings[i]);
  }
  free(strings);
}

JNIEXPORT jintArray JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_spawn(
    JNIEnv *env, jclass type, jstring path, jobjectArray argv, jobjectArray envp, jstring cwd,
    jint rows, jint columns) {
  const char *c_path = (*env)->GetStringUTFChars(env, path, NULL);
  const char *c_cwd = cwd == NULL ? NULL : (*env)->GetStringUTFChars(env, cwd, NULL);
  char **c_argv = copy_strings(env, argv);
  char **c_envp = copy_strings(env, envp);
  jintArray result = NULL;
  int master = -1;
  pid_t pid = 0;
  int status;
  (void)type;

  if (c_path == NULL || c_argv == NULL || c_envp == NULL || (cwd != NULL && c_cwd == NULL)) {
    if (!(*env)->ExceptionCheck(env)) {
      throw_io(env, "cannot prepare the arguments", ENOMEM);
    }
    goto done;
  }
  status = asm_pty_spawn(c_path, c_argv, c_envp, c_cwd, rows, columns, &master, &pid);
  if (status < 0) {
    throw_io(env, "cannot start the process", -status);
    goto done;
  }
  result = (*env)->NewIntArray(env, 2);
  if (result != NULL) {
    jint values[2];
    values[0] = master;
    values[1] = (jint)pid;
    (*env)->SetIntArrayRegion(env, result, 0, 2, values);
  } else {
    close(master);
  }

done:
  free_strings(c_argv);
  free_strings(c_envp);
  if (c_path != NULL) {
    (*env)->ReleaseStringUTFChars(env, path, c_path);
  }
  if (c_cwd != NULL) {
    (*env)->ReleaseStringUTFChars(env, cwd, c_cwd);
  }
  return result;
}

JNIEXPORT void JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_setSize(
    JNIEnv *env, jclass type, jint fd, jint rows, jint columns) {
  int status = asm_pty_set_size(fd, rows, columns);
  (void)type;
  if (status < 0) {
    throw_io(env, "cannot resize the terminal", -status);
  }
}

JNIEXPORT jint JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_waitFor(
    JNIEnv *env, jclass type, jint pid) {
  (void)env;
  (void)type;
  return asm_pty_wait((pid_t)pid);
}

JNIEXPORT void JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_signal(
    JNIEnv *env, jclass type, jint pid, jint signal_number) {
  int status = asm_pty_signal((pid_t)pid, signal_number);
  (void)type;
  /* A process that is already gone is not an error. */
  if (status < 0 && status != -ESRCH) {
    throw_io(env, "cannot signal the process", -status);
  }
}

JNIEXPORT jint JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_read(
    JNIEnv *env, jclass type, jint fd, jbyteArray buffer, jint offset, jint length) {
  char chunk[IO_BUFFER];
  ssize_t count;
  (void)type;
  if (length > IO_BUFFER) {
    length = IO_BUFFER;
  }
  count = asm_pty_read(fd, chunk, (size_t)length);
  if (count < 0) {
    throw_io(env, "cannot read from the terminal", (int)-count);
    return -1;
  }
  if (count == 0) {
    return -1;
  }
  (*env)->SetByteArrayRegion(env, buffer, offset, (jsize)count, (const jbyte *)chunk);
  return (jint)count;
}

JNIEXPORT void JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_write(
    JNIEnv *env, jclass type, jint fd, jbyteArray buffer, jint offset, jint length) {
  char chunk[IO_BUFFER];
  (void)type;
  while (length > 0) {
    jint part = length > IO_BUFFER ? IO_BUFFER : length;
    ssize_t status;
    (*env)->GetByteArrayRegion(env, buffer, offset, part, (jbyte *)chunk);
    if ((*env)->ExceptionCheck(env)) {
      return;
    }
    status = asm_pty_write(fd, chunk, (size_t)part);
    if (status < 0) {
      throw_io(env, "cannot write to the terminal", (int)-status);
      return;
    }
    offset += part;
    length -= part;
  }
}

JNIEXPORT void JNICALL Java_org_codirex_asm_terminal_pty_PtyNative_close(
    JNIEnv *env, jclass type, jint fd) {
  (void)env;
  (void)type;
  close(fd);
}
