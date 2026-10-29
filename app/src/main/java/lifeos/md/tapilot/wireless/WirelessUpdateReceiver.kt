package lifeos.md.tapilot.wireless

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Best-effort restoration after an in-place update; never enables a stopped service. */
class WirelessUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            android.util.Log.i("TapilotWireless", "Received package replacement")
            WirelessControlService.restoreIfEnabled(context)
        }
    }
}
