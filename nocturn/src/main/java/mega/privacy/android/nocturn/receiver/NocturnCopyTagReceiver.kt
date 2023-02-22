package mega.privacy.android.nocturn.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

internal class NocturnCopyTagReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tag = intent.extras?.getString("tag").orEmpty()

        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("text", tag))

        Toast.makeText(context, "Tag copied", Toast.LENGTH_SHORT).show()
    }
}
