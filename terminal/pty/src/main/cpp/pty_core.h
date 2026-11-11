/*
 * Pseudo-terminals for the terminal service (ADR-0023). Plain C with no JNI, so it can be built and
 * tested on any Linux. Written for this project; it does not use code of Termux.
 */
#ifndef ASM_PTY_CORE_H
#define ASM_PTY_CORE_H

#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Starts `path` with `argv` and `envp` (both NULL terminated) in a new session whose controlling
 * terminal is a new pseudo-terminal of size rows x columns, with `cwd` as the working directory
 * (NULL keeps the current one).
 *
 * Returns 0 and sets *master_fd (the master side, close-on-exec) and *pid (also the process
 * group), or returns -errno: of creating the terminal, or of the exec in the child.
 */
int asm_pty_spawn(const char *path, char *const argv[], char *const envp[], const char *cwd,
                  int rows, int columns, int *master_fd, pid_t *pid);

/* Sets the window size of the terminal. Returns 0 or -errno. */
int asm_pty_set_size(int master_fd, int rows, int columns);

/*
 * Waits for the process. Returns its exit code (0 to 255), or minus the number of the signal that
 * ended it, or -1000 - errno if waiting failed.
 */
int asm_pty_wait(pid_t pid);

/* Sends `signal` to the whole process group of `pid`. Returns 0 or -errno. */
int asm_pty_signal(pid_t pid, int signal);

/*
 * Reads from the master side. Returns the number of bytes, 0 at the end of the output (the
 * process has ended and everything was read), or -errno.
 */
ssize_t asm_pty_read(int master_fd, void *buffer, size_t length);

/* Writes all of `length` bytes to the master side. Returns the length or -errno. */
ssize_t asm_pty_write(int master_fd, const void *buffer, size_t length);

#ifdef __cplusplus
}
#endif

#endif
