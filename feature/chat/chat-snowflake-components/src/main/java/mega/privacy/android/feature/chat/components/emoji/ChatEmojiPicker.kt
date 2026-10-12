package mega.privacy.android.feature.chat.components.emoji

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Emoji picker for the chat, for example to add a reaction or to insert an emoji in a message.
 *
 * @param onEmojiPicked Called with the emoji the user picks
 * @param modifier [Modifier]. The caller sets the height.
 * @param showEmojiPicker False to hide the picker but keep its view, so it opens again without
 * loading the emojis again
 */
@Composable
fun ChatEmojiPicker(
    onEmojiPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    showEmojiPicker: Boolean = true,
) {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(DSTokens.colors.background.pageBackground)
            .testTag(TEST_TAG_CHAT_EMOJI_PICKER),
        factory = { context ->
            EmojiPickerView(context).apply {
                emojiGridColumns = if (isPortrait) 9 else 18
                setOnEmojiPickedListener { onEmojiPicked(it.emoji) }
            }
        },
        update = { view -> view.isVisible = showEmojiPicker },
    )
}

/**
 * Test tag for the chat emoji picker.
 */
const val TEST_TAG_CHAT_EMOJI_PICKER = "chat_emoji_picker"

@CombinedThemePreviews
@Composable
private fun ChatEmojiPickerPreview() {
    AndroidThemeForPreviews {
        ChatEmojiPicker(
            onEmojiPicked = {},
            modifier = Modifier.height(300.dp),
        )
    }
}
