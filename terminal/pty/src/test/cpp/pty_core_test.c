/* Tests of pty_core on a plain Linux. Build and run: see the "Native terminal tests" step of CI. */
#define _DEFAULT_SOURCE
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "pty_core.h"

static int fails = 0;

#define CHECK(condition)                                            \
  do {                                                              \
    if (!(condition)) {                                             \
      printf("FAIL line %d: %s\n", __LINE__, #condition);           \
      fails++;                                                      \
    }                                                               \
  } while (0)

/* Reads until the end of the output. */
static void read_all(int fd, char *out, size_t capacity) {
  size_t used = 0;
  ssize_t count;
  while ((count = asm_pty_read(fd, out + used, capacity - used - 1)) > 0) {
    used += (size_t)count;
  }
  out[used] = 0;
}

int main(void) {
  char *envp[] = {"TERM=xterm-256color", "FOO=bar", NULL};
  char out[4096];
  int fd;
  pid_t pid;
  int result;

  /* Output, environment and exit code. */
  char *argv1[] = {"sh", "-c", "echo hello; echo $FOO; exit 3", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv1, envp, NULL, 24, 80, &fd, &pid) == 0);
  read_all(fd, out, sizeof out);
  CHECK(strstr(out, "hello") != NULL);
  CHECK(strstr(out, "bar") != NULL);
  CHECK(asm_pty_wait(pid) == 3);
  close(fd);

  /* Window size, a real terminal, and the environment. */
  char *argv2[] = {"sh", "-c", "stty size; tty; echo $TERM", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv2, envp, NULL, 30, 100, &fd, &pid) == 0);
  read_all(fd, out, sizeof out);
  CHECK(strstr(out, "30 100") != NULL);
  CHECK(strstr(out, "/dev/pts/") != NULL);
  CHECK(strstr(out, "xterm-256color") != NULL);
  CHECK(asm_pty_wait(pid) == 0);
  close(fd);

  /* Resize while the process runs, and input. */
  char *argv3[] = {"sh", "-c", "read line; echo got:$line; stty size", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv3, envp, NULL, 10, 20, &fd, &pid) == 0);
  CHECK(asm_pty_set_size(fd, 40, 120) == 0);
  CHECK(asm_pty_write(fd, "abc\n", 4) == 4);
  read_all(fd, out, sizeof out);
  CHECK(strstr(out, "got:abc") != NULL);
  CHECK(strstr(out, "40 120") != NULL);
  CHECK(asm_pty_wait(pid) == 0);
  close(fd);

  /* Working directory. */
  char *argv4[] = {"sh", "-c", "pwd", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv4, envp, "/tmp", 24, 80, &fd, &pid) == 0);
  read_all(fd, out, sizeof out);
  CHECK(strstr(out, "/tmp") != NULL);
  CHECK(asm_pty_wait(pid) == 0);
  close(fd);

  /* A program or a directory that does not exist reports ENOENT and leaves no zombie. */
  char *argv5[] = {"nothing", NULL};
  result = asm_pty_spawn("/no/such/program", argv5, envp, NULL, 24, 80, &fd, &pid);
  CHECK(result == -2);
  result = asm_pty_spawn("/bin/sh", argv5, envp, "/no/such/dir", 24, 80, &fd, &pid);
  CHECK(result == -2);

  /* A signal reaches the whole process group: the shell and the sleep it started. */
  char *argv6[] = {"sh", "-c", "sleep 100 & wait", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv6, envp, NULL, 24, 80, &fd, &pid) == 0);
  usleep(200000);
  CHECK(asm_pty_signal(pid, SIGKILL) == 0);
  CHECK(asm_pty_wait(pid) == -SIGKILL);
  read_all(fd, out, sizeof out); /* ends instead of hanging */
  close(fd);

  /* Control-C typed on the terminal reaches the foreground program. */
  char *argv7[] = {"sh", "-c", "sleep 100; echo after", NULL};
  CHECK(asm_pty_spawn("/bin/sh", argv7, envp, NULL, 24, 80, &fd, &pid) == 0);
  usleep(200000);
  CHECK(asm_pty_write(fd, "\x03", 1) == 1);
  read_all(fd, out, sizeof out);
  CHECK(strstr(out, "after") == NULL);
  asm_pty_wait(pid);
  close(fd);

  printf(fails ? "%d FAILED\n" : "all passed\n", fails);
  return fails != 0;
}
