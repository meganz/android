package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.shared.original.core.ui.controls.chat.MegaEmojiPickerView

@Composable
fun EmojiBottomSheet(
    onReactionClicked: (String) -> Unit,
){
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .systemBarsPadding()
    ) {
        MegaEmojiPickerView(
            onEmojiPicked = { onReactionClicked(it.emoji) },
            modifier = Modifier
        )
    }
}
