package test.mega.privacy.android.app.presentation.documentsection

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_ITEM_NAME_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_ITEM_SELECTED_ICON_DESCRIPTION
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_ICON_DESCRIPTION
import mega.privacy.android.app.presentation.documentsection.view.DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_DESCRIPTION
import mega.privacy.android.app.presentation.documentsection.view.DocumentGridViewItem
import mega.privacy.android.icon.pack.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DocumentGridViewItemTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setComposeContent(
        isSelected: Boolean = false,
        @DrawableRes icon: Int = R.drawable.ic_text_medium_solid,
        name: String = "name",
        thumbnailData: Any? = null,
        isTakenDown: Boolean = false,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        onMenuClick: () -> Unit = {},
        onLongClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DocumentGridViewItem(
                isSelected = isSelected,
                icon = icon,
                name = name,
                thumbnailData = thumbnailData,
                isTakenDown = isTakenDown,
                modifier = modifier,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when parameters are default value`() {
        setComposeContent()

        listOf(
            DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_DESCRIPTION,
            DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION
        ).forEach { label ->
            composeTestRule.onNodeWithContentDescription(label = label, useUnmergedTree = true)
                .assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_GRID_ITEM_NAME_VIEW_TEST_TAG, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the UIs are correctly displayed when all parameters have values or are set to true`() {
        setComposeContent(isSelected = true, isTakenDown = true)

        listOf(
            DOCUMENT_SECTION_GRID_ITEM_SELECTED_ICON_DESCRIPTION,
            DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION,
            DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_ICON_DESCRIPTION,
            DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_DESCRIPTION
        ).forEach { label ->
            composeTestRule.onNodeWithContentDescription(label = label, useUnmergedTree = true)
                .assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(DOCUMENT_SECTION_GRID_ITEM_NAME_VIEW_TEST_TAG, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onMenuClick invoked when the menu icon is clicked`() {
        val onMenuClick: () -> Unit = mock()
        setComposeContent(onMenuClick = onMenuClick)

        composeTestRule.onNodeWithContentDescription(
            DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION
        ).performClick()
        verify(onMenuClick).invoke()
    }
}