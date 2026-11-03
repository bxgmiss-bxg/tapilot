# Floating virtual display (Debug experiment)

The optional secondary-display probe can show its actual captured screen in a movable window over other apps. The window is not a mirrored primary screen and is not a background Agent implementation.

1. Open **Settings → Developer experiments → Secondary display probe** in a Debug build.
2. Start the experiment, then select **Float and control display**.
3. If requested, grant **Display over other apps** to this test app in Android settings. Return and select the floating-display button again. Only Debug declares this additional permission.
4. Drag the small handle at the top of the borderless preview to move it. Press Home to use the primary screen outside the floating window.
5. Tap or swipe inside the preview to request a gesture on the virtual display. Coordinates follow the preview's aspect ratio; letterbox touches, cancelled gestures, and multi-touch sequences are discarded. The window does not capture the keyboard or primary-screen focus.
6. Inspect the reported gesture result and the actual fixture tap counter. Dispatch acceptance does not prove the target received input. A system cancellation appears as a brief message; detailed status remains on the experiment page; input never falls back to the primary display.
7. Close the window with **×** to release the virtual display, capture buffers and device-control lease. **Stop and release**, activity destruction and the existing two-minute timeout also remove the window. The overlay is not restored after restart.

The preview consumes actual ImageReader frames at up to 10 frames per second, including the last frame of a static scene. Only a single in-flight gesture is allowed. No screenshots are sent to a model or written to storage by this experiment. The underlying Android restrictions still apply: the existing OPPO Android 14 and Redmi Android 12 probes rejected secondary-display gestures and Calculator launch. A floating preview does not grant input-injection or cross-app launch privileges. Text entry, system navigation and arbitrary third-party app control on the virtual display are not implemented.

See [the base experiment](background-display.md) for display isolation, launch checks and the adoption criteria.

## 中文说明

此功能把虚拟屏的真实截图显示为可拖动的悬浮窗，不是主屏镜像，也尚未接入后台 Agent。

1. 在 Debug 版本的“设置 → 开发者实验 → 后台屏实验”中开始实验，再点“悬浮显示并操作”。
2. 如系统要求，给测试应用开启“显示在其他应用上层”，返回后再次点击悬浮按钮。此权限仅在 Debug 中声明。
3. 拖动画面顶部的小横条移动窗口；按 Home 后可在窗口外继续使用主屏。
4. 点击或滑动预览，会按画面比例转换坐标，并仅向虚拟屏发送手势。黑边、多指和中断的触摸不会发送。
5. 以真实画面和测试按钮计数判断操作结果。系统拒绝时短暂提示失败，详细状态保留在实验页面，不会改为操作主屏。
6. 点击 × 会释放虚拟屏；停止实验、页面销毁或运行满两分钟也会关闭窗口并释放资源。

悬浮窗不会获得原本没有的系统输入权限。已测 OPPO 和 Redmi 仍拒绝副屏触控及计算器启动，因此目前只能确认悬浮显示能力，不能声称已支持任意应用的副屏控制。虚拟屏文字输入和系统导航尚未实现。

## Android API references

- [Overlay permission](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context))
- [Gesture target display](https://developer.android.com/reference/android/accessibilityservice/GestureDescription.Builder#setDisplayId(int))
