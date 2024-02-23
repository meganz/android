package mega.privacy.android.app.presentation.meeting.chat.view

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.TEST_TAG_ENABLE_GEOLOCATION_DIALOG
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.INVALID_LOCATION_MESSAGE_ID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ChatLocationViewTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val onEnableGeolocation = mock<() -> Unit>()
    private val onSendLocationMessage = mock<(Intent?) -> Unit>()
    private val onDismissView = mock<() -> Unit>()

    @Test
    fun `test that view is shown`() {
        initComposeRule()
        composeRule.onNodeWithTag(CHAT_LOCATION_VIEW_TAG).assertExists()
    }

    @Test
    fun `test that geolocation dialog shows if geolocation is not enabled`() {
        initComposeRule(isGeolocationEnabled = false)
        composeRule.onNodeWithTag(TEST_TAG_ENABLE_GEOLOCATION_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that geolocation dialog does not show if geolocation is enabled`() {
        initComposeRule(isGeolocationEnabled = true)
        composeRule.onNodeWithTag(TEST_TAG_ENABLE_GEOLOCATION_DIALOG).assertDoesNotExist()
    }

    private fun initComposeRule(isGeolocationEnabled: Boolean = true) {
        composeRule.setContent {
            ChatLocationView(
                isGeolocationEnabled = isGeolocationEnabled,
                onEnableGeolocation = onEnableGeolocation,
                onSendLocationMessage = onSendLocationMessage,
                onDismissView = onDismissView,
                msgId = INVALID_LOCATION_MESSAGE_ID,
            )
        }
    }
}