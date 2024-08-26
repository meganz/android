package mega.privacy.android.shared.original.core.ui.controls.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.emoji2.emojipicker.EmojiViewItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class MegaEmojiPickerViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val onEmojiPicked = mock<(EmojiViewItem) -> Unit>()

    @Test
    fun `test that picker is displayed`() {
        composeRule.setContent {
            MegaEmojiPickerView(
                onEmojiPicked = onEmojiPicked,
                showEmojiPicker = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_EMOJI_PICKER_VIEW).assertIsDisplayed()
    }
}