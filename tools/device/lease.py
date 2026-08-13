#!/usr/bin/env python3
"""Arbitrate access to Android test devices across concurrent Claude Code windows.

Hands out ONE device (physical or an on-demand emulator) per git worktree so two
windows/agents never drive the same device, and so a flavored connected-test run
is always pinned to a specific serial instead of fanning out to every device.

Leases are lock DIRECTORIES (``<serial>.lease.d/``) created with an atomic
``os.mkdir`` under a fixed, shared store (``~/.mega-device-leases`` by default) so
every worktree/window on the machine agrees. Each holds a ``meta`` file. Leases
are keyed on the git worktree root, and reaped when their TTL lapses (heartbeat
renewed on every allowed device command) or, for same-host processes, when the
owning pid is gone.

Subcommands: acquire, release, renew, list, reap, steal, whoami, gate.
See tools/device/README.md for the full contract and env tunables.
"""
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time

HOME = os.path.expanduser("~")

STORE = os.environ.get("MEGA_DEVICE_LEASE_DIR") or os.path.join(
    HOME, ".mega-device-leases")
DEFAULT_TTL = int(os.environ.get("MEGA_LEASE_TTL") or 2700)
MAX_EMULATORS = int(os.environ.get("MEGA_MAX_EMULATORS") or 2)
# AVDs to boot the emulator pool from. If unset, discovered live via
# `emulator -list-avds` — nothing device-specific is baked in.
_ENV_AVDS = [a.strip() for a in (
    os.environ.get("MEGA_EMULATOR_AVDS") or "").split(",") if a.strip()]
BOOT_TIMEOUT = int(os.environ.get("MEGA_EMULATOR_BOOT_TIMEOUT") or 180)
EMULATOR_GPU = os.environ.get("MEGA_EMULATOR_GPU") or "swiftshader_indirect"

EXIT_OK = 0
EXIT_CALLER = 1
EXIT_NONE_FREE = 3


# --- SDK tool resolution ------------------------------------------------------

def _sdk_root():
    root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if root:
        return root
    for base in (os.getcwd(), os.path.dirname(os.path.abspath(__file__))):
        lp = os.path.join(base, "..", "..", "local.properties")
        try:
            with open(lp) as fh:
                for line in fh:
                    if line.startswith("sdk.dir="):
                        return line.split("=", 1)[1].strip()
        except OSError:
            pass
    return os.path.join(HOME, "Library", "Android", "sdk")


def adb_bin():
    return shutil.which("adb") or os.path.join(_sdk_root(), "platform-tools", "adb")


def emulator_bin():
    return shutil.which("emulator") or os.path.join(_sdk_root(), "emulator", "emulator")


def _run(args, timeout=15):
    try:
        p = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
        return p.returncode, (p.stdout or "").strip(), (p.stderr or "").strip()
    except (subprocess.TimeoutExpired, OSError) as exc:
        return 1, "", str(exc)


# --- device discovery ---------------------------------------------------------

def connected_devices():
    """Return {serial: state} for every adb-visible device."""
    rc, out, _ = _run([adb_bin(), "devices"])
    devices = {}
    if rc != 0:
        return devices
    for line in out.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2:
            devices[parts[0]] = parts[1]
    return devices


def connected_physical():
    return [s for s, st in connected_devices().items()
            if st == "device" and not s.startswith("emulator-")]


# --- lease store --------------------------------------------------------------

def _safe(serial):
    return re.sub(r"[^A-Za-z0-9._-]", "_", serial)


def lease_dir(serial):
    return os.path.join(STORE, _safe(serial) + ".lease.d")


def read_meta(path):
    meta = {}
    try:
        with open(os.path.join(path, "meta")) as fh:
            for line in fh:
                if "=" in line:
                    k, v = line.rstrip("\n").split("=", 1)
                    meta[k] = v
    except OSError:
        pass
    return meta


