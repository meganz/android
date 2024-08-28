package mega.privacy.android.app.presentation.meeting.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.view.ParticipantsLimitWarningComposeView
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_PARTICIPANTS_LIMIT_WARNING_MODERATOR_VIEW
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_PARTICIPANTS_LIMIT_WARNING_VIEW
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ParticipantsLimitWarningComposeViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that warning view for organiser is shown`() {
        initComposeRuleContent(isModerator = true)
        composeRule.onNodeWithTag(TEST_TAG_PARTICIPANTS_LIMIT_WARNING_MODERATOR_VIEW)
            .assertIsDisplayed()
    }

    @Test
    fun `test that warning view for participants is shown`() {
        initComposeRuleContent(isModerator = false)
        composeRule.onNodeWithTag(TEST_TAG_PARTICIPANTS_LIMIT_WARNING_VIEW)
            .assertIsDisplayed()
    }

    private fun initComposeRuleContent(
        isModerator: Boolean,
    ) {
        composeRule.setContent {
            ParticipantsLimitWarningComposeView(isModerator = isModerator)
        }
    }
}
