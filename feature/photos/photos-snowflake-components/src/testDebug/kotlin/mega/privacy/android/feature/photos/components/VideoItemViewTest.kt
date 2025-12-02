package mega.privacy.android.feature.photos.components

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VideoItemViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val name = "Video Name"
    private val fileSize = "100 MB"
    private val duration = "3:45"
    private val labelViewTag = "MOCK_LABEL"
    private val labelView: @Composable () -> Unit = {
        Text(
            "Label",
            modifier = Modifier.testTag(labelViewTag)
        )
    }
    private val tagsRowTag = "MOCK_TAGS_ROW"
    private val tagsRow: @Composable () -> Unit = {
        Text(
            "Tags Row",
            modifier = Modifier.testTag(tagsRowTag)
        )
    }
    private val collectionTitle = "Collection Title"

    private fun setComposeContent(
        @DrawableRes icon: Int = R.drawable.ic_video_section_video_default_thumbnail,
        name: String = "",
        fileSize: String? = null,
        duration: String = "",
        collectionTitle: String? = null,
        isFavourite: Boolean = false,
        isSelected: Boolean = false,
        isSharedWithPublicLink: Boolean = false,
        labelView: @Composable (() -> Unit)? = null,
        onClick: () -> Unit = {},
        thumbnailData: Any? = null,
        modifier: Modifier = Modifier,
        showMenuButton: Boolean = true,
        nodeAvailableOffline: Boolean = false,
        onLongClick: (() -> Unit)? = null,
        onMenuClick: () -> Unit = {},
        description: String = "",
        tagsRow: @Composable (() -> Unit)? = null,
        highlightText: String = "",
    ) {
        composeTestRule.setContent {
            VideoItemView(
                icon = icon,
                name = name,
                fileSize = fileSize,
                duration = duration,
                isFavourite = isFavourite,
                isSelected = isSelected,
                isSharedWithPublicLink = isSharedWithPublicLink,
                labelView = labelView,
                onClick = onClick,
                thumbnailData = thumbnailData,
                modifier = modifier,
                collectionTitle = collectionTitle,
                showMenuButton = showMenuButton,
                nodeAvailableOffline = nodeAvailableOffline,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick,
                description = description,
                tagsRow = tagsRow,
                highlightText = highlightText
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when parameters are default value`() {
        setComposeContent(duration = duration)
        listOf(
            VIDEO_ITEM_NAME_VIEW_TEST_TAG,
            VIDEO_ITEM_DURATION_VIEW_TEST_TAG,
            VIDEO_ITEM_MENU_ICON_TEST_TAG,
            VIDEO_ITEM_THUMBNAIL_TEST_TAG,
            VIDEO_ITEM_PLAY_ICON_TEST_TAG,
        ).forEach {
            it.assertIsDisplayedWithTag()
        }

        listOf(
            labelViewTag,
            VIDEO_ITEM_SIZE_VIEW_TEST_TAG,
            VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG
        ).forEach {
            it.assertIsNotDisplayedWithTag()
        }

        listOf(
            VIDEO_ITEM_FAVOURITE_ICON_TEST_TAG,
            VIDEO_ITEM_OFFLINE_ICON_TEST_TAG,
            VIDEO_ITEM_LINK_ICON_TEST_TAG,
            VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG,
            VIDEO_ITEM_NODE_DESCRIPTION_TEST_TAG
        ).forEach {
            it.assertIsNotDisplayedWithTag()
        }
    }

    @Test
    fun `test that description is visible when matches with highlight text`() {
        setComposeContent(
            description = "This is a description with highlight text",
            highlightText = "highlight"
        )
        composeTestRule.onNodeWithTag(VIDEO_ITEM_NODE_DESCRIPTION_TEST_TAG, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that tags are visible when matches with highlight text`() {
        setComposeContent(
            tagsRow = tagsRow,
            highlightText = "highlight"
        )
        composeTestRule.onNodeWithTag(tagsRowTag, true).assertIsDisplayed()
    }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsNotDisplayed()

    @Test
    fun `test that the UIs are correctly displayed when all parameters have values or are set to true`() {
        setComposeContent(
            modifier = Modifier.fillMaxHeight(),
            name = name,
            fileSize = fileSize,
            duration = duration,
            collectionTitle = collectionTitle,
            isFavourite = true,
            isSelected = true,
            isSharedWithPublicLink = true,
            labelView = labelView,
            thumbnailData = null,
            showMenuButton = true,
            nodeAvailableOffline = true,
        )

        VIDEO_ITEM_NAME_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_ITEM_SIZE_VIEW_TEST_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(fileSize)
        }
        VIDEO_ITEM_DURATION_VIEW_TEST_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(duration)
        }
        VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(collectionTitle)
        }

        listOf(
            labelViewTag,
            VIDEO_ITEM_FAVOURITE_ICON_TEST_TAG,
            VIDEO_ITEM_THUMBNAIL_TEST_TAG,
            VIDEO_ITEM_MENU_ICON_TEST_TAG,
            VIDEO_ITEM_PLAY_ICON_TEST_TAG,
            VIDEO_ITEM_OFFLINE_ICON_TEST_TAG,
            VIDEO_ITEM_LINK_ICON_TEST_TAG,
            VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG
        ).forEach {
            it.assertIsDisplayedWithTag()
        }
    }

    private fun String.assertIsDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsDisplayed()

    private fun String.assertTextEqualsWithTag(value: String) =
        composeTestRule.onNodeWithTag(this, true).assertTextEquals(value)

    @Test
    fun `test that onClick is invoked when item is performClick`() {
        val onClick = mock<() -> Unit>()
        setComposeContent(onClick = onClick)

        composeTestRule.onNodeWithTag(
            VIDEO_ITEM_VIEW_TEST_TAG,
            true
        ).performClick()
        verify(onClick).invoke()
    }

    @Test
    fun `test that onMenuClick is invoked when menu icon is preformed`() {
        val onMenuClick = mock<() -> Unit>()
        setComposeContent(
            isSelected = false,
            onMenuClick = onMenuClick
        )

        composeTestRule.onNodeWithTag(
            testTag = VIDEO_ITEM_MENU_ICON_TEST_TAG,
            useUnmergedTree = true
        ).performClick()
        verify(onMenuClick).invoke()
    }
}