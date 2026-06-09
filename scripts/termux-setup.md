# Phone setup — Termux SSH delivery (robust remote install)

Robust to ColorOS network / wireless-debugging flaps. Needs Tailscale + Termux
sshd only. Each install = APK is delivered over SSH and you tap **Install** once.

## 1. Install apps (use F-Droid builds — Play Store Termux is deprecated)
- **Termux**  — https://f-droid.org/packages/com.termux/
- **Termux:API** — https://f-droid.org/packages/com.termux.api/
- **Termux:Boot** — https://f-droid.org/packages/com.termux.boot/  (for sshd on boot)
- **Tailscale** — already installed; keep it connected.

## 2. In Termux (one-time)
```sh
pkg update && pkg upgrade -y
pkg install -y openssh termux-api rsync zstd   # rsync/zstd = faster compressed transfers

# Authorize this Mac's key (paste the Mac pubkey below):
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys <<'KEY'
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJn/hyMewsCWCLt0j6nOpunblZNMPkBRmXfbhBN2bjMD mettyoung@Emmetts-MacBook-Air.local
KEY
chmod 600 ~/.ssh/authorized_keys

# Your Termux ssh listens on port 8022. Start it:
sshd
whoami   # note the username if you want SSH_USER; usually not needed
```

## 3. Termux:API app (optional — for the heads-up notification only)
- Installing **never** happens silently here (non-root + no adb): Android requires
  one foreground tap on the system installer, and a background SSH/Termux command
  cannot pop that dialog. So the deploy drops the APK into **Downloads** and you
  install by tapping it in **Files**.
- The **Termux:API app** (`com.termux.api`, F-Droid build) is optional: it only
  enables the "FitBro build ready" **notification** (a heads-up; not the installer).
  Grant it Notification permission if you want that ping.
- Allow **Install unknown apps** for your **Files** app (Settings → search
  "install unknown" → Files → Allow) so the Downloads tap can install.

## 4. Persist sshd across reboots (Termux:Boot)
```sh
mkdir -p ~/.termux/boot
cat > ~/.termux/boot/start-sshd.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock
sshd
EOF
chmod +x ~/.termux/boot/start-sshd.sh
```
Then open the **Termux:Boot** app once so Android grants it boot permission.
Also exempt Termux from battery optimization (Settings → Battery) so ColorOS
doesn't kill sshd in the background.

## 5. Install from the Mac (any network)
```sh
scripts/remote-install-ssh.sh oppo-find-n5     # or the Tailscale IP
```
Builds the minified `remote` APK, ships it over Tailscale (zstd, ~4 MB), drops it
into Downloads, and pings a notification. On the phone: **Files → Downloads →
fitbro.apk → Install**. The script then auto-launches the app over SSH (`am start`).

For a fully **silent** install (no tap) use the adb path instead — see
`scripts/remote-install.sh` — but it needs adbd, which ColorOS keeps killing.

## Notes
- Port 8022 is Termux's default sshd port. Reachable at `<tailscale-ip>:8022`.
- First app install shows a full installer prompt; updates to the same signed
  debug build are a quicker confirm.
- No `adb tcpip` / wireless debugging needed — survives network switches.
