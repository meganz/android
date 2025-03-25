package mega.privacy.android.app.presentation.videosection

import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_COLLECTION_TITLE_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_DURATION_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_FAVOURITE_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_LABEL_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_LINK_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_MENU_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_NAME_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_NODE_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_NODE_TAGS
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_OFFLINE_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_PLAY_ICON_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_SIZE_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_THUMBNAIL_CONTENT_DESCRIPTION
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEO_ITEM_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideoItemView
import mega.privacy.android.icon.pack.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VideoItemViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val name = "Video Name"
    private val fileSize = "100 MB"
    private val duration = 60.seconds
    private val labelColor = Color.Red
    private val collectionTitle = "Collection Title"

    private fun setComposeContent(
        @DrawableRes icon: Int = R.drawable.ic_video_section_video_default_thumbnail,
        name: String = "",
        fileSize: String? = null,
        duration: Duration = 0.seconds,
        collectionTitle: String? = null,
        isFavourite: Boolean = false,
        isSelected: Boolean = false,
        isSharedWithPublicLink: Boolean = false,
        labelColor: Color? = null,
        onClick: () -> Unit = {},
        thumbnailData: Any? = null,
        modifier: Modifier = Modifier,
        showMenuButton: Boolean = true,
        nodeAvailableOffline: Boolean = false,
        onLongClick: (() -> Unit)? = null,
        onMenuClick: () -> Unit = {},
        description: String = "",
        tags: List<String> = emptyList(),
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
                labelColor = labelColor,
                onClick = onClick,
                thumbnailData = thumbnailData,
                modifier = modifier,
                collectionTitle = collectionTitle,
                showMenuButton = showMenuButton,
                nodeAvailableOffline = nodeAvailableOffline,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick,
                description = description,
                tags = tags,
                highlightText = highlightText
            )
        }
    }

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    @Test
    fun `test that the UIs are displayed correctly when parameters are default value`() {
        setComposeContent()
        VIDEO_ITEM_NAME_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        VIDEO_ITEM_DURATION_VIEW_TEST_TAG.assertIsDisplayedWithTag()
        listOf(
            VIDEO_ITEM_SIZE_VIEW_TEST_TAG,
            VIDEO_ITEM_LABEL_VIEW_TEST_TAG,
            VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG
        ).forEach {
            it.assertIsNotDisplayedWithTag()
        }

        listOf(
            VIDEO_ITEM_PLAY_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_THUMBNAIL_CONTENT_DESCRIPTION,
            VIDEO_ITEM_MENU_ICON_CONTENT_DESCRIPTION,
        ).forEach {
            it.assertIsDisplayedWithDescription()
        }

        listOf(
            VIDEO_ITEM_FAVOURITE_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_OFFLINE_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_LINK_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_COLLECTION_TITLE_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_NODE_TAGS,
            VIDEO_ITEM_NODE_DESCRIPTION
        ).forEach {
            it.assertIsNotDisplayedWithDescription()
        }
    }

    @Test
    fun `test that that description is visible when matches with highlight text`() {
        setComposeContent(
            description = "This is a description with highlight text",
            highlightText = "highlight"
        )
        composeTestRule.onNodeWithTag(VIDEO_ITEM_NODE_DESCRIPTION, true).assertIsDisplayed()
    }

    @Test
    fun `test that that tags are visible when matches with highlight text`() {
        setComposeContent(
            tags = listOf("tag1", "tag2", "highlight", "tag3"),
            highlightText = "highlight"
        )
        composeTestRule.onNodeWithTag(VIDEO_ITEM_NODE_TAGS, true).assertIsDisplayed()
    }

    private fun String.assertIsNotDisplayedWithTag() =
        composeTestRule.onNodeWithTag(this, true).assertIsNotDisplayed()

    private fun String.assertIsNotDisplayedWithDescription() =
        composeTestRule.onNodeWithContentDescription(this, true).assertIsNotDisplayed()

    private fun String.assertIsDisplayedWithDescription() =
        composeTestRule.onNodeWithContentDescription(this, true).assertIsDisplayed()

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
            labelColor = labelColor,
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
            assertTextEqualsWithTag("1:00")
        }
        VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG.run {
            assertIsDisplayedWithTag()
            assertTextEqualsWithTag(collectionTitle)
        }
        VIDEO_ITEM_LABEL_VIEW_TEST_TAG.assertIsDisplayedWithTag()

        listOf(
            VIDEO_ITEM_FAVOURITE_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_THUMBNAIL_CONTENT_DESCRIPTION,
            VIDEO_ITEM_MENU_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_PLAY_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_OFFLINE_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_LINK_ICON_CONTENT_DESCRIPTION,
            VIDEO_ITEM_COLLECTION_TITLE_ICON_CONTENT_DESCRIPTION
        ).forEach {
            it.assertIsDisplayedWithDescription()
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

        composeTestRule.onNodeWithTag(VIDEO_ITEM_VIEW_TEST_TAG, true).performClick()
        verify(onClick).invoke()
    }

    @Test
    fun `test that onMenuClick is invoked when menu icon is preformed`() {
        val onMenuClick = mock<() -> Unit>()
        setComposeContent(
            isSelected = false,
            onMenuClick = onMenuClick
        )

        composeTestRule.onNodeWithContentDescription(
            label = VIDEO_ITEM_MENU_ICON_CONTENT_DESCRIPTION,
            useUnmergedTree = true
        ).performClick()
        verify(onMenuClick).invoke()
    }
}