package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity

internal fun startWaitingRoom(context: Context, chatId: Long) {
    context.startActivity(
        Intent(
            context,
            WaitingRoomActivity::class.java
        ).apply {
            putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatId)
        },
    )
}