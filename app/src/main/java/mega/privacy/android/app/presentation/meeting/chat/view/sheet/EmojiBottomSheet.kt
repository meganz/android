package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.runtime.Composable
import mega.privacy.android.shared.original.core.ui.controls.chat.MegaEmojiPickerView

@Composable
fun EmojiBottomSheet(
    onReactionClicked: (String) -> Unit,
){
    MegaEmojiPickerView(
        onEmojiPicked = {
            //Add reaction
            onReactionClicked(it.emoji)
        },
    )
}