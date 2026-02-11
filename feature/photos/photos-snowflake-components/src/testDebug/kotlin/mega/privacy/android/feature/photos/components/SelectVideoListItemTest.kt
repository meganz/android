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
class SelectVideoListItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val title = "Video Title"
    private val subtitle = "Video Subtitle"

    private fun setComposeContent(
        title: String = "",
        subtitle: String = "",
        @DrawableRes icon: Int = R.drawable.ic_video_section_video_default_thumbnail,
        onItemClicked: () -> Unit = {},
        modifier: Modifier = Modifier,
        titleMaxLines: Int = 1,
        isSelected: Boolean = false,
        isSensitive: Boolean = false,
        isTakenDown: Boolean = false,
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                SelectVideoListItem(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    onItemClicked = onItemClicked,
                    modifier = modifier,
                    thumbnailData = null,
                    titleMaxLines = titleMaxLines,
                    isSelected = isSelected,
                    isSensitive = isSensitive,
                    isTakenDown = isTakenDown,
                )
            }
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when parameters are default value`() {
        setComposeContent(title = title, subtitle = subtitle)

        listOf(
            SELECT_VIDEO_LIST_ITEM_TITLE_TAG,
            SELECT_VIDEO_LIST_ITEM_SUBTITLE_TAG,
            SELECT_VIDEO_LIST_ITEM_ICON_TAG,
        ).forEach {
            it.assertIsDisplayedWithTag()
        }

        listOf(
            SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG,
            SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG,
        ).forEach {
            it.assertIsNotDisplayedWithTag()
        }
    }

    @Test
    fun `test that the UIs are correctly displayed when all parameters have values or are set to true`() {
        setComposeContent(
            title = title,
            subtitle = subtitle,
            isSelected = true,
            isTakenDown = true,
        )

        SELECT_VIDEO_LIST_ITEM_TITLE_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(title)
        }
        SELECT_VIDEO_LIST_ITEM_SUBTITLE_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(subtitle)
        }
        SELECT_VIDEO_LIST_ITEM_ICON_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG.assertIsDisplayedWithTag()
        SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that selected icon is displayed when isSelected is true`() {
        setComposeContent(
            title = title,
            subtitle = subtitle,
            isSelected = true,
        )

        SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that selected icon is not displayed when isSelected is false`() {
        setComposeContent(
            title = title,
            subtitle = subtitle,
            isSelected = false,
        )

        SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that taken down icon is displayed when isTakenDown is true`() {
        setComposeContent(
            title = title,
            subtitle = subtitle,
            isTakenDown = true,
        )

        SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG.assertIsDisplayedWithTag()
    }

    @Test
    fun `test that taken down icon is not displayed when isTakenDown is false`() {
        setComposeContent(
            title = title,
            subtitle = subtitle,
            isTakenDown = false,
        )

        SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG.assertIsNotDisplayedWithTag()
    }

    @Test
    fun `test that onItemClicked is invoked when item is performClick`() {
        val onItemClicked = mock<() -> Unit>()
        setComposeContent(
            title = title,
            subtitle = subtitle,
            onItemClicked = onItemClicked,
        )

        composeTestRule.onNodeWithTag(
            SELECT_VIDEO_LIST_ITEM_TITLE_TAG,
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
