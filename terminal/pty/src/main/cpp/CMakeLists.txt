cmake_minimum_required(VERSION 3.18.1)
project(asm_pty C)

set(CMAKE_C_STANDARD 99)

add_library(asm_pty SHARED pty_core.c pty_jni.c)
target_compile_options(asm_pty PRIVATE -Wall -Wextra -Werror)
