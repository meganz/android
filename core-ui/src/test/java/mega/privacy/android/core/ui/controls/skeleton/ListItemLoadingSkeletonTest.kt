package mega.privacy.android.core.ui.controls.skeleton

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [ListItemLoadingSkeleton]
 */
@RunWith(AndroidJUnit4::class)
internal class ListItemLoadingSkeletonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the loading item is displayed`() {
        composeTestRule.setContent { ListItemLoadingSkeleton() }
        composeTestRule.onNodeWithTag(LIST_ITEM_LOADING_SKELETON).assertIsDisplayed()
    }
}