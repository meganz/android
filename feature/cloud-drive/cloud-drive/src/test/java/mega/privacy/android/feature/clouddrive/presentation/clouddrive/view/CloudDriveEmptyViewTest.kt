package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CloudDriveEmptyViewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that empty image is displayed`() {
        setupComposeContent()

        composeRule.onNodeWithTag(EMPTY_IMAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed`() {
        setupComposeContent()

        composeRule.onNodeWithTag(EMPTY_TITLE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that description is displayed when showAddItems is true`() {
        setupComposeContent(showAddItems = true)

        composeRule.onNodeWithTag(EMPTY_DESCRIPTION_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that description is not displayed when showAddItems is false`() {
        setupComposeContent(showAddItems = false)

        composeRule.onNodeWithTag(EMPTY_DESCRIPTION_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that add items button is displayed when showAddItems is true`() {
        setupComposeContent(showAddItems = true)

        composeRule.onNodeWithTag(ADD_ITEMS_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that add items button is not displayed when showAddItems is false`() {
        setupComposeContent(showAddItems = false)

        composeRule.onNodeWithTag(ADD_ITEMS_BUTTON_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that onAddItemsClicked is called when button is clicked`() {
        val onAddItemsClicked = mock<() -> Unit>()
        setupComposeContent(onAddItemsClicked = onAddItemsClicked)

        composeRule.onNodeWithTag(ADD_ITEMS_BUTTON_TAG).performClick()

        verify(onAddItemsClicked).invoke()
    }

    private fun setupComposeContent(
        isRootCloudDrive: Boolean = false,
        showAddItems: Boolean = true,
        onAddItemsClicked: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                CloudDriveEmptyView(
                    isRootCloudDrive = isRootCloudDrive,
                    showAddItems = showAddItems,
                    onAddItemsClicked = onAddItemsClicked
                )
            }
        }
    }
}

