package lifeos.md.tapilot.shell

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/** UI owns its transport; consent remains owned by ShellSession and is checked again at handoff. */
internal class LocalAdbSetup(private val host: Activity, private val zh: Boolean) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val cleanup = Executors.newSingleThreadExecutor()
    private var dialog: AlertDialog? = null
    private var label: TextView? = null
    private var buttons = listOf<Button>()
    @Volatile private var client: LocalAdbClient? = null
    @Volatile private var generation = 0
    private var busy = false
    @Volatile private var closed = false
    private fun text(cn: String, en: String) = if (zh) cn else en
    private fun update(value: String) { label?.text = value; buttons.forEach { it.isEnabled = !busy } }

    fun show() {
        check(!closed)
        if (dialog?.isShowing == true) return
        val layout = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 12, 32, 12) }
        layout.addView(TextView(host).apply {
            text = text("在系统开发者选项中开启无线调试。首次配对请将设置与 Tapilot 分屏显示，保持“使用配对码配对设备”弹窗打开。只需输入端口，连接仅限本机。配对端口与连接端口不同。",
                "Enable Wireless debugging in Developer options. For first pairing, use split screen with Settings and Tapilot and keep ‘Pair device with pairing code’ open. Enter ports only; connections stay on this phone. Pairing and connection ports are different.")
        })
        layout.addView(Button(host).apply {
            text = text("打开开发者选项", "Open Developer options")
            setOnClickListener { runCatching { host.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                .onFailure { update(text("请手动打开系统设置中的开发者选项", "Open Developer options in system Settings")) } }
        })
        fun field(cn: String, en: String, secret: Boolean = false) = EditText(host).apply {
            hint = text(cn, en); inputType = InputType.TYPE_CLASS_NUMBER or if (secret) InputType.TYPE_NUMBER_VARIATION_PASSWORD else 0
            isSaveEnabled = false; importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            layout.addView(this)
        }
        val pairPort = field("配对弹窗中的端口", "Port in pairing dialog")
        val code = field("六位配对码", "Six-digit pairing code", true)
        val pair = Button(host).apply { text = text("配对此手机", "Pair this phone") }; layout.addView(pair)
        val connectPort = field("无线调试主页中的连接端口", "Connection port on Wireless debugging page")
        val start = Button(host).apply { text = text("连接并启动已授权的辅助", "Connect and start authorized helper") }; layout.addView(start)
        val forget = Button(host).apply { text = text("忘记本机配对密钥", "Forget local pairing key") }; layout.addView(forget)
        label = TextView(host).also { layout.addView(it) }
        buttons = listOf(pair, start, forget)
        pair.setOnClickListener {
            val values = runCatching { LocalAdbInput.port(pairPort.text.toString()) to LocalAdbInput.code(code.text.toString()) }
            code.text.clear()
            values.onSuccess { (port, secret) -> runOperation(false) { connection -> connection.pair(port, secret) } }
                .onFailure { update(text("请填写有效端口和六位配对码", "Enter a valid port and six-digit pairing code")) }
        }
        start.setOnClickListener {
            val values = runCatching { LocalAdbInput.port(connectPort.text.toString()) to ShellSession.pendingToken() }
            values.onSuccess { (port, token) -> runOperation(true) { connection -> connection.start(host.applicationContext, port, token) } }
                .onFailure { update(text("请先在实验页授权，再填写有效连接端口。授权请求五分钟有效。", "Authorize in the experiment first, then enter a valid connection port. Consent requests expire after five minutes.")) }
        }
        forget.setOnClickListener {
            ShellSession.revoke(); disconnect()
            LocalAdbIdentity.forget(host)
            update(text("本机密钥已删除。请在系统无线调试中忘记 Tapilot，再重新配对。", "Local key deleted. Forget Tapilot in Wireless debugging before pairing again."))
        }
        dialog = AlertDialog.Builder(host).setTitle(text("手机内连接 · 无需电脑", "Connect on this phone · no computer"))
            .setView(ScrollView(host).apply { addView(layout) })
            .setNegativeButton(text("关闭面板", "Close panel"), null).create().also { it.show() }
        update(if (busy) text("正在连接…", "Connecting…") else text("已配对过可直接填写连接端口。配对码不会保存。", "Already paired? Enter the connection port directly. Pairing codes are never saved."))
    }

    private fun runOperation(starting: Boolean, action: (LocalAdbClient) -> Unit) {
        if (busy || closed || ShellSession.ready) return
        busy = true
        val run = ++generation
        update(text("正在连接，请保持系统配对弹窗打开…", "Connecting; keep the system pairing dialog open…"))
        worker.execute {
            var connection: LocalAdbClient? = null
            val result = runCatching {
                connection = LocalAdbClient(host.applicationContext)
                if (closed || run != generation) { connection!!.close(); return@execute }
                client = connection
                action(connection!!)
            }
            connection?.close()
            main.post {
                if (closed || run != generation) return@post
                client = null; busy = false
                if (starting) {
                    ShellSession.revoke()
                    update(text("辅助连接已结束。检查无线调试和连接端口，然后重新授权。", "Helper connection ended. Check Wireless debugging and its connection port, then authorize again."))
                } else update(if (result.isSuccess)
                    text("配对成功。请填写无线调试主页的连接端口，然后启动辅助。", "Paired. Enter the connection port from the Wireless debugging page, then start the helper.")
                    else text("配对失败。保持弹窗打开，核对配对端口和当前配对码后重试。", "Pairing failed. Keep the pairing dialog open and retry with its current port and code."))
            }
        }
    }
    fun onSessionChanged() {
        if (ShellSession.ready) {
            dialog?.dismiss()
        } else if (client != null && busy) disconnect()
    }
    private fun disconnect() {
        generation++; busy = false
        val previous = client; client = null
        if (previous != null) cleanup.execute { previous.close() }
    }
    override fun close() {
        closed = true; dialog?.dismiss(); dialog = null; label = null
        disconnect(); worker.shutdownNow(); cleanup.shutdown()
    }
}
