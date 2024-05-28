package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MediaQueueItemViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testIcon = R.drawable.ic_audio_medium_solid
    private val testName = "Media Name"
    private val testCurrentPlayingPosition = "00:00"
    private val testDuration = "10:00"

    private fun setComposeContent(
        @DrawableRes icon: Int = testIcon,
        name: String = testName,
        currentPlayingPosition: String = testCurrentPlayingPosition,
        duration: String = testDuration,
        thumbnailData: Any? = null,
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
        isPaused: Boolean = false,
        isItemPlaying: Boolean = false,
        isSelected: Boolean = false,
        isReorderEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            MediaQueueItemView(
                icon = icon,
                name = name,
                currentPlayingPosition = currentPlayingPosition,
                duration = duration,
                thumbnailData = thumbnailData,
                onClick = onClick,
                modifier = modifier,
                isPaused = isPaused,
                isItemPlaying = isItemPlaying,
                isSelected = isSelected,
                isReorderEnabled = isReorderEnabled
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when default parameters are set`() {
        setComposeContent()

        listOf(
            MEDIA_QUEUE_ITEM_VIEW_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG,
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        composeTestRule.onNodeWithText(testDuration).assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()

        listOf(
            MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    private fun String.isDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.doesNotExist() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun `test that the UIs are displayed correctly when isSelected is true`() {
        setComposeContent(isSelected = true)

        listOf(
            MEDIA_QUEUE_ITEM_VIEW_TEST_TAG,
            MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG,
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        composeTestRule.onNodeWithText(testDuration).assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()

        listOf(
            MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when isReorderEnabled is false`() {
        setComposeContent(isReorderEnabled = false)

        listOf(
            MEDIA_QUEUE_ITEM_VIEW_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG,
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        composeTestRule.onNodeWithText(testDuration).assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()

        listOf(
            MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when the current item is playing`() {
        setComposeContent(isItemPlaying = true)

        listOf(
            MEDIA_QUEUE_ITEM_VIEW_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG,
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        val expectedDuration = "$testCurrentPlayingPosition / $testDuration"
        composeTestRule.onNodeWithText(expectedDuration).assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()

        listOf(
            MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.doesNotExist()
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when playing item is paused`() {
        setComposeContent(isPaused = true, isItemPlaying = true)

        listOf(
            MEDIA_QUEUE_ITEM_VIEW_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG,
            MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG,
            MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG
        ).forEach { viewTag ->
            viewTag.isDisplayed()
        }

        val expectedDuration = "$testCurrentPlayingPosition / $testDuration"
        composeTestRule.onNodeWithText(expectedDuration).assertIsDisplayed()
        composeTestRule.onNodeWithText(testName).assertIsDisplayed()

        MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG.doesNotExist()
        MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG.doesNotExist()
    }

    @Test
    fun `test that onClick is invoked when the item is clicked`() {
        val onClick = Mockito.mock<() -> Unit>()
        setComposeContent(onClick = onClick)

        composeTestRule.onNodeWithTag(MEDIA_QUEUE_ITEM_VIEW_TEST_TAG, true).performClick()
        verify(onClick).invoke()
    }
}