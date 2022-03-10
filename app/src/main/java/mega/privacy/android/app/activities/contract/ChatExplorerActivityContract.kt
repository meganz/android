package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.megachat.ChatExplorerActivity

class ChatExplorerActivityContract : ActivityResultContract<Unit?, Intent?>() {

    override fun createIntent(context: Context, unit: Unit?): Intent =
        Intent(context, ChatExplorerActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Intent? =
        when (resultCode) {
            Activity.RESULT_OK -> intent
            else -> null
        }
}