def write_meta(path, meta):
    tmp = os.path.join(path, "meta.tmp")
    with open(tmp, "w") as fh:
        for k, v in meta.items():
            fh.write("%s=%s\n" % (k, v))
        fh.flush()
        os.fsync(fh.fileno())
    os.replace(tmp, os.path.join(path, "meta"))


def all_leases():
    out = []
    try:
        for name in os.listdir(STORE):
            if name.endswith(".lease.d"):
                path = os.path.join(STORE, name)
                meta = read_meta(path)
                if meta.get("serial"):
                    out.append((meta["serial"], meta, path))
    except OSError:
        pass
    return out


# --- identity / liveness ------------------------------------------------------

def hostname():
    rc, out, _ = _run(["hostname"], timeout=5)
    return out if rc == 0 and out else "unknown"


def owner_for(cwd):
    cwd = cwd or os.getcwd()
    rc, out, _ = _run(["git", "-C", cwd, "rev-parse", "--show-toplevel"], timeout=3)
    return out if rc == 0 and out else os.path.abspath(cwd)


def _now():
    return int(time.time())


def _proc_alive(pid):
    try:
        os.kill(int(pid), 0)
        return True
    except (OSError, ValueError):
        return False


def _is_stale(meta):
    # The owner `pid` is the ephemeral per-command shell that invoked `acquire`
    # (dead within seconds of the command returning), so it is NOT a staleness
    # signal — that would reclaim a live lease immediately. The heartbeat (renewed
    # by the gate on every allowed device command, and by `renew`) plus the TTL
    # is the liveness signal for physical leases.
    if meta.get("kind") == "emulator":
        # An emulator lease also carries the long-lived emulator process pid,
        # which IS a reliable signal: if that process is gone, reclaim.
        epid = meta.get("emulator_pid")
        if epid and not _proc_alive(epid):
            return True
        if meta.get("state") == "booting":
            try:
                if _now() - int(meta.get("acquired_at", "0")) > BOOT_TIMEOUT + 60:
                    return True
            except ValueError:
                return True
    try:
        hb = int(meta.get("heartbeat_at", "0"))
        ttl = int(meta.get("ttl", DEFAULT_TTL))
    except ValueError:
        return True
    return _now() - hb > ttl


# --- emulator lifecycle -------------------------------------------------------

def _terminate_pid(pid):
    try:
        pid = int(pid)
    except (TypeError, ValueError):
        return
    # start_new_session=True made the emulator its own process-group leader, so
    # kill the group; fall back to the bare pid.
    for kill in (lambda: os.killpg(os.getpgid(pid), signal.SIGTERM),
                 lambda: os.kill(pid, signal.SIGTERM)):
        try:
            kill()
            return
        except OSError:
            continue


def _kill_emulator(serial, meta=None):
    _run([adb_bin(), "-s", serial, "emu", "kill"], timeout=10)
    if meta and meta.get("emulator_pid"):
        _terminate_pid(meta["emulator_pid"])


def _wait_for_boot(serial, deadline):
    _run([adb_bin(), "-s", serial, "wait-for-device"],
         timeout=max(1, int(deadline - time.monotonic())))
    while time.monotonic() < deadline:
        rc, out, _ = _run(
            [adb_bin(), "-s", serial, "shell", "getprop", "sys.boot_completed"],
            timeout=10)
        if rc == 0 and out.strip() == "1":
            return True
        time.sleep(2)
    return False


