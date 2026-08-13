# Device + emulator access coordinator

Arbitrates the machine's Android test devices between multiple concurrent Claude
Code windows/agents so two of them never drive the same device, and so a
connected instrumented-test run is always pinned to one serial instead of
fanning out to every attached device.

## Resource model

- **Physical devices** — discovered live from `adb devices`: whatever happens to
  be attached (zero, one, or many, any model). Nothing device-specific is baked in.
- **Emulator pool** — up to `MEGA_MAX_EMULATORS` (default 2) headless emulators
  booted on demand when every physical device is busy and the caller did not
  require real hardware. Booted from an existing AVD with `-read-only` (so
  several instances can share one AVD image) and torn down on release/reap.

## Leases

A lease is a lock **directory** `<serial>.lease.d/` created with an atomic
`mkdir` under a fixed, shared store so every worktree/window on the machine
agrees:

```
${MEGA_DEVICE_LEASE_DIR:-~/.mega-device-leases}/<serial>.lease.d/meta
```

Leases are **keyed on the git worktree root** of the caller (a window and its
subagents share one worktree, hence one lease). Each is reclaimed when its TTL
lapses (the heartbeat is renewed on every allowed device command) or, for a
process on this same host, when the owning pid is gone. `acquire` always reaps
stale leases first, so a crashed window never deadlocks a device.

## Usage

```bash
SERIAL=$(tools/device/lease acquire)               # reserve a free device (physical, else emulator)
SERIAL=$(tools/device/lease acquire --prefer <serial>)      # bias toward a specific device
SERIAL=$(tools/device/lease acquire --serial <serial>)      # demand one exact device
SERIAL=$(tools/device/lease acquire --physical-required)    # never fall back to an emulator
SERIAL=$(tools/device/lease acquire --emulator-only)        # force a fresh emulator
ANDROID_SERIAL=$SERIAL ./gradlew :app:connectedGmsDebugAndroidTest
tools/device/lease list                            # show devices + who holds what
tools/device/lease whoami                          # serial(s) this worktree holds
tools/device/lease renew                           # bump the heartbeat
tools/device/lease release                         # free this worktree's device(s) (+ kill any emulator)
tools/device/lease steal --serial <s>              # force-reclaim a stuck lease
tools/device/lease reap                            # drop stale leases now
```

`acquire` prints the assigned serial to stdout and exits 0; exit 3 means nothing
is free (holders are printed to stderr).

## Enforcement hook

`.claude/hooks/pre-tool-use-device-gate.sh` (a 3rd PreToolUse/Bash hook →
`lease gate`) enforces the Bash transport:

- **deny** a flavored `connected*AndroidTest` with no `ANDROID_SERIAL` (it would
  fan out to every device and collide);
- **deny** an `ANDROID_SERIAL` / `adb -s` targeting a device leased by another
  worktree;
- **allow** (and heartbeat) a device this worktree holds;
- defer everything else.

Bypass for a session: `export MEGA_DEVICE_GATE=off`.

## Environment tunables

| Variable | Default | Purpose |
| --- | --- | --- |
| `MEGA_DEVICE_LEASE_DIR` | `~/.mega-device-leases` | Shared lease store (must be one path all worktrees agree on). |
| `MEGA_MAX_EMULATORS` | `2` | Hard cap on concurrently-booted emulators (protects RAM). |
| `MEGA_EMULATOR_AVDS` | *(auto-discovered)* | Comma-separated AVD name(s) to boot from. If unset, discovered via `emulator -list-avds` and the first is used. Must already exist (see below). |
| `MEGA_EMULATOR_BOOT_TIMEOUT` | `180` | Seconds to wait for `sys.boot_completed` before giving up. |
| `MEGA_EMULATOR_GPU` | `swiftshader_indirect` | `-gpu` mode for headless boots. |
| `MEGA_LEASE_TTL` | `2700` | Lease lifetime (s) before it is considered stale. |
| `MEGA_DEVICE_GATE` | *(unset)* | `off` disables the enforcement hook for the session. |

## One-time setup: emulator AVD

The coordinator only **boots/kills** emulators; it does not create AVDs
(`avdmanager create` is blocked by the bash-guard hook). Create at least one
phone AVD once. If you have several, set `MEGA_EMULATOR_AVDS` to choose which one
is booted (otherwise the first from `emulator -list-avds` is used):

```bash
$ANDROID_HOME/emulator/emulator -list-avds
```

## Interaction with the Gradle build lock

The Part-1 global build lock (`tools/gradle/`) serializes **all** `./gradlew`
invocations, so gradle-driven `connected*AndroidTest` runs are already
serialized — the coordinator's role there is to pin each run to one device and
prevent fan-out/collision, not to parallelize them. Its immediate parallelism
win is for interactive mobile-mcp/`adb` use, which the build lock does not cover.

## Follow-ups (not in this MVP)

- **mobile-mcp coverage** via an `mcp__mobile-mcp__*` PreToolUse matcher (needs
  the installed server's device-select tool name confirmed).
- **SessionEnd auto-release** hook (TTL + PID reaping cover crashes today).
- **Build-once / fan-out** execution to run on-device tests in parallel across
  the leased devices (assemble once under the build lock, then `am instrument`
  per device off the lock).
