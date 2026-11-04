#!/usr/bin/env bash
# Four isolated devices, one native Midscene run/report; each device stays serial.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
: "${ANDROID_HOME:?Android SDK is required}"
: "${RUNNER_TEMP:?Hosted runner temp directory is required}"
[[ "${CI:-}" == true && "${TAPILOT_E2E_CI_PARALLEL:-}" == 1 ]] || { echo 'Hosted parallel CI required' >&2; exit 1; }
export ANDROID_AVD_HOME="$RUNNER_TEMP/tapilot-avd"
mkdir -p "$ANDROID_AVD_HOME" build/apk-midscene
emulator="$ANDROID_HOME/emulator/emulator"
pids=()
serials=()
cleanup() {
  for serial in "${serials[@]}"; do
    adb -s "$serial" logcat -d > "build/apk-midscene/$serial/logcat.txt" 2>&1 || true
    adb -s "$serial" exec-out screencap -p > "build/apk-midscene/$serial/final.png" 2>/dev/null || true
    adb -s "$serial" emu kill >/dev/null 2>&1 || true
  done
  for pid in "${pids[@]}"; do wait "$pid" || true; done
}
trap cleanup EXIT
# Start every device before waiting for any one device's boot or installation.
for port in 5554 5556 5558 5560; do
  serial="emulator-$port"
  if adb devices | grep -q "^$serial"; then
    echo "Refusing to reuse $serial" >&2
    exit 1
  fi
  mkdir -p "build/apk-midscene/$serial"
  printf 'no\n' | avdmanager create avd --force --name "tapilot-$port" --package 'system-images;android-35;google_apis;x86_64' --device pixel_5
  "$emulator" -avd "tapilot-$port" -port "$port" -cores 1 -memory 1536 -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > "build/apk-midscene/$serial/emulator.log" 2>&1 &
  pids+=("$!")
  serials+=("$serial")
done
prepare_device() {
  local serial="$1" pid="$2" ready=false
  for ((attempt=0; attempt<150; attempt++)); do
    if ! kill -0 "$pid" 2>/dev/null; then
      cat "build/apk-midscene/$serial/emulator.log" >&2
      return 1
    fi
    if [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
      ready=true
      break
    fi
    sleep 2
  done
  [[ "$ready" == true ]] || { echo "$serial boot timed out" >&2; return 1; }
  for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb -s "$serial" shell settings put global "$setting" 0
  done
  TAPILOT_E2E_DEVICE_ID="$serial" bash scripts/run-apk-ci-smoke.sh
}
preparations=()
for index in "${!serials[@]}"; do
  prepare_device "${serials[$index]}" "${pids[$index]}" &
  preparations+=("$!")
done
failed=0
for pid in "${preparations[@]}"; do wait "$pid" || failed=1; done
[[ "$failed" == 0 ]] || exit 1
npm --prefix tests/midscene run check:model
npm --prefix tests/midscene run test:resilience
