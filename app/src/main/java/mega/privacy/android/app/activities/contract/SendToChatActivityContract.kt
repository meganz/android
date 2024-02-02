package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants

/**
 * Launcher for send to chat
 */
class SendToChatActivityContract :
    ActivityResultContract<LongArray, Pair<LongArray?, LongArray?>?>() {
    override fun createIntent(context: Context, input: LongArray): Intent {
        val intent = Intent(context, ChatExplorerActivity::class.java)
        intent.putExtra(Constants.NODE_HANDLES, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<LongArray?, LongArray?>? {
        return intent?.let {
            if (resultCode == Activity.RESULT_OK) {
                val nodeIds = it.getLongArrayExtra(Constants.NODE_HANDLES)
                val chatIds = it.getLongArrayExtra(Constants.SELECTED_CHATS)
                Pair(nodeIds, chatIds)
            } else {
                null
            }
        }
    }
}