def _boot_emulator(avd, port, lease_path):
    """Boot a headless read-only emulator on `port`; return its pid once booted, else None."""
    serial = "emulator-%d" % port
    log = open(os.path.join(lease_path, "emulator.log"), "w")
    args = [
        emulator_bin(), "-avd", avd, "-port", str(port),
        "-read-only", "-no-window", "-no-boot-anim", "-no-audio",
        "-no-snapshot-save", "-gpu", EMULATOR_GPU,
        "-netdelay", "none", "-netspeed", "full",
    ]
    try:
        proc = subprocess.Popen(args, stdout=log, stderr=subprocess.STDOUT,
                                stdin=subprocess.DEVNULL, start_new_session=True,
                                close_fds=True)
    except OSError as exc:
        sys.stderr.write("lease: failed to launch emulator: %s\n" % exc)
        return None
    finally:
        log.close()  # the child dup'd the fd; the parent handle must not leak

    # Persist the pid before the (slow) boot wait so release/reap can still kill
    # this process if we're interrupted mid-boot.
    meta = read_meta(lease_path)
    meta["emulator_pid"] = str(proc.pid)
    write_meta(lease_path, meta)

    if _wait_for_boot(serial, time.monotonic() + BOOT_TIMEOUT):
        meta = read_meta(lease_path)
        meta["state"] = "ready"
        write_meta(lease_path, meta)
        return proc.pid
    _terminate_pid(proc.pid)
    return None


def _available_avds():
    if _ENV_AVDS:
        return _ENV_AVDS
    rc, out, _ = _run([emulator_bin(), "-list-avds"], timeout=15)
    if rc != 0:
        return []
    return [line.strip() for line in out.splitlines() if line.strip()]


def _emulator_count():
    return sum(1 for _, m, _ in all_leases() if m.get("kind") == "emulator")


# --- claim / meta -------------------------------------------------------------

def _claim(serial, kind, owner, ttl, extra=None):
    """Atomically claim `serial`; return the lease path or None if already held."""
    os.makedirs(STORE, exist_ok=True)
    path = lease_dir(serial)
    try:
        os.mkdir(path)
    except FileExistsError:
        return None
    meta = {
        "serial": serial,
        "kind": kind,
        "owner": owner,
        "session_id": os.environ.get("CLAUDE_SESSION_ID", ""),
        "pid": str(os.getppid()),
        "host": hostname(),
        "acquired_at": str(_now()),
        "heartbeat_at": str(_now()),
        "ttl": str(ttl),
    }
    if extra:
        meta.update(extra)
    write_meta(path, meta)
    return path


def _remove_lease(serial, meta=None, path=None):
    path = path or lease_dir(serial)
    meta = meta if meta is not None else read_meta(path)
    if meta.get("kind") == "emulator" or serial.startswith("emulator-"):
        _kill_emulator(serial, meta)
    shutil.rmtree(path, ignore_errors=True)


def _device_model(serial):
    rc, out, _ = _run(
        [adb_bin(), "-s", serial, "shell", "getprop", "ro.product.model"],
        timeout=10)
    return out.strip() if rc == 0 and out.strip() else ""


def _label(serial):
    if serial.startswith("emulator-"):
        return "emulator"
    return _device_model(serial) or serial


def _age(meta):
    try:
        return _now() - int(meta.get("acquired_at", _now()))
    except ValueError:
        return 0


def _print_holders():
    leases = all_leases()
    if not leases:
        sys.stderr.write("  (no active leases)\n")
        return
    for serial, meta, _ in leases:
        sys.stderr.write("  %s [%s] held by %s (%ds ago)\n" % (
            serial, _label(serial), meta.get("owner", "?"), _age(meta)))


# --- reaping ------------------------------------------------------------------

ORPHAN_GRACE = 30  # seconds — don't touch a dir that may be a claim mid-write


def reap():
    reclaimed = []
    try:
        names = os.listdir(STORE)
    except OSError:
        return reclaimed
    for name in names:
        if not name.endswith(".lease.d"):
            continue
        path = os.path.join(STORE, name)
        meta = read_meta(path)
        if meta.get("serial"):
            if _is_stale(meta):
                _remove_lease(meta["serial"], meta, path)
                reclaimed.append(meta["serial"])
            continue
        # A lease dir with no valid meta = a crash between mkdir and write_meta.
        # Remove it once it is clearly not an in-flight claim, so it can't wedge
        # that serial's dir (mkdir would keep failing) forever.
        try:
            age = _now() - int(os.path.getmtime(path))
        except OSError:
            age = ORPHAN_GRACE + 1
        if age > ORPHAN_GRACE:
            shutil.rmtree(path, ignore_errors=True)
            reclaimed.append(name)
    return reclaimed


