---
name: analyze-native-crash
description: >    
  Symbolicate Android native crash logs from Firebase Crashlytics: /analyze-native-crash --version "16.4(261070708)(f33dd33fe8)" --log crash.txt
  Automatically downloads the matching debug libmega.so from Artifactory using the app version,
  then runs llvm-addr2line to resolve PC addresses to function names and source locations.
triggers:
  - /analyze-native-crash
  - symbolicate crash
  - native crash log
  - addr2line
  - libmega.so crash
---

# Native Crash Symbolication

Parses a Firebase Crashlytics native crash log, automatically downloads the matching
debug `libmega.so` from Artifactory (cached by SDK version), resolves each PC address
using `llvm-addr2line`, and outputs an annotated stack trace with root cause analysis.

## Usage

```
/analyze-native-crash --version "16.7(261600931)"                                       # Commit optional — looked up automatically
/analyze-native-crash --version "16.4(261070708)(f33dd33fe8)" --log <file>             # Read crash log from a file
/analyze-native-crash --so <path> --log <file>                                          # Use a local libmega.so directly
/analyze-native-crash --version "16.7(261600931)" --ndk <path>                         # Specify NDK root explicitly
/analyze-native-crash --version "16.7(261600931)" --proxy socks5://127.0.0.1:1080      # Specify proxy
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `--version <ver>` | App version string from Crashlytics. The commit hash is **optional** — if omitted it is resolved automatically from the build number via Artifactory. | `--version "16.7(261600931)"` or `--version "16.4(261070708)(f33dd33fe8)"` |
| `--log <file>` | Path to a crash log text file | `--log ~/Downloads/crash.txt` |
| `--so <path>` | Path to a local debug `libmega.so` (skips auto-download) | `--so ~/Downloads/libmega.so` |
| `--ndk <path>` | NDK root directory | `--ndk ~/Library/Android/sdk/ndk/27.1.12297006` |
| `--proxy <url>` | Proxy for Artifactory downloads | `--proxy socks5://127.0.0.1:1080` |

Either `--version` or `--so` is required. If both are provided, `--so` takes precedence.

If `--proxy` is not provided, try downloading without proxy. If the download fails, inform the user and suggest specifying `--proxy`.

---

## Execution Steps

### Step 1 — Collect the Crash Log

**If `--log <file>` was provided:** Read the file using the Read tool.

**Otherwise:** Use the crash log text pasted by the user in the current conversation message.
If no crash log text is visible in the conversation, ask the user to paste it.

### Step 2 — Locate `llvm-addr2line`

**If `--ndk` was provided**, use it directly:
```
<ndk>/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-addr2line
```

**Otherwise**, resolve via shell in this priority order:
1. `$NDK_ROOT`
2. `$ANDROID_NDK_ROOT`
3. `$ANDROID_NDK`
4. Auto-discover: `ls -d ~/Library/Android/sdk/ndk/* | sort -rV | head -1`

> **Sandbox note:** Do NOT redirect to `/dev/null` (or any path outside the
> project root) — the Bash sandbox blocks output redirects whose target is
> outside the project directory and the command fails with
> `Layer 1: Redirect to path outside project directory — /dev/null`.
> To silence stderr, fold it into stdout with `2>&1`, or just let it print.

```bash
NDK="${NDK_ROOT:-${ANDROID_NDK_ROOT:-${ANDROID_NDK:-$(ls -d ~/Library/Android/sdk/ndk/* 2>&1 | sort -rV | head -1)}}}"
ADDR2LINE="$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-addr2line"
echo "NDK=$NDK"
echo "ADDR2LINE=$ADDR2LINE"
ls "$ADDR2LINE" && echo "OK" || echo "ERROR: not found"
```

If not found, halt and tell the user to install the NDK or set `NDK_ROOT`.

### Step 3 — Resolve `libmega.so`

**If `--so <path>` was provided**, use that path directly and skip to Step 4.

**Otherwise**, auto-download using `--version`:

**Proxy setup (do this first).** Do NOT hard-code any proxy host/port — other users may
not need a proxy at all, or may use a different one. Build a `PROXY_OPT` variable from the
`--proxy` argument and reuse it in every `curl` call below. If `--proxy` was not provided,
`PROXY_OPT` stays an empty array and curl connects directly:

```bash
# Set PROXY only if the user passed --proxy <url>; otherwise leave it unset/empty.
PROXY="<value of --proxy, or empty if not provided>"
# Build PROXY_OPT as an array: empty when no proxy, otherwise (--proxy <url>).
PROXY_OPT=()
[ -n "$PROXY" ] && PROXY_OPT=(--proxy "$PROXY")
```

If a direct (no-proxy) request fails, tell the user the download failed and suggest
re-running with `--proxy <url>` (e.g. `--proxy socks5://127.0.0.1:1080`).

#### 3a — Parse the version string

From `--version` extract:
- `MAJOR` = `16.7` (required)
- `BUILD` = `261600931` (required)
- `COMMIT` = `fa9705cdf5` (**optional** — may be absent, e.g. `--version "16.7(261600931)"`)

