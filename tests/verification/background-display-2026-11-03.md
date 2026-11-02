# 后台屏实验真机记录

验证日期：2026-11-03。设备：Redmi K30 Ultra（M2006J10C），Android 12。
安装本次独立 Debug 测试包 `lifeos.md.tapilot.e2everify`，没有覆盖日常 Tapilot。
测试通过设备锁串行执行。安装遇到 MIUI 的 USB 安装确认，按正常界面点击“继续安装”。

本次结果：**独立渲染和读取画面可用，尚不能用这条路线操作后台应用。**

| 检查 | 实际结果 |
| --- | --- |
| 默认关闭 | 打开实验页时显示 Off，frames=0、taps=0 |
| 创建与截图 | 创建 display 2，预览显示测试页面，frames 从 1 增至 6，主屏保留实验页 |
| 副屏点击 | 测试包无障碍服务开启后，手势回调为 cancelled，taps 仍为 0 |
| 计算器启动 | 选择系统计算器，启动许可检查拒绝，显示 System disallows this app on the experiment display；没有降级到主屏启动 |
| 两分钟超时 | 显示 Experiment timed out; display released，预览清空；dumpsys display 中实验 DisplayDeviceInfo 条目为 0 |
| 再次开始 | 可重新创建副屏，实验 DisplayDeviceInfo 条目为 1，480 × 800，FLAG_PRIVATE、FLAG_OWN_CONTENT_ONLY，系统报告 touch NONE |
| 主动停止 | 点击停止并释放后，实验 DisplayDeviceInfo 条目回到 0 |

画面证据：

- [副屏渲染](background-display-2026-11-03/rendered.png)
- [手势取消](background-display-2026-11-03/gesture-cancelled.png)
- [应用启动被拒绝](background-display-2026-11-03/launch-denied.png)
- [超时释放](background-display-2026-11-03/timeout-released.png)

截图只包含实验页、无业务内容的测试图形和系统安装应用的状态栏图标。
没有调用模型，没有发布消息或操作业务数据。测试结束后关闭实验，并移除本次添加的测试包无障碍服务，保留原 Tapilot 服务。

本地验证：396 个 Kotlin 单元测试通过，lintDebug 与隔离 Debug APK 构建通过；
运行时缓存按仓库流程重新生成，并通过源码指纹和归档完整性检查。

未验证：Android 14+、其他厂商设备、主屏持续输入与成功副屏操作并行、第三方应用内容、
后台保活和接管恢复。由于本机副屏点击失败，不能声称并行输入已经通过。
下一步需在 Android 14+ 复测，并独立评估是否值得引入需要额外授权的设备执行后端。