# --- argument parsing ---------------------------------------------------------

def _flag(args, name):
    return name in args


def _opt(args, name):
    if name in args:
        i = args.index(name)
        if i + 1 < len(args):
            return args[i + 1]
    return None


# --- subcommands --------------------------------------------------------------

def cmd_acquire(args):
    reap()
    owner = owner_for(os.getcwd())
    ttl = int(_opt(args, "--ttl") or DEFAULT_TTL)
    want = _opt(args, "--serial")
    prefer = _opt(args, "--prefer")
    physical_only = _flag(args, "--physical-required")
    emulator_only = _flag(args, "--emulator-only")

    leased = {s: m for s, m, _ in all_leases()}

    # Already hold one for this worktree? Reuse it (idempotent).
    for serial, meta in leased.items():
        if meta.get("owner") == owner and (want is None or want == serial):
            _renew_serial(serial)
            print(serial)
            return EXIT_OK

    if want:
        if want in leased:
            sys.stderr.write("lease: %s is already held by %s\n" % (
                want, leased[want].get("owner", "?")))
            return EXIT_NONE_FREE
        if want in connected_physical():
            if _claim(want, "physical", owner, ttl):
                print(want)
                return EXIT_OK
            sys.stderr.write("lease: %s was just claimed by another window\n" % want)
            return EXIT_NONE_FREE
        sys.stderr.write("lease: %s is not a free connected device\n" % want)
        return EXIT_CALLER

    if not emulator_only:
        free = [s for s in connected_physical() if s not in leased]
        if prefer and prefer in free:
            free = [prefer] + [s for s in free if s != prefer]
        for serial in free:
            if _claim(serial, "physical", owner, ttl):
                print(serial)
                return EXIT_OK

    if not physical_only:
        return _acquire_emulator(owner, ttl)

    sys.stderr.write("lease: no free physical device.\n")
    _print_holders()
    return EXIT_NONE_FREE


def _acquire_emulator(owner, ttl):
    if _emulator_count() >= MAX_EMULATORS:
        sys.stderr.write(
            "lease: emulator cap reached (MEGA_MAX_EMULATORS=%d); none free.\n"
            % MAX_EMULATORS)
        _print_holders()
        return EXIT_NONE_FREE
    avds = _available_avds()
    if not avds:
        sys.stderr.write(
            "lease: no AVD available to boot. Create one (Android Studio or "
            "avdmanager), or set MEGA_EMULATOR_AVDS.\n")
        return EXIT_CALLER
    avd = avds[0]
    busy = set(connected_devices())
    for port in range(5554, 5586, 2):
        serial = "emulator-%d" % port
        if serial in busy:
            continue
        path = _claim(serial, "emulator", owner, ttl,
                     extra={"avd": avd, "console_port": str(port), "state": "booting"})
        if not path:
            continue
        # Claim-then-verify against a concurrent acquire that also passed the cap
        # check before either had claimed a slot.
        if _emulator_count() > MAX_EMULATORS:
            _remove_lease(serial, path=path)
            sys.stderr.write(
                "lease: emulator cap reached (MEGA_MAX_EMULATORS=%d).\n" % MAX_EMULATORS)
            return EXIT_NONE_FREE
        sys.stderr.write("lease: booting emulator %s from AVD %s…\n" % (serial, avd))
        if _boot_emulator(avd, port, path) is not None:
            print(serial)
            return EXIT_OK
        _remove_lease(serial, path=path)
        sys.stderr.write("lease: emulator %s failed to boot within %ds.\n" % (
            serial, BOOT_TIMEOUT))
        return EXIT_NONE_FREE
    sys.stderr.write("lease: no free emulator console port.\n")
    return EXIT_NONE_FREE


