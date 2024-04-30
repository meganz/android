package mega.privacy.android.app.presentation.meeting.managechathistory.navigation

import androidx.lifecycle.SavedStateHandle
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

internal class ManageChatHistoryArgs(val chatId: Long, val email: String?) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        chatId = savedStateHandle.get<Long>(Constants.CHAT_ID) ?: MEGACHAT_INVALID_HANDLE,
        email = savedStateHandle[Constants.EMAIL]
    )
}
