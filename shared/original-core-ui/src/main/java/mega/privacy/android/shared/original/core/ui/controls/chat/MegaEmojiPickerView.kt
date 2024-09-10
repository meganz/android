package mega.privacy.android.shared.original.core.ui.controls.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Emoji picker view.
 */
@Composable
fun MegaEmojiPickerView(
    onEmojiPicked: (EmojiViewItem) -> Unit,
    showEmojiPicker: Boolean = true,
    modifier: Modifier,
) = Column {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(MegaOriginalTheme.colors.background.pageBackground)
            .testTag(TEST_TAG_EMOJI_PICKER_VIEW),
        factory = { context ->
            EmojiPickerView(context).apply {
                emojiGridColumns = if (isPortrait) 9 else 18
                setOnEmojiPickedListener { emoji -> onEmojiPicked(emoji) }
            }
        },
        update = { view: EmojiPickerView ->
            view.isVisible = showEmojiPicker
        },
    )
}

/**
 * Tag used to identify the emoji picker view.
 */
const val TEST_TAG_EMOJI_PICKER_VIEW = "emoji_picker_view"