# Global Gradle build lock

Serializes local Gradle builds so that **at most one build runs at a time on a
machine**, across every git worktree and every terminal/Claude Code window.

## Why

All worktrees and the main checkout share one `~/.gradle` (`GRADLE_USER_HOME`
is not customized): the daemon registry, the dependency cache, and — crucially
— a push-enabled local build cache (`caches/build-cache-1`) plus a shared
`.tmp/` staging dir. Two builds running at once contend on that shared
*writable* state, which can corrupt cached task outputs (observed as spurious
test failures), and two ~10 GiB daemons oversubscribe RAM on a 32 GiB box.
Serializing removes both problems without the cost of per-worktree cache
isolation.

## How it works

`gradlew`'s final `exec` is routed through `with-build-lock.sh`, which (on a
local, non-CI machine) hands off to `build_lock.py`. That script holds an
exclusive `fcntl.flock` on a shared lock file and then `execvp`s the JVM.
Because the lock lives on the open file description, it **releases
automatically when the build process exits or is killed** — there is no stale
lock to clean up. macOS has no `flock(1)`, which is why this uses `python3`.

The first lock attempt is non-blocking, so a "waiting for the global Gradle
build lock…" line is printed only when a build is actually queued behind
another.

## When the lock is skipped (runs unlocked)

`with-build-lock.sh` runs the build directly, with no serialization, when any
of these hold — so it is inert on CI and can never break a build:

- `BUILD_NUMBER` is set — Jenkins, the only Gradle CI here.
- `CI` is set — any other CI runner. (Note: some local tools export `CI`; if it is set in your shell the lock silently no-ops. This is fail-safe — the build still runs, just unserialized — but unset it locally if you want serialization.)
- `MEGA_BUILD_LOCK=off` — session-level bypass.
- `python3` is not on `PATH`.

## Tunables (environment variables)

| Variable | Default | Purpose |
| --- | --- | --- |
| `MEGA_BUILD_LOCK` | *(unset)* | `off` disables the lock for the shell/session. |
| `MEGA_BUILD_LOCK_FILE` | `~/.gradle/.mega-build.lock` | Lock file path (the machine-wide rendezvous point). |
| `MEGA_BUILD_WORKERS` | `4` | `--max-workers` injected **only when proceeding unlocked** (the `MEGA_BUILD_LOCK_TIMEOUT` fallback), where another build really is running concurrently, to bound CPU/RAM. A normally-locked build is the only build running, so it keeps Gradle's default parallelism. Never applied on CI; skipped if the caller already passes `--max-workers`. |
| `MEGA_BUILD_LOCK_TIMEOUT` | `0` | Seconds to wait before **proceeding unlocked** instead of blocking forever (`0` = wait indefinitely). A safety valve against a hung build wedging the queue. |

## Caveats

- **`--continuous` / `-t` builds hold the lock forever.** Run them with
  `MEGA_BUILD_LOCK=off ./gradlew -t …` so they don't block every other build.
- **Android Studio bypasses the lock** (it drives Gradle via the tooling API,
  not `./gradlew`). This tool targets terminal / Claude Code builds only.
- **`./gradlew wrapper` regeneration overwrites `gradlew`** and drops the
  route-through. The patched wrapper's final block must read:
  ```sh
  if [ -f "$APP_HOME/tools/gradle/with-build-lock.sh" ]; then
      exec bash "$APP_HOME/tools/gradle/with-build-lock.sh" "$JAVACMD" "$@"
  fi
  exec "$JAVACMD" "$@"
  ```
  Re-apply it after any wrapper upgrade.
