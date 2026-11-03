# Optional shell-assisted virtual display (Experimental)

Virtual displays now require **both explicit in-app authorization and a live shell helper**. Accessibility permission alone cannot enable them. This is Debug-only, off by default, and does not change the normal accessibility-based Agent path or enable production background tasks.

## Connect on the phone (no computer)

Android 12+ is required by the experimental helper. Keep the phone connected to Wi-Fi. This feature uses Android Wireless debugging; it is separate from Tapilot's computer-to-phone wireless-control server.

1. Open **Settings → Developer experiments → Secondary display probe** in the Debug app.
2. Select **Pair and connect on this phone**, then **Open Developer options**. Enable **Wireless debugging** in Android settings.
3. For first pairing, put Settings and Tapilot in split screen. In Wireless debugging, choose **Pair device with pairing code** and keep that dialog open. Enter its **pairing port** and **six-digit code** in Tapilot, then select **Pair this phone**. The host is fixed to this phone's loopback address; do not enter an IP address.
4. After pairing succeeds, note the **connection port** on the main Wireless debugging page. It is different from the pairing port and can change after restarting Wireless debugging.
5. In the experiment select **Authorize shell helper** and accept the explanation. Reopen **Pair and connect on this phone**, enter the connection port and select **Connect and start authorized helper** within five minutes.
6. Confirm **Shell helper authorized and connected**, then start the virtual display. Enable overlay permission separately for the floating preview.
7. Select **Revoke shell authorization** to release the display and helper. Closing the experiment activity also revokes authorization. An app restart requires fresh authorization and connection, but pairing normally does not need repeating.

Pairing codes are not saved. The ADB identity is stored only in private app storage excluded from backup. **Forget local pairing key** deletes that identity and revokes the helper; also forget **Tapilot local helper** in Android's paired-device list. Pairing alone never authorizes display creation. No wireless debugging, Wi-Fi or developer setting is enabled automatically.

The initial UI uses split screen to keep Android's short-lived pairing dialog alive. If the device does not support split screen or cancels pairing when focus changes, this flow needs device-specific validation; there is no claim of universal OEM support.

## Optional computer fallback

An already-authorized computer ADB connection is still supported. After in-app consent, run `python3 scripts/start-shell-helper.py --serial <device> --package lifeos.md.tapilot.e2everify`. The script uses Debug `run-as`, does not pair automatically and is not a release-build activation protocol. The phone-only path starts the same restricted helper directly through local ADB and does not require that script.

## Capability boundary

The shell process exposes only display creation, bounded touch paths, explicit component launch, display release and shutdown. All input and launches require the current nonzero owned display ID. No arbitrary command endpoint or primary-display fallback is exposed. Input coordinates and duration are validated. Display creation uses Android display flags for touch and, on supported versions, trusted display and independent focus; unsupported permissions fail closed.

The exported handoff provider requires Android's DUMP permission, verifies shell UID 2000, and consumes a one-use authorization token. The helper checks the calling application UID on every Binder request and watches client death. The application watches helper death and closes its preview. Display leases expire after two minutes on both sides; the helper process has a ten-minute maximum lifetime. Authorization is never persisted as an always-enabled preference. Consent values are passed through standard input rather than command-line arguments or logs.

This is an independently implemented experiment using Android APIs. It does not include Mika source code or copy. Android behavior still needs verification per device. Never treat a returned launch or injection result as proof that the requested app action happened; inspect the actual captured screen.

## Verification

The [phone-only verification record](../../tests/verification/local-adb-2026-11-03.md) covers in-app pairing, local helper startup, reuse of pairing, consent rejection, floating input, revocation, wireless disconnection and key deletion on Android 15.

Android 15 emulator verification passed for the consent gate, floating fixture taps, Clock launch and stopwatch control, primary focus, revocation and helper-death cleanup. See the [steps and screenshots](../../tests/verification/shell-display-2026-11-03.md). This does not establish OEM compatibility or production background Agent support.

## 中文说明

虚拟屏必须同时满足“应用内明确授权”和“Shell 辅助进程已连接”，仅开启无障碍权限不能使用。功能仅在 Debug 实验中提供，默认关闭，不改变普通 Agent 的无障碍执行方式。

可直接在手机内配对和连接，无需电脑：在系统中开启无线调试，将设置与 Tapilot 分屏，保持配对码弹窗打开，在 Tapilot 输入配对端口和六位配对码。配对后回到实验页授权，再填写无线调试主页的连接端口并启动辅助。两个端口不同，配对码不会保存，也不会自动开启无线调试。撤销授权、退出实验或连接断开会释放虚拟屏；进程重启后需要重新授权。虚拟屏每次最多运行两分钟。

The Debug client uses [LibADB Android BC 3.5.1](https://github.com/osservatorionessuno/libadb-android-bc/tree/3.5.1) and Bouncy Castle. A local-only relay bounds pairing to 30 seconds, including stalled TLS handshakes. Dependency notices are included in the APK.

## API references

- [DisplayManager and display flags](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/hardware/display/DisplayManager.java)
- [InputManager input injection](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/android/hardware/input/InputManager.java)
- [Binder caller identity](https://developer.android.com/reference/android/os/Binder#getCallingUid())
