package mega.privacy.android.core.ui.controls.chat.attachpanel

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AskGalleryPermissionViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test ask gallery permission view shows correctly`() {
        composeRule.setContent {
            AskGalleryPermissionView()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chat_toolbar_bottom_sheet_allow_gallery_access_title))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chat_toolbar_bottom_sheet_grant_gallery_access_button))
            .assertExists()
    }
}