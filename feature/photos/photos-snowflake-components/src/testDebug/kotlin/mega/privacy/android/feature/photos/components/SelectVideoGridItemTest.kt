package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SelectVideoGridItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val name = "Video Name"

    private fun setComposeContent(
        name: String = "",
        @DrawableRes icon: Int = R.drawable.ic_video_section_video_default_thumbnail,
        onItemClicked: () -> Unit = {},
        modifier: Modifier = Modifier,
        duration: String? = null,
        isSelected: Boolean = false,
        isSensitive: Boolean = false,
        isTakenDown: Boolean = false,
        isFolder: Boolean = false,
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                SelectVideoGridItem(
                    name = name,
                    icon = icon,
                    onItemClicked = onItemClicked,
                    modifier = modifier,
                    duration = duration,
                    thumbnailData = null,
                    isSelected = isSelected,
                    isSensitive = isSensitive,
                    isTakenDown = isTakenDown,
                    isFolder = isFolder,
                )
            }
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when parameters are default value`() {
        setComposeContent(name = name)

        listOf(
            SELECT_VIDEO_GRID_ITEM_NODE_TITLE_TAG,
            SELECT_VIDEO_GRID_ITEM_THUMBNAIL_FILE_TAG,
            SELECT_VIDEO_GRID_ITEM_VIDEO_PLAY_ICON_TAG,
        ).forEach {
            it.assertIsDisplayedWithTag()
        }

        SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG.assertIsNotDisplayedWithTag()
        SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG.assertIsNotDisplayedWithTag()
        composeTestRule.onNodeWithTag(
            SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that the UIs are correctly displayed when all parameters have values or are set to true`() {
        val duration = "3:45"
        setComposeContent(
            name = name,
            duration = duration,
            isSelected = true,
            isTakenDown = true,
        )

        SELECT_VIDEO_GRID_ITEM_NODE_TITLE_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(name)
        }
        SELECT_VIDEO_GRID_ITEM_THUMBNAIL_FILE_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEO_GRID_ITEM_VIDEO_PLAY_ICON_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(duration)
        }
        SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that play icon is not displayed when item is folder`() {
        setComposeContent(name = name, isFolder = true)

        SELECT_VIDEO_GRID_ITEM_VIDEO_PLAY_ICON_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that duration is displayed when duration is not null`() {
        val duration = "12:30"
        setComposeContent(name = name, duration = duration)

        SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(duration)
        }
    }

    @Test
    fun `test that duration is not displayed when duration is null`() {
        setComposeContent(name = name, duration = null)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that duration is not displayed when duration is empty`() {
        setComposeContent(name = name, duration = "")

        composeTestRule.onNodeWithTag(
            SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that select icon is displayed when isSelected is true`() {
        setComposeContent(name = name, isSelected = true)

        SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that select icon is not displayed when isSelected is false`() {
        setComposeContent(name = name, isSelected = false)

        SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that taken down icon is displayed when isTakenDown is true`() {
        setComposeContent(name = name, isTakenDown = true)

        SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that taken down icon is not displayed when isTakenDown is false`() {
        setComposeContent(name = name, isTakenDown = false)

        SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that onItemClicked is invoked when item is performClick`() {
        val onItemClicked = mock<() -> Unit>()
        setComposeContent(name = name, onItemClicked = onItemClicked)

        composeTestRule.onNodeWithTag(
            SELECT_VIDEO_GRID_ITEM_NODE_TITLE_TAG,
            useUnmergedTree = true
        ).performClick()
        verify(onItemClicked).invoke()
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertIsNotDisplayed()

    private fun String.assertTextEqualsWithTag(value: String) =
        composeTestRule.onNodeWithTag(this, useUnmergedTree = true).assertTextEquals(value)
}