def _renew_serial(serial):
    path = lease_dir(serial)
    meta = read_meta(path)
    if not meta:
        return False
    meta["heartbeat_at"] = str(_now())
    try:
        write_meta(path, meta)
    except OSError:
        return False
    return True


def cmd_release(args):
    owner = owner_for(os.getcwd())
    want = _opt(args, "--serial")
    released = []
    for serial, meta, path in all_leases():
        if meta.get("owner") == owner and (want is None or want == serial):
            _remove_lease(serial, meta, path)
            released.append(serial)
    for s in released:
        sys.stderr.write("lease: released %s\n" % s)
    return EXIT_OK


def cmd_renew(args):
    owner = owner_for(os.getcwd())
    want = _opt(args, "--serial")
    n = 0
    for serial, meta, _ in all_leases():
        if meta.get("owner") == owner and (want is None or want == serial):
            if _renew_serial(serial):
                n += 1
    return EXIT_OK if n else EXIT_CALLER


def cmd_list(_args):
    leases = {s: m for s, m, _ in all_leases()}
    print("%-18s %-18s %-8s %-8s %s" % ("SERIAL", "DEVICE", "KIND", "AGE", "OWNER"))
    for serial in connected_physical():
        m = leases.get(serial)
        owner = m.get("owner", "") if m else "(free)"
        age = "%ds" % _age(m) if m else "-"
        print("%-18s %-18s %-8s %-8s %s" % (
            serial, _label(serial), "physical", age, owner))
    for serial, m, _ in all_leases():
        if m.get("kind") == "emulator":
            print("%-18s %-18s %-8s %-8s %s" % (
                serial, m.get("avd", "emulator"), "emulator",
                "%ds" % _age(m), m.get("owner", "")))
    return EXIT_OK


def cmd_reap(_args):
    for s in reap():
        sys.stderr.write("lease: reaped stale lease %s\n" % s)
    return EXIT_OK


def cmd_steal(args):
    want = _opt(args, "--serial")
    if not want:
        sys.stderr.write("lease: steal requires --serial\n")
        return EXIT_CALLER
    path = lease_dir(want)
    if not os.path.isdir(path):
        sys.stderr.write("lease: %s is not leased\n" % want)
        return EXIT_OK
    _remove_lease(want, path=path)
    sys.stderr.write("lease: force-released %s\n" % want)
    return EXIT_OK


def cmd_whoami(_args):
    owner = owner_for(os.getcwd())
    mine = [s for s, m, _ in all_leases() if m.get("owner") == owner]
    for s in mine:
        print(s)
    return EXIT_OK if mine else EXIT_CALLER


# --- PreToolUse gate ----------------------------------------------------------

_CONNECTED_RE = re.compile(r"connected[A-Za-z]*AndroidTest\b")
_SERIAL_ENV_RE = re.compile(r"ANDROID_SERIAL=([^\s]+)")
_ADB_S_RE = re.compile(r"\badb\b[^|;&]*?\s-s\s+([^\s]+)")
_ADB_RE = re.compile(r"\badb\b")
# adb subcommands that act on a device (vs. adb devices/version/start-server…).
_ADB_DIRECTED_RE = re.compile(
    r"\badb\b.*\b(shell|install|uninstall|push|pull|emu|forward|reverse|sync|"
    r"logcat|screencap|screenrecord|remount)\b")


def _decision(kind, reason=None):
    out = {"hookEventName": "PreToolUse", "permissionDecision": kind}
    if reason:
        out["permissionDecisionReason"] = reason
    print(json.dumps({"hookSpecificOutput": out}))


