package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.TakeDownWarningView
import mega.privacy.android.core.ui.controls.banners.TEST_TAG_WARNING_BANNER_CLOSE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class TakeDownWarningViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that on close click event is fired when close button is clicked`() {
        val onCloseEvent = mock<() -> Unit>()
        composeTestRule.setContent {
            TakeDownWarningView(isFile = true, onCloseClick = onCloseEvent, onLinkClick = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_WARNING_BANNER_CLOSE, true).performClick()
        verify(onCloseEvent).invoke()
    }
}