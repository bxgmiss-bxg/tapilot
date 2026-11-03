# Optional shell-assisted virtual display (Experimental)

Virtual displays now require **both explicit in-app authorization and a live shell helper**. Accessibility permission alone cannot enable them. This is Debug-only, off by default, and does not change the normal accessibility-based Agent path or enable production background tasks.

## Connect

1. Install the Debug verification build on an Android 12+ device with an already-authorized ADB connection. No root is required; the helper rejects root and application UIDs.
2. Open **Settings → Developer experiments → Secondary display probe**.
3. Select **Authorize shell helper** and accept the explanation. This creates a single-use request that expires in five minutes. The Start button stays disabled until connection succeeds.
4. From this checkout run `python3 scripts/start-shell-helper.py --serial <device> --package lifeos.md.tapilot.e2everify`. For a different Debug application ID, pass that exact package. Keep the terminal open. The script uses the selected ADB connection; it does not change Wi-Fi, enable debugging or pair a device automatically.
5. Confirm **Shell helper authorized and connected**, then start the display. Enable Android's overlay permission separately to float its captured screen.
6. Select **Revoke shell authorization** to release the display and stop the helper. Closing the experiment activity also revokes authorization. A process restart requires fresh authorization and activation.

The initial activation path uses a computer and ADB (USB or an already-paired wireless connection). An in-app wireless-debugging pairing wizard is not included. The private consent file is readable by the starter only through Debug `run-as`; this is not a release-build activation protocol.

## Capability boundary

The shell process exposes only display creation, bounded touch paths, explicit component launch, display release and shutdown. All input and launches require the current nonzero owned display ID. No arbitrary command endpoint or primary-display fallback is exposed. Input coordinates and duration are validated. Display creation uses Android display flags for touch and, on supported versions, trusted display and independent focus; unsupported permissions fail closed.

The exported handoff provider requires Android's DUMP permission, verifies shell UID 2000, and consumes a one-use authorization token. The helper checks the calling application UID on every Binder request and watches client death. The application watches helper death and closes its preview. Display leases expire after two minutes on both sides; the helper process has a ten-minute maximum lifetime. Authorization is never persisted as an always-enabled preference. Consent values are passed through standard input rather than command-line arguments or logs.

This is an independently implemented experiment using Android APIs. It does not include Mika source code or copy. Android behavior still needs verification per device. Never treat a returned launch or injection result as proof that the requested app action happened; inspect the actual captured screen.

## Verification

Android 15 emulator verification passed for the consent gate, floating fixture taps, Clock launch and stopwatch control, primary focus, revocation and helper-death cleanup. See the [steps and screenshots](../../tests/verification/shell-display-2026-11-03.md). This does not establish OEM compatibility or production background Agent support.

## 中文说明

虚拟屏必须同时满足“应用内明确授权”和“Shell 辅助进程已连接”，仅开启无障碍权限不能使用。功能仅在 Debug 实验中提供，默认关闭，不改变普通 Agent 的无障碍执行方式。

先在实验页授权，再通过已授权的 ADB 连接运行启动脚本。首版需要电脑，不包含手机内无线调试配对向导，也不会自动开启无线调试。撤销授权、退出实验或连接断开会释放虚拟屏；进程重启后需要重新授权。虚拟屏每次最多运行两分钟。

## API references

- [DisplayManager and display flags](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/hardware/display/DisplayManager.java)
- [InputManager input injection](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/hardware/input/InputManager.java)
- [Binder caller identity](https://developer.android.com/reference/android/os/Binder#getCallingUid())
