#!/usr/bin/env bash
#
# remote-install.sh — build + install + launch the debug APK on a phone reachable
# over the internet via Tailscale (WireGuard, UDP hole-punched; DERP TCP relay if
# direct fails). Connects with `adb connect <host>:<port>` and retries with
# reconnect logic if the link is flaky.
#
# Prereqs (one-time):
#   1. Tailscale installed + logged in on BOTH this Mac and the phone (same tailnet).
#   2. scripts/device-bootstrap.sh has been run (adbd listening on 0.0.0.0:5555).
#      Re-run bootstrap after every phone reboot (tcpip mode is not persistent).
#
# Usage:
#   scripts/remote-install.sh <phone-tailscale-ip-or-magicdns-name>
#   FITBRO_PHONE=phone.tailnet.ts.net scripts/remote-install.sh
#
set -euo pipefail

ADB="${ADB:-adb}"
PORT="${FITBRO_ADB_PORT:-5555}"
PKG="com.mettyoung.fitbro"
PHONE="${1:-${FITBRO_PHONE:-}}"

if [[ -z "${PHONE:-}" ]]; then
  echo "Usage: scripts/remote-install.sh <phone-tailscale-ip-or-name>" >&2
  echo "   or: FITBRO_PHONE=<ip-or-name> scripts/remote-install.sh" >&2
  if command -v tailscale >/dev/null 2>&1; then
    echo >&2; echo "Tailnet peers (pick the phone):" >&2
    tailscale status 2>/dev/null | awk 'NR>0{print "  " $1 "  " $2}' >&2 || true
  fi
  exit 1
fi

TARGET="$PHONE:$PORT"

is_online() {
  "$ADB" -s "$TARGET" get-state 2>/dev/null | grep -q '^device$'
}

connect_once() {
  "$ADB" connect "$TARGET" >/dev/null 2>&1 || true
  is_online
}

echo "Connecting to $TARGET ..."
connected=false
for attempt in 1 2 3 4; do
  if connect_once; then connected=true; break; fi
  echo "  attempt $attempt failed; reconnecting ..."
  "$ADB" disconnect "$TARGET" >/dev/null 2>&1 || true
  "$ADB" reconnect offline >/dev/null 2>&1 || true
  # Nudge / verify the tailnet path (direct or via DERP) if the CLI is available.
  if command -v tailscale >/dev/null 2>&1; then
    tailscale ping -c 2 "$PHONE" 2>&1 | tail -1 || true
  fi
  sleep 2
done

if [[ "$connected" != true ]]; then
  cat >&2 <<EOF
ERROR: could not reach $TARGET.

Checklist:
  - Phone Tailscale is up:        (phone) Tailscale app shows "Connected"
  - This Mac is on the tailnet:   tailscale status
  - Path exists:                  tailscale ping $PHONE
  - adbd is in tcpip mode:        re-run scripts/device-bootstrap.sh on LAN/USB
                                  (required again after any phone reboot)
EOF
  exit 1
fi

# Silent path: build the small minified `remote` variant (~12 MB vs 73 MB debug),
# then adb install + launch — no tap. Override with FITBRO_VARIANT=debug.
VARIANT="${FITBRO_VARIANT:-remote}"
CAP="$(tr '[:lower:]' '[:upper:]' <<<"${VARIANT:0:1}")${VARIANT:1}"
APK="composeApp/build/outputs/apk/${VARIANT}/composeApp-${VARIANT}.apk"

echo "Connected. Building ($VARIANT) ..."
# Assemble then install via `adb -s` so ONLY the remote device is targeted.
# (`installDebug` ignores the injected serial and installs to every attached device.)
./gradlew ":composeApp:assemble${CAP}"
[[ -f "$APK" ]] || { echo "APK not found at $APK" >&2; exit 1; }

echo "Installing (silent) $APK -> $TARGET ..."
"$ADB" -s "$TARGET" install -r "$APK"

echo "Launching $PKG ..."
"$ADB" -s "$TARGET" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

echo "Done — installed + launched on $TARGET (no tap needed)"
