---
name: deploy-android
description: Build and deploy the FitBro Android APK to the remote phone (oppo-find-n5) over Tailscale SSH. Use when the user says "deploy", "redeploy", "install on phone", or "try again" after a deploy failure.
---

# Remote Android Deploy — FitBro

Deploy FitBro to `oppo-find-n5` via `scripts/remote-install-ssh.sh`.

## Steps

1. Run the deploy script (UNSANDBOXED — needs network + gradle):
   ```bash
   scripts/remote-install-ssh.sh oppo-find-n5 2>&1 | tail -8
   ```

2. **If it succeeds** (`Done — delivered`): tell the user to install from Files → Downloads → fitbro.apk.

3. **If SSH fails** (`ERROR: cannot SSH`):
   - Check Tailscale reachability: `tailscale ping oppo-find-n5`
   - If ping times out: phone Tailscale is off — ask user to enable it and try again.
   - If ping succeeds but SSH fails: Termux sshd is down — ask user to open Termux and run `sshd`.
   - After user confirms, retry step 1.

4. **If build fails**: show the Gradle error and stop — do not retry the deploy.

## Phone details

- Tailscale name: `oppo-find-n5` (IP: `100.98.233.108`)
- SSH port: `8022`
- App ID: `com.mettyoung.fitbro`
- Transfer: zstd-compressed over Tailscale (≈4 MB for the minified APK)

## Why install is manual

Android 16 + ColorOS blocks Background Activity Launch. SSH commands run in a background Termux context and cannot pop the package installer or launch the app. The script drops the APK into Downloads and posts a notification — the user taps Install themselves.
