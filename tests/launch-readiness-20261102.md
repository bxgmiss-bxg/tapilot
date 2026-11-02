# 宣发前修复记录

2026-11-02，基线 `13f1c63`。本轮只修改、构建和验证，未提交、推送、公开仓库或发布安装包。

## 已修复

- 修复本机 pnpm 12.4.1 安装，给独立 runtime 固定相同包管理器版本。修正旧补丁的 hunk 行数，并显式配置安装脚本权限。
- 将 glob、minimatch、follow-redirects 更新到通过审计的版本。审计命令直接使用 npm 官方 registry，避免镜像缺失审计接口；没有修改系统代理、DNS 或持久 registry。
- 对七条改写历史中的扫描命中逐行复核，确认与先前审查的飞书文件标识一致。新旧两套可达历史共保留十四条精确豁免，没有添加通配豁免。
- 将所有当前 Git 跟踪文件中的旧组织链接改为 `tapilot-ai/tapilot`，包括插件安装、项目元数据和安全反馈模板。
- 配对接收端只绑定二维码公布的地址。修改前有三个配对测试超时，修改后完整插件测试通过。
- Termux 仓库已下架固定的 Node 包。更新到 Node 24.22.2、SQLite 3.54.1-1，并同步 SQLite notice 映射。包哈希与官方 Packages 索引一致，提取和原生 notice 收集通过。
- 本地候选版本为 0.1.8，versionCode 为 9；线上下载仍是 0.1.7。

## 验证证据

日志位于忽略目录 `artifacts/launch-fixes-20261102`，敏感设备截图不随源码提交。

| 检查 | 结果 | 日志 |
| --- | --- | --- |
| 根目录、runtime、桌面插件、E2E 依赖审计 | 四处均零条已知漏洞 | checked.log |
| Gitleaks 8.30.1 历史扫描 | 485 个提交，无未豁免命中 | gitleaks-final.log |
| 桌面插件 | 118 项通过 | plugin-final.log |
| Node runtime | 130 项通过，另含实际 SDK 冒烟检查 | build.log |
| E2E 定义 | 18 项通过（最终用例再次检查） | e2e-definition-final.log |
| 脚本 | 34 项通过，主题检查通过 | scripts-check.log |
| 原生构建、Kotlin、lint | APK 构建及完整性检查通过；395 项通过、1 项跳过；lint 无错误、52 条警告 | native-final.log |
| 真机旧版对话 | 失败，ChatGPT HTTP 403，未自动重放 | conversation-old.log |
| 真机 0.1.8 多轮对话 | 1/1 通过，独立截图确认先追问、后回答 100 | conversation-settled.log |
| 真机 0.1.8 设置读取与回放 | LONG-02 1/1 通过，三个事实分别核对回放 | settings-qwen37.log |

首次原生构建遇到缺失的增量启动图标产物，完整重建后资源编译通过。随后因更新 notice 映射导致 runtime 指纹失效，按要求重新生成 bundle，没有绕过完整性检查。

T5 曾仅剩约 830 MB。仅清理本轮下载的 NDK 归档、未完成的分发材料副本和 runtime 暂存目录，均可重新生成；保留源码、安装包、宣传素材、截图及日志。未清理其他会话目录。

Kotlin 网络测试的三个失败已修复：本地测试服务显式绑定 IPv4 回环地址，测试客户端禁用代理，避免宿主机网络配置干扰；未修改生产代理策略或系统网络。

候选 APK：`artifacts/launch-fixes-20261102/Tapilot-0.1.8-beta-arm64-candidate.apk`，SHA-256 为 `481420148da1e50d5ef742d2991190fca94e53b96031eac9b2ddbbc1b53dbc4e`。签名与兼容 beta 的预期证书一致，非 debuggable。

E2E 曾误将旧失败会话当作新任务，后续观察器又在菜单过渡帧上反复点击。保留这两次失败报告，确认没有新任务执行后，增加空会话断言及动作后的 1000 ms 稳定等待。最终对话用例通过；该等待只影响外部验收脚本，不影响手机 Agent 或性能基准。

此前 LONG-02 手机设置用例未通过：外部观察器被交互式设备锁的 300 秒空闲计时终止，手机任务仍继续运行。确认实际状态后，在 Tapilot 中正常停止本次任务，记录耗时 12 分 27 秒、五次已完成操作；未把局部截图当成最终报告。后续长测试改用命令模式设备锁，并增加新会话断言。命令模式示例：`node ../studio/scripts/with-device-lock.mjs --platform android --reason tapilot-acceptance -- node <test-runner>`；不要用默认 300 秒空闲超时的交互会话包裹长测试。失败日志为 `settings-evidence.log`，停止证据为 `settings-test-stopped.png`。

手机 custom 配置已保留原 API Key，将模型从 qwen3.5-plus / qwen3.5 更新为 qwen3.7-plus / qwen3，界面明确显示连接成功，截图为 `qwen37-connected.png`。连接成功与实际任务验收分别记录。

新配置下 LONG-02 首次运行通过，未重试：Android 12、已用 99.3 GB / 总计 128 GB、自动亮度开启，三项均由外部观察器打开对应执行回放核对。独立最终截图 `qwen37-passed.png` 确认任务完成、3 次操作、界面耗时 6 分钟。`task.waitForIdle` 等待 358037 ms；完整用例 612291 ms，包含外部操作和回放验收，不能把它当作纯手机执行耗时。结果 ID：`20261102142159-6a155fd5`。这一个样本不能证明总体成功率达到 95%。

## 下载与发布准备

飞书固定下载地址实际返回 `Tapilot-0.1.7-beta-arm64.apk`，SHA-256 为 `2f92fd7a18560cd9f3c3208393a7b785c54bd8f43a3f212f6333ffa7397cbe22`，与官网清单一致。本轮使用已有浏览器登录态，未完成匿名下载验收。

Product Hunt 已核验当前账号与新产品填写入口可用，没有创建 Tapilot 草稿。GitHub About 已有描述与官网地址，仓库仍为 PRIVATE。

## 尚未关闭的检查项

- 真机 ChatGPT HTTP 403 的原因尚未确认，不能判断为欠费。保留账号配置，已保存的百炼订阅 API 模式返回 HTTP 429，也未通过连接检查。
- 0.1.8 已保留数据覆盖安装到 Redmi 真机。自定义千问配置连接成功，多轮对话、手机设置操作及执行证据验收通过。自动化工具报告点击完成不等于连接或任务成功。
- `@img/sharp-wasm32@0.35.5` 对应源码与重链流程、Termux 对应源码材料，以及 `proxy-agent-negotiate@1.1.0` 完整许可仍需审核。详见 `third-party/README.md`。没有把材料下载成功等同于再分发批准。
- 仓库公开、安装包上线、匿名入口验收及正式宣发仍保持未执行。宣传视频与最终发布候选的逐项行为比对尚未完成。

## 提交前同步验证

用户随后授权提交推送。同步远端 `b5b4072` 的配对与网络恢复改动；两份插件生成页面发生冲突，按合并后的源码重新构建解决，没有丢弃远端功能。合并后的插件测试 147 项通过，日志为 `artifacts/launch-fixes-20261102/push-merge-verification.log`。此前 APK 真机结果对应同步前候选，本次没有重新发布安装包。
