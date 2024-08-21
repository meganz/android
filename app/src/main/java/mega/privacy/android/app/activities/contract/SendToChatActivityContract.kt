package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.main.model.SendToChatResult
import mega.privacy.android.app.utils.Constants

/**
 * Launcher for send to chat
 */
class SendToChatActivityContract :
    ActivityResultContract<LongArray, SendToChatResult?>() {
    override fun createIntent(context: Context, input: LongArray): Intent {
        val intent = Intent(context, ChatExplorerActivity::class.java)
        if (input.isNotEmpty()) {
            intent.putExtra(Constants.NODE_HANDLES, input)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SendToChatResult? {
        return intent?.let {
            if (resultCode == Activity.RESULT_OK) {
                val nodeIds = it.getLongArrayExtra(Constants.NODE_HANDLES)
                val chatIds = it.getLongArrayExtra(Constants.SELECTED_CHATS)
                val userHandles = it.getLongArrayExtra(Constants.SELECTED_USERS)
                SendToChatResult(
                    nodeIds ?: longArrayOf(),
                    chatIds ?: longArrayOf(),
                    userHandles ?: longArrayOf()
                )
            } else {
                null
            }
        }
    }
}