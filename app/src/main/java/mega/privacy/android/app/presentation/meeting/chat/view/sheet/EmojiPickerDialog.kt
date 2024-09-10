package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.cards.MegaCard
import mega.privacy.android.shared.original.core.ui.controls.chat.MegaEmojiPickerView

/**
 * Emoji picker dialog
 *
 * @param onReactionClicked
 */
@Composable
fun EmojiPickerDialog(
    onReactionClicked: (String) -> Unit,
){
    MegaCard(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        content = {
            MegaEmojiPickerView(
                onEmojiPicked = { onReactionClicked(it.emoji) },
                modifier = Modifier
            )
        }
    )
}
