package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class ShareMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val underTest: ShareMessageAction = ShareMessageAction()

    @Test
    fun `test that action applies to node attachment messages`() {
        assertThat(underTest.appliesTo(setOf(mock<NodeAttachmentMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply if one of message is not attachment message`() {
        assertThat(
            underTest.appliesTo(
                setOf(
                    mock<NodeAttachmentMessage>(),
                    mock<TextMessage>(),
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that open with option shows correctly`() {
        val hideBottomSheet = mock<() -> Unit>()
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                setOf(mock<NodeAttachmentMessage>()),
                hideBottomSheet
            ) {}
        )
        with(composeTestRule) {
            onNodeWithTag(underTest.bottomSheetItemTestTag).assertIsDisplayed()
            onNodeWithText(composeTestRule.activity.getString(R.string.general_share)).assertIsDisplayed()
            onNodeWithTag(underTest.bottomSheetItemTestTag).performClick()
            verify(hideBottomSheet).invoke()
        }
    }
}