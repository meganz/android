package mega.privacy.android.app.presentation.videosection

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction.Companion.TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VIDEO_RECENTLY_WATCHED_EMPTY_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VIDEO_RECENTLY_WATCHED_HEADER_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VIDEO_RECENTLY_WATCHED_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VideoRecentlyWatchedView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoilApi::class)
@RunWith(AndroidJUnit4::class)
class VideoRecentlyWatchedViewKtTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testDate = "12 April 2024"
    private val testEntity = mock<VideoUIEntity> {
        on { id }.thenReturn(NodeId(1L))
        on { name }.thenReturn("name")
        on { size }.thenReturn(1L)
    }
    private val testGroup = mapOf((testDate to listOf(testEntity)))

    @Before
    fun setUp() {
        val engine = FakeImageLoaderEngine.Builder().build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setComposeContent(
        group: Map<String, List<VideoUIEntity>> = emptyMap(),
        accountType: AccountType? = AccountType.FREE,
        clearRecentlyWatchedVideosSuccess: StateEvent = consumed,
        removeRecentlyWatchedItemSuccess: StateEvent = consumed,
        modifier: Modifier = Modifier,
        onBackPressed: () -> Unit = {},
        onClick: (item: VideoUIEntity, index: Int) -> Unit = { _, _ -> },
        onActionPressed: (VideoSectionMenuAction?) -> Unit = {},
        onMenuClick: (VideoUIEntity) -> Unit = {},
        clearRecentlyWatchedVideosMessageShown: () -> Unit = {},
        removedRecentlyWatchedItemMessageShown: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            VideoRecentlyWatchedView(
                accountType = accountType,
                group = group,
                clearRecentlyWatchedVideosSuccess = clearRecentlyWatchedVideosSuccess,
                removeRecentlyWatchedItemSuccess = removeRecentlyWatchedItemSuccess,
                modifier = modifier,
                onBackPressed = onBackPressed,
                onClick = onClick,
                onActionPressed = onActionPressed,
                onMenuClick = onMenuClick,
                clearRecentlyWatchedVideosMessageShown = clearRecentlyWatchedVideosMessageShown,
                removedRecentlyWatchedItemMessageShown = removedRecentlyWatchedItemMessageShown
            )
        }
    }

    @Test
    fun `test that ui is displayed correctly when the group is empty`() {
        setComposeContent()

        VIDEO_RECENTLY_WATCHED_TOP_BAR_TEST_TAG.assertIsDisplayed()
        VIDEO_RECENTLY_WATCHED_EMPTY_TEST_TAG.assertIsDisplayed()
        VIDEO_RECENTLY_WATCHED_HEADER_TEST_TAG.assertIsNotDisplayed()
    }

    private fun String.assertIsDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    private fun String.assertIsNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    @Test
    fun `test that ui is displayed correctly when the group is not empty`() {
        setComposeContent(testGroup)

        VIDEO_RECENTLY_WATCHED_TOP_BAR_TEST_TAG.assertIsDisplayed()
        VIDEO_RECENTLY_WATCHED_EMPTY_TEST_TAG.assertIsNotDisplayed()
        VIDEO_RECENTLY_WATCHED_HEADER_TEST_TAG.assertIsDisplayed()
        VIDEO_RECENTLY_WATCHED_HEADER_TEST_TAG.assertTextEquals(testDate)
    }

    private fun String.assertTextEquals(value: String) =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true)
            .assertTextEquals(value)

    @Test
    fun `test that onActionPressed is invoked when VideoRecentlyWatchedClearAction is clicked`() {
        val onActionPressed = mock<(VideoSectionMenuAction?) -> Unit>()
        setComposeContent(group = testGroup, onActionPressed = onActionPressed)
        TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION.assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION,
            useUnmergedTree = true
        ).performClick()
        verify(onActionPressed).invoke(VideoSectionMenuAction.VideoRecentlyWatchedClearAction)
    }

    @Test
    fun `test that VideoRecentlyWatchedClearAction does not exist when the group is empty`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_VIDEO_SECTION_RECENTLY_WATCHED_CLEAR_ACTION,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }
}