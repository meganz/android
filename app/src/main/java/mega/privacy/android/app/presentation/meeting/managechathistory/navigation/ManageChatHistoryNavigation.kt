package mega.privacy.android.app.presentation.meeting.managechathistory.navigation

import androidx.lifecycle.SavedStateHandle
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

internal class ManageChatHistoryArgs(val chatId: Long, val email: String?) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        chatId = savedStateHandle.get<Long>(manageChatHistoryChatIdArg) ?: MEGACHAT_INVALID_HANDLE,
        email = savedStateHandle[manageChatHistoryEmailIdArg]
    )
}

// Currently the value is the same as [Constants.CHAT_ID] to get the intent.extras
// Can be updated later after our codebase is fully migrated to Compose Navigation to make it more unique
internal const val manageChatHistoryChatIdArg = "CHAT_ID"

// Currently the value is the same as [Constants.EMAIL] to get the intent.extras
// Can be updated later after our codebase is fully migrated to Compose Navigation to make it more unique
internal const val manageChatHistoryEmailIdArg = "email"
