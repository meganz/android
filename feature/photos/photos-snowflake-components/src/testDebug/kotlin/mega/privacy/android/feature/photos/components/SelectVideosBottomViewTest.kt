package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SelectVideosBottomViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setComposeContent(
        isAddedButtonEnabled: Boolean = false,
        onCancelClicked: () -> Unit = {},
        onAddClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                SelectVideosBottomView(
                    isAddedButtonEnabled = isAddedButtonEnabled,
                    onCancelClicked = onCancelClicked,
                    onAddClicked = onAddClicked,
                )
            }
        }
    }

    @Test
    fun `test that row and both buttons are displayed`() {
        setComposeContent()

        listOf(
            SELECT_VIDEOS_BOTTOM_VIEW_ROW_TEST_TAG,
            SELECT_VIDEOS_BOTTOM_VIEW_CANCEL_BUTTON_TEST_TAG,
            SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG,
        ).forEach {
            composeTestRule.onNodeWithTag(it, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `test that onCancelClicked is invoked when cancel button is clicked`() {
        val onCancelClicked = mock<() -> Unit>()
        setComposeContent(onCancelClicked = onCancelClicked)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEOS_BOTTOM_VIEW_CANCEL_BUTTON_TEST_TAG,
            useUnmergedTree = true
        ).performClick()

        verify(onCancelClicked).invoke()
    }

    @Test
    fun `test that onAddClicked is invoked when add button is clicked`() {
        val onAddClicked = mock<() -> Unit>()
        setComposeContent(isAddedButtonEnabled = true, onAddClicked = onAddClicked)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG,
            useUnmergedTree = true
        ).performClick()

        verify(onAddClicked).invoke()
    }

    @Test
    fun `test that add button is enabled when isAddedButtonEnabled is true`() {
        setComposeContent(isAddedButtonEnabled = true)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG,
            useUnmergedTree = true
        ).assertIsEnabled()
    }

    @Test
    fun `test that add button is not enabled when isAddedButtonEnabled is false`() {
        setComposeContent(isAddedButtonEnabled = false)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG,
            useUnmergedTree = true
        ).assertIsNotEnabled()
    }
}
