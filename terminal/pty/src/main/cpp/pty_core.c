#define _GNU_SOURCE
#include "pty_core.h"

#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static int set_cloexec(int fd) {
  int flags = fcntl(fd, F_GETFD);
  if (flags < 0) {
    return -errno;
  }
  return fcntl(fd, F_SETFD, flags | FD_CLOEXEC) < 0 ? -errno : 0;
}

int asm_pty_set_size(int master_fd, int rows, int columns) {
  struct winsize size;
  memset(&size, 0, sizeof(size));
  size.ws_row = (unsigned short)rows;
  size.ws_col = (unsigned short)columns;
  return ioctl(master_fd, TIOCSWINSZ, &size) < 0 ? -errno : 0;
}

/* Runs in the child after fork: only calls that are safe there. Never returns. */
static void run_child(const char *slave_name, int error_fd, const char *path, char *const argv[],
                      char *const envp[], const char *cwd) {
  sigset_t none;
  int fd;
  int saved;
  int max_fd;

  /* A new session without a controlling terminal, so that the slave becomes ours. */
  if (setsid() < 0) {
    goto fail;
  }
  fd = open(slave_name, O_RDWR);
  if (fd < 0) {
    goto fail;
  }
#ifdef TIOCSCTTY
  if (ioctl(fd, TIOCSCTTY, 0) < 0) {
    goto fail;
  }
#endif
  if (dup2(fd, 0) < 0 || dup2(fd, 1) < 0 || dup2(fd, 2) < 0) {
    goto fail;
  }
  if (fd > 2) {
    close(fd);
  }

  /* The program starts with default signal handling and nothing blocked. */
  signal(SIGHUP, SIG_DFL);
  signal(SIGINT, SIG_DFL);
  signal(SIGQUIT, SIG_DFL);
  signal(SIGTERM, SIG_DFL);
  signal(SIGCHLD, SIG_DFL);
  signal(SIGPIPE, SIG_DFL);
  sigemptyset(&none);
  sigprocmask(SIG_SETMASK, &none, NULL);

  /* Close every other descriptor except the pipe that reports a failed exec. */
  max_fd = (int)sysconf(_SC_OPEN_MAX);
  if (max_fd < 0 || max_fd > 4096) {
    max_fd = 4096;
  }
  for (fd = 3; fd < max_fd; fd++) {
    if (fd != error_fd) {
      close(fd);
    }
  }

  if (cwd != NULL && chdir(cwd) < 0) {
    goto fail;
  }
  execve(path, argv, envp);

fail:
  saved = errno;
  /* The write end is close-on-exec: a successful exec closes it and the parent reads nothing. */
  if (write(error_fd, &saved, sizeof(saved)) < 0) {
    /* Nothing more can be done. */
  }
  _exit(127);
}

int asm_pty_spawn(const char *path, char *const argv[], char *const envp[], const char *cwd,
                  int rows, int columns, int *master_fd, pid_t *pid) {
  char slave_name[128];
  int master;
  int error_pipe[2];
  int result;
  pid_t child;
  int child_errno = 0;
  ssize_t got;

  master = open("/dev/ptmx", O_RDWR | O_NOCTTY | O_CLOEXEC);
  if (master < 0) {
    return -errno;
  }
  if (grantpt(master) < 0 || unlockpt(master) < 0 ||
      ptsname_r(master, slave_name, sizeof(slave_name)) != 0) {
    result = -errno;
    close(master);
    return result;
  }

  /* Raw defaults that a login shell expects: UTF-8 input, CR to NL, echo, canonical mode. */
  {
    struct termios tio;
    int slave = open(slave_name, O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (slave >= 0) {
      if (tcgetattr(slave, &tio) == 0) {
#ifdef IUTF8
        tio.c_iflag |= IUTF8;
#endif
        tio.c_iflag |= ICRNL;
        tio.c_iflag &= ~(IXON | IXOFF);
        tio.c_oflag |= OPOST | ONLCR;
        tio.c_lflag |= ICANON | ISIG | ECHO | ECHOE | ECHOK | IEXTEN;
        tcsetattr(slave, TCSANOW, &tio);
      }
      close(slave);
    }
  }

  result = asm_pty_set_size(master, rows, columns);
  if (result < 0) {
    close(master);
    return result;
  }

  if (pipe(error_pipe) < 0) {
    result = -errno;
    close(master);
    return result;
  }
  if (set_cloexec(error_pipe[0]) < 0 || set_cloexec(error_pipe[1]) < 0) {
    result = -errno;
    close(error_pipe[0]);
    close(error_pipe[1]);
    close(master);
    return result;
  }

  child = fork();
  if (child < 0) {
    result = -errno;
    close(error_pipe[0]);
    close(error_pipe[1]);
    close(master);
    return result;
  }
  if (child == 0) {
    run_child(slave_name, error_pipe[1], path, argv, envp, cwd);
  }

  close(error_pipe[1]);
  do {
    got = read(error_pipe[0], &child_errno, sizeof(child_errno));
  } while (got < 0 && errno == EINTR);
  close(error_pipe[0]);

  if (got == (ssize_t)sizeof(child_errno)) {
    /* The exec failed; collect the child so that no zombie stays. */
    int status;
    while (waitpid(child, &status, 0) < 0 && errno == EINTR) {
    }
    close(master);
    return -child_errno;
  }

  *master_fd = master;
  *pid = child;
  return 0;
}

int asm_pty_wait(pid_t pid) {
  int status;
  pid_t done;
  do {
    done = waitpid(pid, &status, 0);
  } while (done < 0 && errno == EINTR);
  if (done < 0) {
    return -1000 - errno;
  }
  if (WIFEXITED(status)) {
    return WEXITSTATUS(status);
  }
  if (WIFSIGNALED(status)) {
    return -WTERMSIG(status);
  }
  return -1000 - ECHILD;
}

int asm_pty_signal(pid_t pid, int signal_number) {
  return kill(-pid, signal_number) < 0 ? -errno : 0;
}

ssize_t asm_pty_read(int master_fd, void *buffer, size_t length) {
  ssize_t count;
  do {
    count = read(master_fd, buffer, length);
  } while (count < 0 && errno == EINTR);
  if (count < 0) {
    /* EIO is how a pseudo-terminal master reports that the slave side is gone. */
    return errno == EIO ? 0 : -errno;
  }
  return count;
}

ssize_t asm_pty_write(int master_fd, const void *buffer, size_t length) {
  const char *data = (const char *)buffer;
  size_t done = 0;
  while (done < length) {
    ssize_t count = write(master_fd, data + done, length - done);
    if (count < 0) {
      if (errno == EINTR) {
        continue;
      }
      return -errno;
    }
    done += (size_t)count;
  }
  return (ssize_t)done;
}
