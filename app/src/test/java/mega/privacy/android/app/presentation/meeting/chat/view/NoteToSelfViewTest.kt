package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class NoteToSelfViewTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that avatar is shown`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(
            NOTE_TO_SELF_ITEM_AVATAR_IMAGE,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `test that text is shown`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(
            NOTE_TO_SELF_ITEM_TITLE_TEXT,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    private fun initComposeRuleContent() {
        composeRule.setContent {
            NoteToSelfView(
                onNoteToSelfClicked = {},
            )
        }
    }
}