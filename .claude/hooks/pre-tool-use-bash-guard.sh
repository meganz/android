#!/usr/bin/env bash
# Three-Layer Bash Command Defense Hook for Claude Code (Android)
# Layer 1: Dangerous pattern detection → DENY
# Layer 2: Blacklist interception → DENY
# Layer 3: Whitelist auto-allow → ALLOW
# Fallback: pass to user for judgment

set -uo pipefail

# Read JSON from stdin
INPUT=$(cat)

# Check jq availability; if missing, don't block
if ! command -v jq &>/dev/null; then
    exit 0
fi

# Extract the command string
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
if [[ -z "$COMMAND" ]]; then
    exit 0
fi

# --- Helper functions ---

deny() {
    jq -n --arg reason "$1" '{
        hookSpecificOutput: {
            hookEventName: "PreToolUse",
            permissionDecision: "deny",
            permissionDecisionReason: $reason
        }
    }'
    exit 0
}

allow() {
    jq -n '{
        hookSpecificOutput: {
            hookEventName: "PreToolUse",
            permissionDecision: "allow"
        }
    }'
    exit 0
}

# Split compound commands into segments (by |, ;, &&, ||)
# Store in array to avoid subshell issues
split_segments() {
    local cmd="$1"
    # Replace compound operators with newlines (order matters: || before |)
    echo "$cmd" | sed -E 's/\|\|/\n/g; s/&&/\n/g; s/;/\n/g; s/\|/\n/g'
}

# Extract the base command from a segment
get_base_cmd() {
    echo "$1" | sed 's/^[[:space:]]*//' | sed 's/^[A-Z_]*=[^ ]* //' | awk '{print $1}'
}

# Strip the bodies of single- and double-quoted argument strings.
# Used by Layer 1 path/redirect checks so literal examples inside argument
# strings (e.g. a `git commit -m "...'..foo'..."` message body) don't
# trigger false-positive denies. State is carried across newlines so
# multi-line quoted bodies (heredoc-substituted commit messages) also work.
# Limitations: heredoc bodies fed via `<<` are not stripped, and escaped
# quotes inside a quoted body are not handled.
strip_quoted() {
    awk 'BEGIN { sq=0; dq=0 } {
        line=""
        for (i=1; i<=length($0); i++) {
            c=substr($0,i,1)
            if (sq) { if (c=="\047") sq=0; continue }
            if (dq) { if (c=="\"") dq=0; continue }
            if (c=="\047") { sq=1; continue }
            if (c=="\"")   { dq=1; continue }
            line = line c
        }
        print line
    }'
}

# ============================================================
# LAYER 1: Dangerous Pattern Detection → DENY
# Structural patterns that are dangerous regardless of command
# ============================================================

# Fork bomb patterns
if echo "$COMMAND" | grep -qE ':\(\)\s*\{.*\|.*&\s*\}'; then
    deny "Layer 1: Fork bomb pattern detected"
fi

# Pipe-to-shell: curl/wget piped to bash/sh/zsh/python
if echo "$COMMAND" | grep -qE '\|\s*(bash|sh|zsh|dash|python3?|ruby|perl|node)\b'; then
    deny "Layer 1: Pipe-to-shell detected — command output piped to interpreter"
fi

# Directory traversal: only flag '..' when it appears as a path segment
# (./.., /../, /..$). Avoids false positives on git ranges (master..feature),
# version strings (v1.0..v2.0), and other non-path uses of '..'.
# Run against the quote-stripped command so literal examples inside an
# argument string (e.g. text inside a `git commit -m "..."` body) don't
# trigger false denies.
if echo "$COMMAND" | strip_quoted | grep -qE '(^|\s)\.\./|/\.\./|/\.\.$'; then
    deny "Layer 1: Directory traversal detected — '..' in file path"
fi

# Redirect to ~ (home directory paths)
if echo "$COMMAND" | grep -qE '>\s*~'; then
    deny "Layer 1: Redirect to home directory path"
fi

# Redirect to absolute path outside project directory
redirect_target=$(echo "$COMMAND" | grep -oE '>\s*/[^ ]+' | head -1 | sed 's/^>[[:space:]]*//')
if [[ -n "$redirect_target" && -n "${CLAUDE_PROJECT_DIR:-}" ]] && [[ "$redirect_target" != "$CLAUDE_PROJECT_DIR"* ]]; then
    deny "Layer 1: Redirect to path outside project directory — $redirect_target"
fi

# dd writing to disk
if echo "$COMMAND" | grep -qE '\bdd\b.*\bof='; then
    deny "Layer 1: dd write operation detected"
fi

