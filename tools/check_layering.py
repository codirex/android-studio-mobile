#!/usr/bin/env python3
"""Enforces the module rules of ADR-0003. Run from anywhere: python3 tools/check_layering.py

It reads the build files and sources only, so it needs no Gradle and runs in seconds.
Exit status 1 means at least one rule is broken; every violation is printed.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKIPPED_DIRS = {"build-logic", "build", ".git", ".gradle", "tools", "docs"}
LAYERS = {"app", "feature", "domain", "data", "core", "terminal"}
PROJECT_DEP = re.compile(r'project\(\s*"(:[^"]+)"\s*\)')
ANDROID_IMPORT = re.compile(r"^\s*import\s+(static\s+)?(android|androidx)\.", re.M)
DOMAIN_FORBIDDEN_IN_BUILD = re.compile(r"libs\.(androidx|hilt|clotho)|com\.android\.|asm\.android\.")


def find_modules():
    modules = {}
    for build_file in ROOT.rglob("build.gradle.kts"):
        rel = build_file.parent.relative_to(ROOT)
        if rel == Path(".") or any(part in SKIPPED_DIRS for part in rel.parts):
            continue
        modules[":" + ":".join(rel.parts)] = build_file
    return modules


def layer_of(module):
    return module.split(":")[1]


def allowed(src, dst):
    s, d = layer_of(src), layer_of(dst)
    if s == "app":
        return True
    if s == "feature":
        return d in ("domain", "core") or dst == ":terminal:protocol"
    if s == "domain":
        return d == "domain"
    if s in ("data", "core"):
        return d == "core" if s == "core" else d in ("domain", "core")
    if s == "terminal":
        return d in ("terminal", "core")
    return False


def find_cycle(graph):
    state = {}

    def visit(node, path):
        state[node] = 1
        for nxt in graph.get(node, ()):
            if state.get(nxt) == 1:
                return path + [node, nxt]
            if nxt not in state:
                found = visit(nxt, path + [node])
                if found:
                    return found
        state[node] = 2
        return None

    for node in graph:
        if node not in state:
            found = visit(node, [])
            if found:
                return found
    return None


def main():
    modules = find_modules()
    errors = []
    graph = {}

    for module, build_file in sorted(modules.items()):
        text = build_file.read_text(encoding="utf-8")
        if layer_of(module) not in LAYERS:
            errors.append(f"{module}: unknown layer '{layer_of(module)}', expected one of {sorted(LAYERS)}")
            continue

        deps = sorted(set(PROJECT_DEP.findall(text)))
        graph[module] = deps
        for dep in deps:
            if dep not in modules:
                errors.append(f"{module}: depends on {dep}, which does not exist")
            elif not allowed(module, dep):
                errors.append(f"{module} -> {dep}: not allowed for layer '{layer_of(module)}' (ADR-0003 rules 1 and 2)")

        if layer_of(module) == "domain":
            if DOMAIN_FORBIDDEN_IN_BUILD.search(text):
                errors.append(f"{module}: a domain build file must not use Android, Hilt or Clotho (ADR-0003 rule 3)")
            for source in build_file.parent.rglob("*"):
                if source.suffix in (".java", ".kt") and ANDROID_IMPORT.search(source.read_text(encoding="utf-8")):
                    errors.append(f"{source.relative_to(ROOT)}: Android import in a domain module (ADR-0003 rule 3)")

        if module != ":core:concurrency" and re.search(r"libs\.clotho", text):
            errors.append(f"{module}: only :core:concurrency may depend on Clotho (ADR-0003 rule 4)")

        if module != ":app" and "asm.android.application" in text:
            errors.append(f"{module}: only :app may be an application module")

    cycle = find_cycle(graph)
    if cycle:
        errors.append("dependency cycle: " + " -> ".join(cycle))

    if errors:
        print(f"Layering check failed ({len(errors)} problem(s)):")
        for error in errors:
            print(f"  - {error}")
        return 1
    print(f"Layering check passed for {len(modules)} module(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
