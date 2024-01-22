package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AddReactionsSheetItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val onReactionsClicked = mock<(String) -> Unit>()
    private val onMoreReactionsClicked = mock<() -> Unit>()

    @Test
    fun `test that add reactions item correctly displays all the sub-items `() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_ADD_REACTIONS_SHEET_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_SLIGHT_SMILE_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ROLLING_EYES_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_THUMBS_UP_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLAP_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ADD_MORE_REACTIONS).assertIsDisplayed()
        }
    }

    @Test
    fun `test that click add more reactions invokes onMoreReactionsClicked`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_SLIGHT_SMILE_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ADD_MORE_REACTIONS).performClick()
            verify(onMoreReactionsClicked).invoke()
        }
    }

    @Test
    fun `test that click slight smile reaction invokes onReactionClicked correctly`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_SLIGHT_SMILE_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_SLIGHT_SMILE_REACTION).performClick()
            verify(onReactionsClicked).invoke(SLIGHT_SMILE_REACTION)
        }
    }

    @Test
    fun `test that click rolling eyes reaction invokes onReactionClicked correctly`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_ROLLING_EYES_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ROLLING_EYES_REACTION).performClick()
            verify(onReactionsClicked).invoke(ROLLING_EYES_REACTION)
        }
    }

    @Test
    fun `test that click rolling on the floor laughing reaction invokes onReactionClicked correctly`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION).performClick()
            verify(onReactionsClicked).invoke(ROLLING_ON_THE_FLOOR_LAUGHING_REACTION)
        }
    }

    @Test
    fun `test that click thumbs up reaction invokes onReactionClicked correctly`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_THUMBS_UP_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_THUMBS_UP_REACTION).performClick()
            verify(onReactionsClicked).invoke(THUMBS_UP_REACTION)
        }
    }

    @Test
    fun `test that click clap reaction invokes onReactionClicked correctly`() {
        initComposeRule()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CLAP_REACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLAP_REACTION).performClick()
            verify(onReactionsClicked).invoke(CLAP_REACTION)
        }
    }

    private fun initComposeRule() {
        composeRule.setContent {
            AddReactionsSheetItem(
                onReactionClicked = onReactionsClicked,
                onMoreReactionsClicked = onMoreReactionsClicked,
            )
        }
    }
}