# /dev/sd* or /dev/disk* direct access
if echo "$COMMAND" | grep -qE '/dev/(sd[a-z]|disk[0-9]|nvme)'; then
    deny "Layer 1: Direct disk device access detected"
fi

# ============================================================
# LAYER 2: Blacklist Interception → DENY
# Known dangerous/write commands
# ============================================================

check_blacklist() {
    local cmd="$1"
    local base
    base=$(get_base_cmd "$cmd")

    case "$base" in
        rm|rmdir)
            deny "Layer 2: Destructive file removal — $base"
            ;;
        sudo|su)
            deny "Layer 2: Privilege escalation — $base"
            ;;
        eval|exec)
            deny "Layer 2: Dynamic execution — $base"
            ;;
        kill|killall|pkill)
            deny "Layer 2: Process termination — $base"
            ;;
        ssh|scp|sftp)
            deny "Layer 2: Remote access — $base"
            ;;
        chmod|chown|chgrp)
            deny "Layer 2: Permission modification — $base"
            ;;
        mkfs|fdisk|mount|umount)
            deny "Layer 2: Disk operation — $base"
            ;;
        systemctl|launchctl)
            deny "Layer 2: Service management — $base"
            ;;
        wget)
            deny "Layer 2: wget downloads files by default"
            ;;
        tee)
            deny "Layer 2: tee writes to files"
            ;;
        mv)
            deny "Layer 2: File move/rename"
            ;;
        cp)
            deny "Layer 2: File copy — may overwrite"
            ;;
        git)
            if echo "$cmd" | grep -qE '\bgit\s+(reset|clean)\b'; then
                deny "Layer 2: Destructive git command — reset/clean"
            fi
            if echo "$cmd" | grep -qE '\bgit\s+push\b.*(-f\b|--force\b|--force-with-lease\b)'; then
                deny "Layer 2: Force push — destructive"
            fi
            if echo "$cmd" | grep -qE '\bgit\s+checkout\s+--\s*\.'; then
                deny "Layer 2: git checkout -- . discards all changes"
            fi
            if echo "$cmd" | grep -qE '\bgit\s+restore\s+\.'; then
                deny "Layer 2: git restore . discards all changes"
            fi
            ;;
        curl)
            if echo "$cmd" | grep -qE '\bcurl\b.*\s-[A-Za-z]*[xX]\s*(POST|PUT|DELETE|PATCH)'; then
                deny "Layer 2: curl with write HTTP method"
            fi
            if echo "$cmd" | grep -qE '\bcurl\b.*\s--request\s+(POST|PUT|DELETE|PATCH)'; then
                deny "Layer 2: curl with write HTTP method"
            fi
            if echo "$cmd" | grep -qE '\bcurl\b.*\s(-[A-Za-z]*d\b|--data|--data-raw|--data-binary|--data-urlencode|-F\b|--form)'; then
                deny "Layer 2: curl with data upload (-d/--data/-F/--form implies POST)"
            fi
            if echo "$cmd" | grep -qE '\bcurl\b.*\s-[A-Za-z]*[oO]\b'; then
                deny "Layer 2: curl with file output (-o/-O)"
            fi
            if echo "$cmd" | grep -qE '\bcurl\b.*\s--output\b'; then
                deny "Layer 2: curl with file output (--output)"
            fi
            ;;
        pip|pip3)
            if echo "$cmd" | grep -qE '\bpip3?\s+install\b'; then
                deny "Layer 2: pip install — modifies environment"
            fi
            ;;
        npm|npx)
            if echo "$cmd" | grep -qE '\bnpm\s+install\b'; then
                deny "Layer 2: npm install — modifies node_modules"
            fi
            ;;
        brew)
            if echo "$cmd" | grep -qE '\bbrew\s+(install|uninstall|remove|upgrade)\b'; then
                deny "Layer 2: brew package modification"
            fi
            ;;
        # ----- Android-specific denies -----
        adb)
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+install\b'; then
                deny "Layer 2: adb install — modifies installed apps on device"
            fi
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+uninstall\b'; then
                deny "Layer 2: adb uninstall — removes apps from device"
            fi
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+push\b'; then
                deny "Layer 2: adb push — writes files to device"
            fi
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+root\b'; then
                deny "Layer 2: adb root — restarts adbd with root permissions"
            fi
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+reboot\b'; then
                deny "Layer 2: adb reboot — reboots device"
            fi
            if echo "$cmd" | grep -qE '\badb\b.*\bshell\b.*\b(rm|rmdir)\b'; then
                deny "Layer 2: adb shell rm — destroys files on device"
            fi
            if echo "$cmd" | grep -qE '\badb\b.*\bshell\b.*\bpm\s+(install|uninstall|clear|disable|enable)\b'; then
                deny "Layer 2: adb shell pm — modifies installed packages on device"
            fi
            if echo "$cmd" | grep -qE '\badb\b.*\bshell\b.*\bsvc\s+'; then
                deny "Layer 2: adb shell svc — toggles device services"
            fi
            if echo "$cmd" | grep -qE '\badb\b.*\bshell\b.*\bsettings\s+(put|delete)\b'; then
                deny "Layer 2: adb shell settings put/delete — modifies device settings"
            fi
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+logcat\b.*\s-c\b'; then
                deny "Layer 2: adb logcat -c — clears device log buffer"
            fi
            ;;
        sdkmanager)
            if echo "$cmd" | grep -qE '\bsdkmanager\b.*--(install|uninstall|update)\b'; then
                deny "Layer 2: sdkmanager modifies the Android SDK installation"
            fi
            # Without an explicit verb, sdkmanager <pkg> implies install — also block
            if echo "$cmd" | grep -qE '\bsdkmanager\s+[^-]'; then
                deny "Layer 2: sdkmanager <package> implies install — modifies SDK"
            fi
            ;;
        avdmanager)
            if echo "$cmd" | grep -qE '\bavdmanager\s+(create|delete|move)\b'; then
                deny "Layer 2: avdmanager create/delete/move — modifies AVDs"
            fi
            ;;
        emulator)
            # Starting an emulator is fine (no deny), but wiping data is not
            if echo "$cmd" | grep -qE '\bemulator\b.*-wipe-data\b'; then
                deny "Layer 2: emulator -wipe-data — destroys emulator state"
            fi
            ;;
    esac
}

