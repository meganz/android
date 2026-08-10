package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
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

    @Test
    fun `test that the header is displayed when state is Data`() {
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

        // At rest the scrolling (non-sticky) header sits at the top; the pinned overlay only appears
        // once it scrolls past the viewport top.
        composeRule.onNodeWithTag(TIMELINE_REVAMP_NON_STICKY_HEADER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that card skeleton is displayed when period cards are loading for Years`() {
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
                selectedPeriod = MediaTimePeriod.Years,
                periodCards = emptyList(),
                arePeriodCardsLoading = true,
            )
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_CARD_LIST_SKELETON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that card list is displayed when period cards finished loading for Years`() {
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
                selectedPeriod = MediaTimePeriod.Years,
                periodCards = listOf(
                    PhotosNodeListCard(
                        period = PhotosNodeListCardPeriod.Year,
                        key = 1L,
                        id = 1L,
                        day = 1,
                        month = 1,
                        year = 2026,
                        formattedDate = "2026",
                        thumbnailFilePath = null,
                        previewFilePath = null,
                        extension = "",
                        isSensitive = false,
                        count = 3,
                    ),
                ),
                arePeriodCardsLoading = false,
            )
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_CARD_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_REVAMP_CARD_LIST_SKELETON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that day sections in the same month are grouped under a single month header`() {
        // 2026-06-20 (first month, shown by the sticky header) then 2026-05-15 and 2026-05-10 —
        // two day sections in May that must collapse into a single May inline header.
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-20",
                        startDate = 1_781_913_600L,
                        endDate = 1_781_913_600L,
                        count = 1,
                    ),
                    MediaTimelineSection(
                        groupId = "2026-05-15",
                        startDate = 1_778_803_200L,
                        endDate = 1_778_803_200L,
                        count = 1,
                    ),
                    MediaTimelineSection(
                        groupId = "2026-05-10",
                        startDate = 1_778_371_200L,
                        endDate = 1_778_371_200L,
                        count = 1,
                    ),
                ),
                sectionStartOffsets = listOf(0, 1, 2),
                loadedNodes = emptyMap(),
            )
        )

        composeRule.onAllNodesWithTag("${TIMELINE_REVAMP_SECTION_HEADER_TAG}2026-5").assertCountEquals(1)
    }

    private fun ComposeContentTestRule.setScreen(uiState: TimelineRevampUiState) {
        setContent {
            TimelineRevampScreen(
                uiState = uiState,
                mediaCameraUploadUiState = MediaCameraUploadUiState(),
                showEnableCameraUploadsPage = false,
                onVisibleRangeChanged = { _, _ -> },
                onGridSizeChange = {},
                onZoomIn = {},
                onZoomOut = {},
                onMediaTimePeriodSelected = {},
                onNodeClicked = {},
                onNodeSelected = {},
                onScrollingChanged = {},
                selectedPhotoIds = emptySet(),
                onTakenDownDialogEventConsumed = {},
                clearCameraUploadsCompletedMessage = {},
                onNavigateToCameraUploadsSettings = {},
                onNavigateToMobileDataSettings = {},
                onNavigateToUpgradeAccount = {},
                onCameraUploadsBannerDismiss = {},
                handleCameraUploadsPermissionsResult = {},
                handleNotificationPermissionResult = {},
            )
        }
    }
}
