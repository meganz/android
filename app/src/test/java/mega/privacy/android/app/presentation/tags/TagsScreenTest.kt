package mega.privacy.android.app.presentation.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun `test that tags info text is shown when there is no tags already created`() {
        // Test the TagsScreen
        composeTestRule.setContent {
            TagsScreen(
                consumeInfoMessage = {},
                validateTagName = { _ -> },
                addOrRemoveTag = { _ -> },
                onBackPressed = {},
                consumeMaxTagsError = {},
                uiState = TagsUiState(),
                consumeTagsUpdated = {},
            )
        }

        // Verify the TagsScreen
        composeTestRule.onNodeWithTag(TAGS_SCREEN_APP_BAR).assertExists()
        composeTestRule.onNodeWithTag(TAGS_SCREEN_CONTENTS_LABEL).assertExists()
        composeTestRule.onNodeWithText(
            text = "Tags",
            substring = true,
            ignoreCase = true
        ).assertIsDisplayed()

    }

    @Test
    fun `test that tags are displayed when tags available in UI state`() {
        composeTestRule.setContent {
            TagsScreen(
                consumeInfoMessage = {},
                validateTagName = { _ -> },
                addOrRemoveTag = { _ -> },
                onBackPressed = {},
                consumeMaxTagsError = {},
                uiState = TagsUiState(tags = persistentListOf("tag1", "tag2")),
                consumeTagsUpdated = {}
            )
        }

        composeTestRule.onNodeWithTag(TAGS_SCREEN_APP_BAR).assertExists()
        composeTestRule.onNodeWithTag(TAGS_SCREEN_EXISTING_TAGS_LABEL).assertExists()
        composeTestRule.onNodeWithText(
            text = "tag1",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            text = "tag2",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }
}