#### 3b — Resolve the commit hash (only if not provided)

The commit is **not required** from the user. The symbol file is ultimately keyed by SDK
version, not commit — the commit is only used to locate `buildinfo.txt`. If `COMMIT` is
missing, look it up from the Artifactory release directory using the build number, which is
unique. The folder name has the form `{BUILD}_{COMMIT}`:

```bash
# Only run this if COMMIT was not given on the command line.
COMMIT=$(curl "${PROXY_OPT[@]}" -s "https://artifactory.developers.mega.co.nz/artifactory/api/storage/android-mega/release/v${MAJOR}/" \
  | tr ',' '\n' | grep -o "${BUILD}_[0-9a-f]*" | head -1 | cut -d_ -f2)
echo "Resolved COMMIT=$COMMIT"
```

If this returns nothing (build too old / purged, or the listing needs a proxy), tell the
user and ask them to supply the commit in `--version`, or pass a local `--so` instead.

#### 3c — Download buildinfo.txt

```bash
BUILDINFO_URL="https://artifactory.developers.mega.co.nz/artifactory/android-mega/release/v${MAJOR}/${BUILD}_${COMMIT}/buildinfo.txt"
curl "${PROXY_OPT[@]}" -sf "$BUILDINFO_URL"
```

Example URL for version `16.4(261070708)(f33dd33fe8)`:
```
https://artifactory.developers.mega.co.nz/artifactory/android-mega/release/v16.4/261070708_f33dd33fe8/buildinfo.txt
```

The response looks like:
```
Version: v16.4(261070708)(f33dd33fe8)
Upload Time: Fri Apr 17 19:26:35 NZST 2026
Android: branch(release/v16.4) - commit(f33dd33fe8d94e929ee78a63227a4088866ceec3)
SDK version: 20260417.015208-rel
```

Extract the `SDK version` field (e.g. `20260417.015208-rel`).

#### 3d — Check local cache

Symbol files are cached by SDK version under the project root:
```
<project_root>/symbols/<sdk_version>/libmega.so
```

Check if the file already exists. `SDK_VER` is **not** a fixed value — set it to the
`SDK version` you extracted from buildinfo.txt in 3c (it looks like `20260417.015208-rel`):
```bash
SDK_VER="<SDK version from buildinfo.txt, e.g. 20260417.015208-rel>"
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
SO_PATH="${PROJECT_ROOT}/symbols/${SDK_VER}/libmega.so"
[ -f "$SO_PATH" ] && ls -lh "$SO_PATH" && echo "Cache hit" || echo "Need to download"
```

If the file exists, use it directly and skip 3e.

#### 3e — Download libmega.so

The symbol file is stored inside a zip archive in Artifactory. Use the `!` syntax to
extract the specific file directly.

**First, fetch the REAL size** with a HEAD request and report the actual bytes to the
user — never assume or hard-code a size, it changes from build to build:

```bash
# Reuse the same SDK_VER from 3c/3d — the real value from buildinfo.txt,
# e.g. 20260417.015208-rel. Do not hard-code the example.
SDK_VER="<SDK version from buildinfo.txt, e.g. 20260417.015208-rel>"
SO_URL="https://artifactory.developers.mega.co.nz/artifactory/android-mega/cicd/native-symbol/${SDK_VER}.zip!/arm64-v8a/libmega.so"

BYTES=$(curl "${PROXY_OPT[@]}" -sI "$SO_URL" | awk 'tolower($1) == "content-length:" { gsub(/\r/, "", $2); print $2 }')
echo "Symbol file size: ${BYTES} bytes (~$(( BYTES / 1024 / 1024 )) MB)"
```

Report the actual size to the user (e.g. `The symbol file is 340 MB. Downloading now.`).
Confirm before downloading only if the size seems unexpectedly large (well over ~500MB).

**Then download.** Write the output by **redirecting to a path inside the project**
(`> "$SO_PATH"`) rather than `curl -o`, which the execution sandbox blocks:

```bash
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
SO_DIR="${PROJECT_ROOT}/symbols/${SDK_VER}"
SO_PATH="${SO_DIR}/libmega.so"

mkdir -p "$SO_DIR"
curl "${PROXY_OPT[@]}" -s "$SO_URL" > "$SO_PATH"
ls -lh "$SO_PATH"
```

All Artifactory `curl` calls use the same `"${PROXY_OPT[@]}"` built in the proxy-setup step
above — pass a proxy only when the user supplied `--proxy`, otherwise connect directly.

### Step 4 — Parse the Crash Log

Read the crash log text (from Step 1) and identify:

1. **Signal line** — the first line, e.g. `Crashed: Thread: SIGSEGV  0x0000000000000008`
2. **Stack frames** — lines matching `#NN pc 0xADDR  LIBRARY (BuildId: ...)`:
    - Extract frame number, hex address, and library name from each line
    - Split into `libmega_frames` (library == `libmega.so`) and `other_frames`
3. **BuildId** — extract from the first `libmega.so` frame, e.g. `5642dae2e6736fa132edbe42ad00e99aa9528978`

