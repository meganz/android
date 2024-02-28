package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.presentation.extensions.copyToClipboard
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CopyBottomSheetOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage

internal class CopyMessageAction : MessageAction() {
    override fun appliesTo(messages: Set<TypedMessage>) =
        messages.all { it is NormalMessage || it is RichPreviewMessage || it is LocationMessage }

    override fun bottomSheetItem(onClick: () -> Unit): @Composable () -> Unit = {
        CopyBottomSheetOption(
            onClick = {
                onClick()
            }
        )
    }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        messages.joinToString(separator = "\n") { it.content.orEmpty() }
            .copyToClipboard(LocalContext.current)
        onHandled()
    }
}