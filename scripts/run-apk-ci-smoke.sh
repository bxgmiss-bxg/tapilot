#!/usr/bin/env bash
set -euo pipefail
: "${TAPILOT_E2E_DEVICE_ID:?Select the CI emulator explicitly}"
serial="$TAPILOT_E2E_DEVICE_ID"
apk=app/build/outputs/apk/debug/app-debug.apk
evidence="build/apk-midscene/$serial"
mkdir -p "$evidence"
adb -s "$serial" get-state
adb -s "$serial" shell getprop ro.product.cpu.abilist | tee "$evidence"/abis.txt
adb -s "$serial" install -r "$apk" | tee "$evidence"/install.txt
# Provision only notifications on this disposable verification app, before capture.
adb -s "$serial" shell pm grant lifeos.md.tapilot.e2everify android.permission.POST_NOTIFICATIONS
sha256sum "$apk" > "$evidence"/SHA256SUMS
git rev-parse HEAD > "$evidence"/source.txt
adb -s "$serial" shell dumpsys package lifeos.md.tapilot.e2everify > "$evidence"/package.txt
adb -s "$serial" shell svc power stayon true
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
adb -s "$serial" shell wm dismiss-keyguard
node scripts/setup-apk-ci-model.mjs