# Read segments into array using process substitution (avoids subshell)
while IFS= read -r segment; do
    [[ -z "$segment" ]] && continue
    check_blacklist "$segment"
done < <(split_segments "$COMMAND")

# ============================================================
# LAYER 3: Whitelist Auto-Allow → ALLOW
# Known safe read-only commands
# ============================================================

check_whitelist() {
    local cmd="$1"
    local base
    base=$(get_base_cmd "$cmd")

    case "$base" in
        # File browsing
        ls|cat|head|tail|file|stat|wc|du|df|less|more)
            return 0 ;;
        # Search
        grep|rg|find|which|whereis|locate|mdfind)
            return 0 ;;
        # Text processing (read-only)
        sort|uniq|cut|awk|tr|diff|comm|jq|column|fmt|fold|expand|unexpand|xmllint)
            return 0 ;;
        sed)
            # sed without -i is safe (stdout only)
            if echo "$cmd" | grep -qE '\bsed\s+-[A-Za-z]*i'; then
                return 1
            fi
            return 0
            ;;
        # Directory info
        pwd|basename|dirname|realpath|tree)
            return 0 ;;
        # System info
        ps|top|uptime|uname|sw_vers|hostname|whoami|id|env|printenv|echo|printf|date|cal)
            return 0 ;;
        # Network read-only
        curl)
            # Already passed Layer 2 blacklist, so no dangerous flags
            return 0 ;;
        ping|nslookup|dig|host|traceroute)
            return 0 ;;
        # Git read-only
        git)
            if echo "$cmd" | grep -qE '\bgit\s+(log|diff|status|branch|show|fetch|stash\s+list|tag|remote|blame|rev-parse|ls-files|ls-tree|shortlog|describe|config\s+--get|config\s+-l|name-rev|reflog|submodule\s+status|submodule\s+foreach\s+--quiet\s+git\s+(status|log|diff|rev-parse))\b'; then
                return 0
            fi
            return 1 ;;
        # ----- Android dev tools -----
        # Gradle wrapper — build/test/lint write to build/, but that's expected dev workflow
        ./gradlew|gradle|gradlew)
            if echo "$cmd" | grep -qE '\b(\./)?gradlew?\s+(--version|-v|--help|-h|tasks|projects|properties|dependencies|dependencyInsight|help|model|outgoingVariants|signingReport|sourceSets|buildEnvironment|javaToolchains|kotlinDslAccessorsReport)\b'; then
                return 0
            fi
            if echo "$cmd" | grep -qE '\b(\./)?gradlew?\s+([^ ]*:)?(check|lint|lintDebug|lintRelease|test|testDebug|testRelease|test[A-Za-z]*UnitTest|build|assemble|assembleDebug|assembleRelease|compile|compileDebug[A-Za-z]*|connectedAndroidTest|connectedDebugAndroidTest|verifyPaparazzi[A-Za-z]*|recordPaparazzi[A-Za-z]*|detekt|ktlintCheck|spotlessCheck|clean)\b'; then
                return 0
            fi
            return 1 ;;
        # Kotlin compiler — version only
        kotlin|kotlinc)
            if echo "$cmd" | grep -qE '\b(kotlin|kotlinc)\s+-version\b'; then
                return 0
            fi
            return 1 ;;
        # Java toolchain — version only (compile/run falls through)
        java|javac)
            if echo "$cmd" | grep -qE '\b(java|javac)\s+-(-)?version\b'; then
                return 0
            fi
            return 1 ;;
        # ADB — only specific read-only subcommands; broader adb falls through to user
        adb)
            # Plain status / version commands
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+(devices|version|--version|get-state|get-serialno|start-server|kill-server|wait-for-device|reconnect)\b'; then
                return 0
            fi
            # logcat (deny -c already filtered in Layer 2)
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+logcat\b'; then
                return 0
            fi
            # bugreport — read-only diagnostic output (large but safe)
            if echo "$cmd" | grep -qE '\badb\b(\s+-\S+)*\s+bugreport\b'; then
                return 0
            fi
            # Read-only shell verbs only
            if echo "$cmd" | grep -qE '\badb\b.*\bshell\s+(getprop|ls|ps|cat|grep|wm\s+(size|density)|pm\s+(list|path|dump)|am\s+(stack|task)|dumpsys|service\s+list|input\s+keyevent|screencap|screenrecord|df|du|getenforce|id|uptime|date|stat|whoami|ip\s+addr|netstat|top\s+-n\s+1)\b'; then
                return 0
            fi
            return 1 ;;
        # Android SDK tools — read-only flags only
        sdkmanager)
            if echo "$cmd" | grep -qE '\bsdkmanager\b\s+(--version|--list|--list_installed)\b'; then
                return 0
            fi
            return 1 ;;
        avdmanager)
            if echo "$cmd" | grep -qE '\bavdmanager\s+list\b'; then
                return 0
            fi
            return 1 ;;
        emulator)
            if echo "$cmd" | grep -qE '\bemulator\s+(-list-avds|-version|-help|-accel-check)\b'; then
                return 0
            fi
            return 1 ;;
        aapt|aapt2)
            if echo "$cmd" | grep -qE '\b(aapt2?|aapt)\s+(version|d|dump)\b'; then
                return 0
            fi
            return 1 ;;
        apksigner)
            if echo "$cmd" | grep -qE '\bapksigner\s+(verify|version|--version)\b'; then
                return 0
            fi
            return 1 ;;
        bundletool)
            if echo "$cmd" | grep -qE '\bbundletool\s+(version|dump|validate|get-size|get-device-spec)\b'; then
                return 0
            fi
            return 1 ;;
        keytool)
            if echo "$cmd" | grep -qE '\bkeytool\s+-(list|printcert|help)\b'; then
                return 0
            fi
            return 1 ;;
        jarsigner)
            if echo "$cmd" | grep -qE '\bjarsigner\s+-verify\b'; then
                return 0
            fi
            return 1 ;;
        # Project's `android` knowledge-base CLI (mentioned in CLAUDE.md)
        android)
            if echo "$cmd" | grep -qE '\bandroid\s+(--version|-v|docs\s+(search|fetch|list))\b'; then
                return 0
            fi
            return 1 ;;
        # Misc dev tools — version only
        python3)
            if echo "$cmd" | grep -qE '\bpython3\s+(--version|-m\s+json\.tool)\b'; then
                return 0
            fi
            return 1 ;;
        node|ruby)
            if echo "$cmd" | grep -qE '\b(node|ruby)\s+--version\b'; then
                return 0
            fi
            return 1 ;;
        # xargs, open, osascript intentionally NOT whitelisted — too powerful
        # They fall through to user judgment
        *)
            return 1 ;;
    esac
}

# Guard: if command contains $(...) or backticks, skip whitelist auto-allow
# (embedded commands can't be statically verified — fall through to user judgment)
if echo "$COMMAND" | grep -qE '\$\(|`'; then
    # Skip whitelist, go straight to fallback
    exit 0
fi

# For whitelist: ALL segments must be whitelisted
all_whitelisted=true
while IFS= read -r segment; do
    [[ -z "$segment" ]] && continue
    if ! check_whitelist "$segment"; then
        all_whitelisted=false
        break
    fi
done < <(split_segments "$COMMAND")

if [[ "$all_whitelisted" == "true" ]]; then
    allow
fi

# ============================================================
# FALLBACK: No match → pass to user for judgment
# ============================================================
exit 0
