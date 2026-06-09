#!/usr/bin/env bash
#
# remote-install-ssh.sh — deliver the debug APK to the phone over Tailscale via
# Termux sshd, then pop the Android package installer for a one-tap install.
#
# Robust to ColorOS network/wireless-debugging flaps: it needs only Tailscale +
# Termux sshd (no adb, no wireless debugging). Trade-off: you tap "Install" once
# on the phone (and "install unknown apps" must be allowed for Termux/Termux:API).
#
# Prereqs (one-time, see scripts/termux-setup.md):
#   - Phone: Termux + Termux:API installed; `pkg install openssh termux-api`;
#     Mac public key in ~/.ssh/authorized_keys; sshd running (Termux:Boot for persistence).
#   - Tailscale up on Mac + phone.
#
# Usage:
#   scripts/remote-install-ssh.sh <phone-tailscale-ip-or-name>
#   FITBRO_PHONE=oppo-find-n5 scripts/remote-install-ssh.sh
#
# Env overrides: SSH_PORT (8022), SSH_USER (default: none → ssh picks), REMOTE_APK (~/fitbro-debug.apk)
set -euo pipefail

PHONE="${1:-${FITBRO_PHONE:-}}"
PKG="com.mettyoung.fitbro"
PORT="${SSH_PORT:-8022}"
USER_PART="${SSH_USER:+${SSH_USER}@}"
REMOTE_APK="${REMOTE_APK:-/data/data/com.termux/files/home/fitbro.apk}"
# Default to the minified `remote` variant (~5-8 MB on the wire). Override with
# FITBRO_VARIANT=debug for an unminified build.
VARIANT="${FITBRO_VARIANT:-remote}"
CAP="$(tr '[:lower:]' '[:upper:]' <<<"${VARIANT:0:1}")${VARIANT:1}"
GRADLE_TASK=":composeApp:assemble${CAP}"
APK="composeApp/build/outputs/apk/${VARIANT}/composeApp-${VARIANT}.apk"

if [[ -z "${PHONE:-}" ]]; then
  echo "Usage: scripts/remote-install-ssh.sh <phone-tailscale-ip-or-name>" >&2
  exit 1
fi

SSH_OPTS=(-p "$PORT" -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new)
HOST="${USER_PART}${PHONE}"

echo "Building APK ($VARIANT) ..."
./gradlew "$GRADLE_TASK"
[[ -f "$APK" ]] || { echo "APK not found at $APK" >&2; exit 1; }

echo "Checking SSH to $HOST:$PORT ..."
if ! ssh "${SSH_OPTS[@]}" "$HOST" true 2>/dev/null; then
  cat >&2 <<EOF
ERROR: cannot SSH to $HOST:$PORT.
  - Phone Tailscale connected?      tailscale ping $PHONE
  - Termux sshd running?            (phone) run: sshd   (or set up Termux:Boot)
  - Mac key authorized?             (phone) ~/.ssh/authorized_keys has this Mac's pubkey
EOF
  exit 1
fi

echo "Transferring APK -> $HOST:$REMOTE_APK ..."
# The debug APK is mostly UNCOMPRESSED dex, so wire compression helps a lot, and
# rsync's delta only ships changed blocks across rebuilds. Prefer rsync, then a
# zstd pipe, then plain scp -C — whichever both ends support.
SSH_CMD="ssh -p $PORT -o StrictHostKeyChecking=accept-new"
remote_has() { ssh "${SSH_OPTS[@]}" "$HOST" "command -v $1 >/dev/null 2>&1"; }

# zstd ratio (~26% on this raw-dex APK) beats rsync's zlib, and rsync's delta
# barely helps since the dex reshuffles each rebuild — so zstd pipe is primary.
if command -v zstd >/dev/null 2>&1 && remote_has zstd; then
  echo "  via zstd pipe (~4x smaller)"
  zstd -"${ZSTD_LEVEL:-3}" -c "$APK" | ssh "${SSH_OPTS[@]}" "$HOST" "zstd -d -f -o '$REMOTE_APK'"
elif command -v rsync >/dev/null 2>&1 && remote_has rsync; then
  echo "  via rsync -z (delta + compression)"
  rsync -z --partial --inplace -e "$SSH_CMD" "$APK" "${HOST}:${REMOTE_APK}"
else
  echo "  via scp -C (install 'zstd' in Termux for ~4x faster transfers)"
  scp -C -P "$PORT" -o StrictHostKeyChecking=accept-new "$APK" "${HOST}:${REMOTE_APK}"
fi

echo "Dropping APK into Downloads + posting a notification ..."
# Android 16 + ColorOS block Background Activity Launch: a background Termux context
# (SSH command or a notification's background tap-action) cannot pop the package
# installer, and adb (which could) gets killed by ColorOS. So install/run stay MANUAL:
# the notification is a heads-up whose tap just DISMISSES it; you install the apk from
# Files -> Downloads and open the app yourself.
ssh "${SSH_OPTS[@]}" "$HOST" "
  for d in ~/storage/downloads /sdcard/Download /storage/emulated/0/Download; do
    cp '$REMOTE_APK' \"\$d/fitbro.apk\" 2>/dev/null && { echo \"  Downloads copy: \$d/fitbro.apk\"; break; }
  done
  termux-notification --id fitbro --priority high \
    --title 'FitBro build ready' \
    --content 'Install: Files -> Downloads -> fitbro.apk' \
    --action 'termux-notification-remove fitbro' \
    2>/dev/null || true
"

echo "Done — delivered. On the phone: Files -> Downloads -> fitbro.apk -> Install, then open the app."
