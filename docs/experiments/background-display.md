# 后台屏技术验证

本实验检查当前无 ADB 运行路线下，普通 Android 应用能否创建、读取并操作独立显示区域。
实现仅使用 Android 公共 API，独立编写，没有引入其他手机 Agent 的代码或文案。
它是默认关闭的调试工具，尚未接入 Tapilot 任务执行器，也不代表已经支持后台自动操作任意应用。

## 打开实验

构建 Debug 版本，从“设置 → 开发实验 → 后台屏技术验证”进入，再点击“开始实验”。
Beta 和 Release 不包含实验 Activity。打开实验页本身不会创建副屏，每次运行需主动开始。
建议使用独立的 `-Pe2eVerification` 测试包，避免覆盖日常安装。

实验以 480 × 800 像素创建私有 VirtualDisplay，使用 OWN_CONTENT_ONLY 防止镜像主屏。
ImageReader 读取副屏实际帧，Presentation 提供无业务副作用的测试按钮。
点击测试按钮通过无障碍服务的指定 displayId 手势执行，界面上的 taps 只记录按钮实际收到的点击。
截图保留在内存中，不发给模型，不自动导出。悬浮预览需单独授权“显示在其他应用上层”，仅 Debug 声明该权限；不会修改系统网络配置。

## 验证方法

- 开始实验，确认预览显示测试页面，frames 大于零，displayId 不为 0。
- 启用测试包自己的设备控制，点击“点击副屏测试按钮”，检查 taps 是否增加。
  手势被系统接受不等于点击成功，必须以实际计数和画面为准。
- 在主屏输入框输入文字，观察副屏操作是否影响主屏焦点、键盘和已有文字。
- 选择无敏感数据的测试应用，检查它是否出现在副屏预览，并检查主屏是否被抢占。
  启动前检查系统多屏能力与启动许可；拒绝时不改到主屏执行。
  即使检查通过，应用的启动模式或厂商系统仍可能使行为不同，必须实测。
- 点击“停止并释放”，确认副屏消失。再次开始应创建新的会话，旧异步回调不能更新新会话。
  页面销毁或运行满两分钟也会释放副屏和设备控制锁。

实验运行时占用应用内设备控制锁，不能与本包的正式任务或无线控制同时运行。
该锁无法控制其他应用的无障碍服务，测试期间应避免其他自动化。
测试不包含后台保活、熄屏执行、受保护页面、第三方输入法和支付接管。

## 已有验证结果

[2026-11-03 真机记录](../../tests/verification/background-display-2026-11-03.md)：
Redmi K30 Ultra、Android 12 上可独立渲染和截图，副屏手势被取消，系统计算器启动许可检查拒绝。
超时和主动停止均释放了副屏，随后可再次创建。此结果不支持把实验直接接入正式 Agent。

## 采用条件

独立渲染成功只证明可以拥有副屏。要接入 Agent，还必须验证目标应用兼容性、截图来源、
文字输入和系统导航如何限定到副屏，以及接管、取消、进程中断时的行为。
在这些条件完成前，正式任务继续使用已有主屏执行方式。

## 依据

- [Android 多屏启动规则](https://source.android.com/docs/core/display/multi_display/activity-launch)
- [ActivityOptions.setLaunchDisplayId](https://developer.android.com/reference/android/app/ActivityOptions#setLaunchDisplayId(int))
- [GestureDescription.Builder.setDisplayId](https://developer.android.com/reference/android/accessibilityservice/GestureDescription.Builder#setDisplayId(int))

## 悬浮显示与操作

副屏支持可拖动的悬浮预览，点击和滑动只向副屏发送，不回退主屏。
开启方法、权限与限制见[悬浮虚拟屏说明](floating-virtual-display.md)。
