package mega.privacy.android.app.presentation.videosection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.VIDEO_SECTION_LOADING_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.AllVideosView
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEOS_EMPTY_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.allvideos.VIDEOS_LIST_TEST_TAG
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoilApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AllVideosViewTest {
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
        items: List<VideoUIEntity> = emptyList(),
        accountType: AccountType? = null,
        progressBarShowing: Boolean = false,
        searchMode: Boolean = false,
        scrollToTop: Boolean = false,
        lazyListState: LazyListState = LazyListState(),
        sortOrder: String = "",
        modifier: Modifier = Modifier,
        selectedLocationFilterOption: LocationFilterOption = LocationFilterOption.AllLocations,
        selectedDurationFilterOption: DurationFilterOption = DurationFilterOption.AllDurations,
        onLocationFilterItemClicked: (LocationFilterOption?) -> Unit = {},
        onDurationFilterItemClicked: (DurationFilterOption?) -> Unit = {},
        onClick: (item: VideoUIEntity, index: Int) -> Unit = { _, _ -> },
        onMenuClick: (VideoUIEntity) -> Unit = { _ -> },
        onSortOrderClick: () -> Unit = {},
        onLongClick: ((item: VideoUIEntity, index: Int) -> Unit) = { _, _ -> },
    ) {
        composeTestRule.setContent {
            AllVideosView(
                items = items,
                accountType = accountType,
                progressBarShowing = progressBarShowing,
                searchMode = searchMode,
                scrollToTop = scrollToTop,
                lazyListState = lazyListState,
                sortOrder = sortOrder,
                modifier = modifier,
                selectedLocationFilterOption = selectedLocationFilterOption,
                selectedDurationFilterOption = selectedDurationFilterOption,
                onLocationFilterItemClicked = onLocationFilterItemClicked,
                onDurationFilterItemClicked = onDurationFilterItemClicked,
                onClick = onClick,
                onMenuClick = onMenuClick,
                onSortOrderClick = onSortOrderClick,
                onLongClick = onLongClick
            )
        }
    }

    @Test
    fun `test that the ui is displayed correctly when progressBarShowing is true`() {
        setComposeContent(progressBarShowing = true)

        composeTestRule.onNodeWithTag(VIDEO_SECTION_LOADING_VIEW_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the ui is displayed correctly when items is empty`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(VIDEOS_EMPTY_VIEW_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the ui is displayed correctly when items is not empty`() {
        val videoUIEntity = mock<VideoUIEntity> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("name")
            on { size }.thenReturn(1L)
        }
        val items = listOf(videoUIEntity)
        setComposeContent(items = items)

        composeTestRule.onNodeWithTag(VIDEOS_LIST_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VIDEOS_FILTER_BUTTON_VIEW_TEST_TAG).assertIsDisplayed()
    }
}