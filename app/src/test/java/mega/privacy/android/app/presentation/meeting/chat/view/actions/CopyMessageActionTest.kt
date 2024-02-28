package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CHAT_BOTTOM_SHEET_OPTION_COPY_TAG
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CopyMessageActionTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var underTest: CopyMessageAction

    @Before
    fun setUp() {
        underTest = CopyMessageAction()
    }

    @Test
    fun `test that action applies to NormalMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to LocationMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<LocationMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to RichPreviewMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<RichPreviewMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply to other types of messages`() {
        assertThat(underTest.appliesTo(setOf(mock<GiphyMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to mixed types of messages`() {
        assertThat(
            underTest.appliesTo(
                setOf(
                    mock<NormalMessage>(),
                    mock<GiphyMessage>()
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that composable contains bottom sheet option`() {
        val bottomSheetMenuItem = underTest.bottomSheetMenuItem(
            messages = setOf(mock<NormalMessage>()),
            hideBottomSheet = {}
        )
        composeTestRule.setContent { bottomSheetMenuItem() }
        composeTestRule.onNodeWithTag(CHAT_BOTTOM_SHEET_OPTION_COPY_TAG).assertExists()
    }
}