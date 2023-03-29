package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.AvailableOfflineView
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_AVAILABLE_OFFLINE_SWITCH
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class AvailableOfflineViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the switch is enabled when enabled is true`() {
        composeTestRule.setContent {
            AvailableOfflineView(enabled = true, available = true, onCheckChanged = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).assertIsEnabled()
    }

    @Test
    fun `test that the switch is disabled when enabled is false`() {
        composeTestRule.setContent {
            AvailableOfflineView(enabled = false, available = true, onCheckChanged = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).assertIsNotEnabled()
    }

    @Test
    fun `test that the switch is checked when available is true`() {
        composeTestRule.setContent {
            AvailableOfflineView(enabled = true, available = true, onCheckChanged = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).assertIsOn()
    }

    @Test
    fun `test that switch is not checked when available is false`() {
        composeTestRule.setContent {
            AvailableOfflineView(enabled = true, available = false, onCheckChanged = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).assertIsOff()
    }

    @Test
    fun `test that the on checked changed event is fired when enabled is true and switch clicked`() {
        val event = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            AvailableOfflineView(enabled = true, available = false, onCheckChanged = event)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).performClick()
        verify(event).invoke(any())
    }

    @Test
    fun `test that the on checked changed event is not fired when enabled is false and switch clicked`() {
        val event = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            AvailableOfflineView(enabled = false, available = false, onCheckChanged = event)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH).performClick()
        verifyNoInteractions(event)
    }
}