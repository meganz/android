package mega.privacy.android.core.ui.controls.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Emoji picker view.
 */
@Composable
fun MegaEmojiPickerView(
    onEmojiPicked: (EmojiViewItem) -> Unit,
    showEmojiPicker: Boolean,
) = Column {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isPortrait) 300.dp else 150.dp)
            .background(MegaTheme.colors.background.pageBackground)
            .testTag(TEST_TAG_EMOJI_PICKER_VIEW),
        factory = { context ->
            EmojiPickerView(context).apply {
                emojiGridColumns = if (isPortrait) 9 else 18
                setOnEmojiPickedListener { emoji -> onEmojiPicked(emoji) }
            }
        },
        update = {
            it.isVisible = showEmojiPicker
        },
    )
}

/**
 * Tag used to identify the emoji picker view.
 */
const val TEST_TAG_EMOJI_PICKER_VIEW = "emoji_picker_view"