Do this parsing yourself by reading the text — no script needed.

### Step 5 — Symbolicate `libmega.so` Frames

Run a **single** `llvm-addr2line` call with all `libmega.so` addresses at once:

```bash
<ADDR2LINE> -f -C -e <SO_PATH> <addr1> <addr2> <addr3> ...
```

Example:
```bash
"$ADDR2LINE" \
  -f -C -e "$SO_PATH" \
  0x2415144 0x1021848 0x1020614 0x11cd758 0x11ca794 0x109e1e4 0xe98170 0xe98054
```

The output is pairs of lines per address: `function_name\nsource_file:line`.
Map each pair back to its frame in order.

A result of `??` / `??:0` means the address is unresolved (stripped symbol table).

### Step 6 — Output Annotated Stack Trace

Print the fully annotated stack trace. For each frame:

- **SIGSEGV near 0x0 (e.g. 0x8, 0x10, 0x18)**: near-null offset dereference — accessing a member of a null object. Mark the signal line accordingly.
- **SIGSEGV at exactly 0x0**: true null pointer dereference.
- **Resolved `libmega.so` frame**: show function name and source location on the next line
- **Unresolved `libmega.so` frame**: show address with `(no symbol)`
- **Other library frame** (libc, etc.): mark as `(external — not symbolicated)`

**Output format:**

```
Native Crash Symbolication Report
==================================
Signal    : SIGSEGV  0x0000000000000008   ← near-null offset (member of null object)
BuildId   : 5642dae2e6736fa132edbe42ad00e99aa9528978
SDK Ver   : 20260417.015208-rel

Symbolicated Stack Trace:
--------------------------
#00  pc 0x8bea4      libc.so      (external — not symbolicated)
#01  pc 0x2415144    libmega.so   std::__ndk1::recursive_mutex::lock()
                                  (no source — inlined/system)
#02  pc 0x1021848    libmega.so   std::lock_guard<recursive_mutex>::lock_guard(...)
                                  __mutex/lock_guard.h:35
#03  pc 0x1020614    libmega.so   LibwebsocketsClient::wsCallback(...)
                                  MEGAchat/src/net/libwebsocketsIO.cpp:535
#04  pc 0x11cd758    libmega.so   lws_client_connect_3_connect
                                  libwebsockets/connect3.c:728
...
#09  pc 0x109e1e4    libmega.so   mega::LibuvWaiter::wait()
                                  MEGAchat/src/waiter/libuvWaiter.cpp:58
#10  pc 0xe98170     libmega.so   megachat::MegaChatApiImpl::loop()
                                  MEGAchat/src/megachatapi_impl.cpp:166
#11  pc 0xe98054     libmega.so   megachat::MegaChatApiImpl::threadEntryPoint(void*)
                                  MEGAchat/src/megachatapi_impl.cpp:153
#12  pc 0x8a714      libc.so      (external — not symbolicated)
#13  pc 0x7b3b4      libc.so      (external — not symbolicated)

Summary
-------
Total frames   : 14
libmega frames : 11  (10 resolved, 1 unresolved)
Other frames   : 3

Root Cause Analysis
-------------------
<Inspect the top resolved frames and explain the likely root cause.>
<SIGSEGV at small non-zero offset (0x8, 0x10…): accessing member of null object — identify which object and member.>
<SIGSEGV at 0x0: true null pointer dereference — name the function where it occurs.>
<If the crash is deep in libuv/libc, trace upward to find the MEGA code responsible.>
```

### Step 7 — Root Cause & Next Steps

Based on the symbolicated trace:

1. **SIGSEGV at small non-zero offset (0x8, 0x10, 0x18…)** → member access on a null object. The offset corresponds to a field in the object (e.g., mutex at offset 8). Look for use-after-free or missing null check.
2. **SIGSEGV at exactly 0x0** → null pointer dereference. Identify which pointer in the nearest resolved frame could be null.
3. **Crash in libuv** (`uv__work_done`, `uv__run_timers`, etc.) → the libuv event loop thread crashed. Check what async work or timer callback was executing.
4. **Crash in `megachatapi_impl.cpp`** → the MegaChat event loop thread. Look at what operation `loop()` was executing at the time.
5. **BuildId check**: compare the BuildId from the crash log against the `libmega.so` you downloaded. If they differ, symbolication may be wrong — ensure the SDK version matches.

## Notes

- If `--proxy` was provided, pass it to every `curl` call via `--proxy <url>`. Otherwise try without proxy; if the download fails, tell the user to specify `--proxy`.
- Symbol files are cached at `<project_root>/symbols/<sdk_version>/libmega.so` — reused across sessions.
- `libmega.so` from Artifactory is ~350MB unstripped with DWARF symbols. A stripped `.so` returns `??`.
- `llvm-addr2line` accepts multiple addresses in a single invocation — always batch them.
- On macOS the toolchain path is `darwin-x86_64`; on Linux it is `linux-x86_64`.
- `libc.so` and other system libraries cannot be symbolicated without matching device system symbols.
