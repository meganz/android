package mega.privacy.android.app.presentation.mediaplayer

import androidx.annotation.DrawableRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.view.MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG
import mega.privacy.android.app.mediaplayer.queue.view.MediaQueueItemWithHeaderAndFooterView
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaQueueItemWithHeaderAndFooterViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testHeaderTextPlaying = "Now playing"
    private val testHeaderTextPlayingWithPaused = "Now playing (paused)"
    private val testHeaderTextPrevious = "Previous"
    private val testTextFooter = "Next"

    private fun setComposeContent(
        @DrawableRes icon: Int = R.drawable.ic_audio_medium_solid,
        name: String = "Media Name",
        currentPlayingPosition: String = "00:00",
        duration: String = "10:00",
        thumbnailData: Any? = null,
        isHeaderVisible: Boolean = false,
        isFooterVisible: Boolean = false,
        queueItemType: MediaQueueItemType = MediaQueueItemType.Playing,
        isAudio: Boolean = true,
        isPaused: Boolean = false,
        isSelected: Boolean = false,
        isSearchMode: Boolean = false,
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            MediaQueueItemWithHeaderAndFooterView(
                icon = icon,
                name = name,
                currentPlayingPosition = currentPlayingPosition,
                duration = duration,
                thumbnailData = thumbnailData,
                isHeaderVisible = isHeaderVisible,
                isFooterVisible = isFooterVisible,
                queueItemType = queueItemType,
                isAudio = isAudio,
                isPaused = isPaused,
                onClick = onClick,
                isSelected = isSelected,
                modifier = modifier,
                isSearchMode = isSearchMode
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when the queueItemType is Playing`() {
        setComposeContent(isHeaderVisible = true, isFooterVisible = true)

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isNotDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.textEquals(testHeaderTextPlaying)
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.textEquals(testTextFooter)
    }

    private fun String.isDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.textEquals(expectedText: String) =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)
            .assertTextEquals(expectedText)

    private fun String.isNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    @Test
    fun `test that the UIs are displayed correctly when the queueItemType is Playing and isPaused is true`() {
        setComposeContent(isPaused = true, isHeaderVisible = true, isFooterVisible = true)

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isNotDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.textEquals(testHeaderTextPlayingWithPaused)
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.textEquals(testTextFooter)
    }

    @Test
    fun `test that the UIs are displayed correctly when the queueItemType is Playing and isFooterVisible is false`() {
        setComposeContent(isPaused = true, isHeaderVisible = true)

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isNotDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.textEquals(testHeaderTextPlayingWithPaused)
    }

    @Test
    fun `test that the UIs are displayed correctly when isHeaderVisible is true and queueItemType is Previous`() {
        setComposeContent(isHeaderVisible = true, queueItemType = MediaQueueItemType.Previous)

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isNotDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.textEquals(testHeaderTextPrevious)
    }

    @Test
    fun `test that the UIs are displayed correctly when queueItemType is Next`() {
        setComposeContent(queueItemType = MediaQueueItemType.Next)
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isNotDisplayed()
    }

    @Test
    fun `test that the UIs are displayed correctly when search mode is enabled`() {
        setComposeContent(queueItemType = MediaQueueItemType.Playing, isSearchMode = true)
        MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG.isDisplayed()
        MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG.isDisplayed()

        MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG.isNotDisplayed()
        MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG.isNotDisplayed()
    }
}