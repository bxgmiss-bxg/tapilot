#!/usr/bin/env bash
# Own a disposable emulator for the Midscene action command, including cleanup.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
: "${ANDROID_HOME:?Android SDK is required}"
export TAPILOT_E2E_DEVICE_ID=emulator-5554
# Keep avdmanager and emulator on the same path across SDK tool versions.
export ANDROID_AVD_HOME="${RUNNER_TEMP:?Hosted runner temp directory is required}/tapilot-avd"
mkdir -p "$ANDROID_AVD_HOME"
mkdir -p build/apk-midscene
emulator="$ANDROID_HOME/emulator/emulator"
if adb devices | grep -q '^emulator-5554'; then
  echo 'Refusing to reuse an existing emulator' >&2
  exit 1
fi
printf 'no\n' | avdmanager create avd --force --name tapilot-ci --package 'system-images;android-35;google_apis;x86_64' --device pixel_5
"$emulator" -avd tapilot-ci -port 5554 -partition-size 2048 -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > build/apk-midscene/emulator.log 2>&1 &
emulator_pid=$!
cleanup() {
  adb -s "$TAPILOT_E2E_DEVICE_ID" emu kill >/dev/null 2>&1 || true
  wait "$emulator_pid" || true
}
trap cleanup EXIT
ready=false
for ((attempt=0; attempt<150; attempt++)); do
  if ! kill -0 "$emulator_pid" 2>/dev/null; then
    cat build/apk-midscene/emulator.log >&2
    exit 1
  fi
  if [[ "$(adb -s "$TAPILOT_E2E_DEVICE_ID" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
    ready=true
    break
  fi
  sleep 2
done
[[ "$ready" == true ]] || { echo 'Emulator boot timed out' >&2; exit 1; }
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -s "$TAPILOT_E2E_DEVICE_ID" shell settings put global "$setting" 0
done
# Edge swipes in DocumentsUI must open its drawer, not trigger system Back.
# This changes only the disposable emulator, never a developer's connected phone.
adb -s "$TAPILOT_E2E_DEVICE_ID" shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
adb -s "$TAPILOT_E2E_DEVICE_ID" shell cmd overlay list > build/apk-midscene/overlays.txt
grep -F '[x] com.android.internal.systemui.navbar.threebutton' build/apk-midscene/overlays.txt
bash scripts/run-apk-ci-smoke.sh
