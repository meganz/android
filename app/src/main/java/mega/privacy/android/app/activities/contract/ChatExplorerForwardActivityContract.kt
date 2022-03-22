package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.main.megachat.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants

@Suppress("ArrayInDataClass")
class ChatExplorerForwardActivityContract :
    ActivityResultContract<ChatExplorerForwardActivityContract.Params, ChatExplorerForwardActivityContract.Result?>() {

    data class Params(
        val messageIds: LongArray,
        val chatRoomId: Long
    )

    data class Result(
        val messageIds: LongArray?,
        val selectedChats: LongArray?,
        val selectedUsers: LongArray?
    )

    override fun createIntent(context: Context, input: Params): Intent =
        Intent(context, ChatExplorerActivity::class.java).apply {
            action = Constants.ACTION_FORWARD_MESSAGES
            putExtra(Constants.ID_CHAT_FROM, input.chatRoomId)
            putExtra(Constants.ID_MESSAGES, input.messageIds)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? =
        when (resultCode) {
            Activity.RESULT_OK ->
                Result(
                    intent?.getLongArrayExtra(Constants.ID_MESSAGES),
                    intent?.getLongArrayExtra(Constants.SELECTED_CHATS),
                    intent?.getLongArrayExtra(Constants.SELECTED_CONTACTS)
                )
            else -> null
        }
}
