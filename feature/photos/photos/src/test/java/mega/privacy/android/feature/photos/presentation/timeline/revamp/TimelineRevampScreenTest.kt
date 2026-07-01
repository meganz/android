package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineRevampScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that loading skeleton is displayed when state is Loading`() {
        composeRule.setScreen(TimelineRevampUiState.Loading)

        composeRule.onNodeWithTag(TIMELINE_REVAMP_LOADING_SKELETON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when state is Empty`() {
        composeRule.setScreen(TimelineRevampUiState.Empty)

        composeRule.onNodeWithTag(TIMELINE_REVAMP_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that content grid is displayed when state is Data`() {
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "May 2026",
                        startDate = 0L,
                        endDate = 0L,
                        count = 3,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = emptyMap(),
            )
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_CONTENT_GRID_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the grid size selector is displayed when state is Data`() {
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "May 2026",
                        startDate = 0L,
                        endDate = 0L,
                        count = 3,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = emptyMap(),
            )
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_GRID_SIZE_ICON_TAG).assertIsDisplayed()
    }

    private fun ComposeContentTestRule.setScreen(uiState: TimelineRevampUiState) {
        setContent {
            TimelineRevampScreen(
                uiState = uiState,
                onVisibleRangeChanged = { _, _ -> },
                onGridSizeChange = {},
                onZoomIn = {},
                onZoomOut = {},
                onNodeClicked = {},
                onTakenDownDialogEventConsumed = {},
            )
        }
    }
}