def cmd_gate(_args):
    try:
        payload = json.loads(sys.stdin.read() or "{}")
    except (ValueError, OSError):
        return EXIT_OK
    command = (payload.get("tool_input") or {}).get("command") or ""
    cwd = payload.get("cwd") or os.getcwd()
    if not command:
        return EXIT_OK

    is_connected = bool(_CONNECTED_RE.search(command)) and "gradlew" in command
    has_android_serial = bool(_SERIAL_ENV_RE.search(command))
    m = _SERIAL_ENV_RE.search(command) or _ADB_S_RE.search(command)
    token = m.group(1) if m else None
    # ANDROID_SERIAL=$SERIAL (the documented flow) arrives unexpanded; a token
    # containing `$` can't be resolved from the raw command string.
    dynamic = bool(token) and "$" in token
    # A bare `adb shell/install/…` with no serial targets the single default
    # device — which may be one another worktree holds.
    directed_bare_adb = (
        not token and not has_android_serial
        and bool(_ADB_RE.search(command)) and bool(_ADB_DIRECTED_RE.search(command)))

    if not is_connected and not token and not directed_bare_adb:
        return EXIT_OK

    owner = owner_for(cwd)
    leases = {s: meta for s, meta, _ in all_leases()}

    serial = token if (token and not dynamic) else None
    if dynamic:
        # Resolve $SERIAL to this worktree's own lease when it holds exactly one.
        mine = [s for s, meta in leases.items() if meta.get("owner") == owner]
        if len(mine) == 1:
            serial = mine[0]

    if is_connected and not has_android_serial and not serial:
        _decision("deny",
                  "Connected instrumented tests without ANDROID_SERIAL fan out to "
                  "every attached device and collide with other agents. Reserve a "
                  "device first:  SERIAL=$(tools/device/lease acquire)  then run  "
                  "ANDROID_SERIAL=$SERIAL ./gradlew :app:connectedGmsDebugAndroidTest …")
        return EXIT_OK

    if serial:
        holder = leases.get(serial)
        if holder and holder.get("owner") != owner:
            _decision("deny",
                      "Device %s is leased by another worktree (%s). Use a different "
                      "device or run: tools/device/lease acquire" % (
                          serial, holder.get("owner", "?")))
            return EXIT_OK
        if holder and holder.get("owner") == owner:
            _renew_serial(serial)
            _decision("allow")
            return EXIT_OK
        if is_connected:
            _decision("deny",
                      "Device %s is not leased by this worktree. Reserve it first: "
                      "tools/device/lease acquire --serial %s" % (serial, serial))
            return EXIT_OK
        return EXIT_OK

    if directed_bare_adb:
        devs = list(connected_devices())
        if len(devs) == 1:
            holder = leases.get(devs[0])
            if holder and holder.get("owner") != owner:
                _decision("deny",
                          "This bare adb command targets %s — the only connected "
                          "device — which is leased by another worktree (%s). Pin "
                          "ANDROID_SERIAL / adb -s to a device you hold, or run "
                          "tools/device/lease acquire." % (
                              devs[0], holder.get("owner", "?")))
                return EXIT_OK
    return EXIT_OK


COMMANDS = {
    "acquire": cmd_acquire,
    "release": cmd_release,
    "renew": cmd_renew,
    "list": cmd_list,
    "reap": cmd_reap,
    "steal": cmd_steal,
    "whoami": cmd_whoami,
    "gate": cmd_gate,
}


def main(argv):
    if not argv or argv[0] not in COMMANDS:
        sys.stderr.write("usage: lease {%s} [args]\n" % "|".join(COMMANDS))
        return EXIT_CALLER
    try:
        return COMMANDS[argv[0]](argv[1:])
    except Exception as exc:  # never let the gate hook block a build on a bug
        sys.stderr.write("lease: %s: %s\n" % (argv[0], exc))
        return EXIT_OK if argv[0] == "gate" else EXIT_CALLER


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
