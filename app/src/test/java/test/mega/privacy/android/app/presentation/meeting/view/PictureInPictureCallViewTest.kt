package test.mega.privacy.android.app.presentation.meeting.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.meeting.pip.PictureInPictureCallView
import mega.privacy.android.app.meeting.pip.TEST_TAG_PIP_IMAGE
import mega.privacy.android.app.meeting.pip.TEST_TAG_PIP_VIDEO
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PictureInPictureCallViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that video view is shown`() {
        initComposeRuleContent(isVideoOn = true)
        composeRule.onNodeWithTag(TEST_TAG_PIP_VIDEO)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_PIP_IMAGE)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that avatar is shown`() {
        initComposeRuleContent(isVideoOn = false)
        composeRule.onNodeWithTag(TEST_TAG_PIP_IMAGE)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_PIP_VIDEO)
            .assertIsNotDisplayed()
    }

    private fun initComposeRuleContent(
        isVideoOn: Boolean,
    ) {
        composeRule.setContent {
            PictureInPictureCallView(
                isVideoOn = isVideoOn,
                peerId = -1L,
                videoStream = { emptyFlow() })
        }
    }
}
