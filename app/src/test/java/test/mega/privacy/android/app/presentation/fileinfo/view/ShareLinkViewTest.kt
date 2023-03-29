package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.ShareLinkView
import mega.privacy.android.app.presentation.fileinfo.view.TEST_SHARE_LINK_COPY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ShareLinkViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that on copy link click event is fired when copy button is clicked`() {
        val event = mock<() -> Unit>()
        composeTestRule.setContent {
            ShareLinkView(link = "link", date = 1233, onCopyLinkClick = event)
        }
        composeTestRule.onNodeWithTag(TEST_SHARE_LINK_COPY).performClick()
        verify(event).invoke()
    }
}