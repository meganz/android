package mega.privacy.android.feature.chat.settings.calls.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CallSettingsViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun initComposeRuleContent(
        onBackPressed: () -> Unit = {},
        isSoundNotificationActive: Boolean? = false,
        onSoundNotificationChanged: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            CallSettingsView(
                onBackPressed, isSoundNotificationActive, onSoundNotificationChanged,
            )
        }
    }

    @Test
    fun `test that title is shown`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.settings_calls_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that onBackPressed is invoked when navigation icon is clicked`() {
        val onBackPressed = mock<() -> Unit>()
        initComposeRuleContent(onBackPressed = onBackPressed)
        composeRule.onNodeWithContentDescription(
            "Navigation Icon",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true
        ).performClick()

        verify(onBackPressed).invoke()
    }

    @Test
    fun `test that sound notification setting is shown`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that toggle is not enabled when isSoundNotificationActive is null`() {
        initComposeRuleContent(
            isSoundNotificationActive = null
        )
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TOGGLE_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that toggle is enabled and checked when isSoundNotificationActive is true`() {
        initComposeRuleContent(
            isSoundNotificationActive = true
        )
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TOGGLE_TEST_TAG)
            .assertIsEnabled()
            .assertIsOn()
    }

    @Test
    fun `test that toggle is enabled and unchecked when isSoundNotificationActive is false`() {
        initComposeRuleContent(
            isSoundNotificationActive = false
        )
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TOGGLE_TEST_TAG)
            .assertIsEnabled()
            .assertIsOff()
    }

    @Test
    fun `test that onSoundNotificationChanged is invoked when item is clicked`() {
        val onSoundNotificationChanged = mock<(Boolean) -> Unit>()
        initComposeRuleContent(
            onSoundNotificationChanged = onSoundNotificationChanged
        )
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TEST_TAG).performClick()
        verify(onSoundNotificationChanged).invoke(any())
    }

    @Test
    fun `test that onSoundNotificationChanged is invoked when toggle is clicked`() {
        val onSoundNotificationChanged = mock<(Boolean) -> Unit>()
        initComposeRuleContent(
            onSoundNotificationChanged = onSoundNotificationChanged
        )
        composeRule.onNodeWithTag(SOUND_NOTIFICATION_TOGGLE_TEST_TAG).performClick()
        verify(onSoundNotificationChanged).invoke(any())
    }
}