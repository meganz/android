package mega.privacy.android.core.ui.controls.lists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [MenuActionListTileWithBody]
 */
@RunWith(AndroidJUnit4::class)
internal class MenuActionListTileWithBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the content is displayed with only the necessary parameters provided`() {
        composeTestRule.setContent {
            MenuActionListTileWithBody(
                title = "Tile Title",
                body = "Tile Body",
                icon = R.drawable.ic_folder_list
            )
        }
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_MAIN_CONTAINER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_ICON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_TEXT_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_TEXT_BODY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_DIVIDER).assertIsDisplayed()
    }

    @Test
    fun `test that the divider is hidden`() {
        composeTestRule.setContent {
            MenuActionListTileWithBody(
                title = "Tile Title",
                body = "Tile Body",
                icon = R.drawable.ic_folder_list,
                addSeparator = false,
            )
        }
        composeTestRule.onNodeWithTag(TILE_WITH_BODY_DIVIDER).assertDoesNotExist()
    }
}