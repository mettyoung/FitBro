#!/usr/bin/env bash
#
# device-bootstrap.sh — one-time (per phone reboot) setup so adbd listens on a
# FIXED TCP port on ALL interfaces, including the Tailscale interface. This is
# what makes remote `adb connect <tailscale-ip>:5555` possible — Android's normal
# "wireless debugging" pairing is mDNS + random-port and only works on the LAN.
#
# Run this with the phone reachable LOCALLY first:
#   - over USB, or
#   - over same-LAN wireless debugging (already paired).
#
# Usage:
#   scripts/device-bootstrap.sh [serial]
#
# After this, adbd listens on 0.0.0.0:${PORT} until the phone reboots.
set -euo pipefail

ADB="${ADB:-adb}"
PORT="${FITBRO_ADB_PORT:-5555}"

SERIAL="${1:-$("$ADB" devices | grep -v 'List of' | grep -v emulator | awk 'NF{print $1; exit}')}"
if [[ -z "${SERIAL:-}" ]]; then
  echo "No local device found. Connect the phone via USB or LAN wireless debugging, then retry." >&2
  exit 1
fi

echo "Bootstrapping '$SERIAL' -> adb tcpip $PORT ..."
"$ADB" -s "$SERIAL" tcpip "$PORT"
sleep 1

echo
echo "Done. adbd now listens on 0.0.0.0:$PORT (until the phone reboots)."
echo "Next: find the phone's Tailscale IP and run scripts/remote-install.sh <ip>."
echo "  - On the phone: Tailscale app shows its 100.x.y.z address, or check the admin console."
echo "  - From the Mac (if signed into the same tailnet): tailscale status"
