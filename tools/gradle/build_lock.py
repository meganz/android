#!/usr/bin/env python3
"""Serialize local Gradle builds behind one machine-wide exclusive lock.

Invoked from ``with-build-lock.sh`` (itself routed to from the ``gradlew``
wrapper) as::

    build_lock.py <JAVACMD> <java args...>

Holds ``fcntl.flock(LOCK_EX)`` on a shared lock file, then ``os.execvp``s the
JVM. The lock lives on the open file description, so it releases automatically
when the build process exits or is killed — there is no stale-PID to reap.

The exclusive-lock first attempt is non-blocking so the "waiting" note is only
printed when a build is actually queued behind another.

Environment:
  MEGA_BUILD_LOCK_FILE     lock path (default: ~/.gradle/.mega-build.lock)
  MEGA_BUILD_WORKERS       --max-workers to inject (default: 4; skipped if the
                           caller already passed --max-workers)
  MEGA_BUILD_LOCK_TIMEOUT  seconds to wait before proceeding UNLOCKED rather
                           than blocking forever (default: 0 = wait forever)
"""
import fcntl
import os
import sys
import time


def main():
    argv = sys.argv[1:]
    if not argv:
        sys.exit("build_lock: no command to run")

    lock_path = os.environ.get("MEGA_BUILD_LOCK_FILE") or os.path.join(
        os.path.expanduser("~"), ".gradle", ".mega-build.lock")
    workers = (os.environ.get("MEGA_BUILD_WORKERS") or "4").strip()
    try:
        timeout = float(os.environ.get("MEGA_BUILD_LOCK_TIMEOUT", "0") or 0)
    except ValueError:
        timeout = 0.0

    lock_dir = os.path.dirname(lock_path)
    if lock_dir:
        os.makedirs(lock_dir, exist_ok=True)
    fd = os.open(lock_path, os.O_RDWR | os.O_CREAT, 0o644)
    os.set_inheritable(fd, True)

    acquired = True
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        sys.stderr.write(
            "⏳ waiting for the global Gradle build lock "
            "(another build is running on this machine)…\n")
        sys.stderr.flush()
        if timeout > 0:
            acquired = False
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline:
                try:
                    fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    acquired = True
                    break
                except OSError:
                    time.sleep(1)
            if not acquired:
                sys.stderr.write(
                    "build_lock: timed out after %ds; proceeding unlocked "
                    "(bounding parallelism to --max-workers=%s)\n"
                    % (int(timeout), workers or "default"))
                sys.stderr.flush()
        else:
            fcntl.flock(fd, fcntl.LOCK_EX)

    # A held lock guarantees this is the only build running, so leave Gradle's
    # default parallelism alone. Only bound workers on the proceed-UNLOCKED
    # path, where another build really is running concurrently.
    if not acquired and workers and not any(
            a == "--max-workers" or a.startswith("--max-workers=")
            for a in argv):
        argv = argv + ["--max-workers=%s" % workers]

    try:
        os.execvp(argv[0], argv)
    except OSError as exc:
        sys.exit("build_lock: failed to exec %s: %s" % (argv[0], exc))


if __name__ == "__main__":
    main()
