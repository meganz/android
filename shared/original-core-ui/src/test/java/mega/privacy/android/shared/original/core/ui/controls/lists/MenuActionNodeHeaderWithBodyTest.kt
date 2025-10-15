package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [MenuActionNodeHeaderWithBody]
 */
@RunWith(AndroidJUnit4::class)
internal class MenuActionNodeHeaderWithBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the content is displayed with only the necessary parameters provided`() {
        composeTestRule.setContent {
            MenuActionNodeHeaderWithBody(
                title = "Node Title",
                body = "Node Body",
                nodeIcon = mega.privacy.android.icon.pack.R.drawable.ic_mic_medium_thin_solid,
            )
        }
        composeTestRule.onNodeWithTag(HEADER_MAIN_CONTAINER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HEADER_NODE_IMAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HEADER_NODE_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(HEADER_NODE_BODY).assertIsDisplayed()
    }

    @Test
    fun `test that the optional body icon is shown`() {
        composeTestRule.setContent {
            MenuActionNodeHeaderWithBody(
                title = "Node Title",
                body = "Node Body",
                nodeIcon = mega.privacy.android.icon.pack.R.drawable.ic_mic_medium_thin_solid,
                bodyIcon = R.drawable.ic_favorite,
            )
        }
        composeTestRule.onNodeWithTag(HEADER_NODE_BODY_ICON).assertIsDisplayed()
    }

    @Test
    fun `test that the optional subtitle is shown`() {
        composeTestRule.setContent {
            MenuActionNodeHeaderWithBody(
                title = "Node Title",
                body = "Node Body",
                nodeIcon = mega.privacy.android.icon.pack.R.drawable.ic_mic_medium_thin_solid,
                subTitle = "Subtitle Text",
            )
        }
        composeTestRule.onNodeWithTag(HEADER_NODE_SUBTITLE).assertIsDisplayed()
    }
}
