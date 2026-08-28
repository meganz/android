package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.model.MediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.PHOTOS_NODE_BODY_IMAGE_NODE_TAG
import mega.privacy.android.feature.photos.presentation.component.PHOTOS_NODE_BODY_SHIMMER_TAG
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.mobile.analytics.event.MediaScreenDateHeaderSelectAllPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenDragToSelectStartedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineRevampScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private companion object {
        const val MAY_HEADER_CHECKBOX_TAG = "${TIMELINE_REVAMP_SECTION_HEADER_CHECKBOX_TAG}2026-5"
    }

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
    fun `test that the grid size selector tap area spans the header without growing it`() {
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

        val icon = composeRule.onNodeWithTag(TIMELINE_REVAMP_GRID_SIZE_ICON_TAG)
            .getUnclippedBoundsInRoot()
        val header = composeRule.onNodeWithTag(TIMELINE_REVAMP_NON_STICKY_HEADER_TAG)
            .getUnclippedBoundsInRoot()

        // The action grows its touch target into the header insets, so the header keeps its height.
        assertThat(header.height).isEqualTo(60.dp)
        assertThat(icon.width).isEqualTo(64.dp)
        assertThat(icon.height).isEqualTo(40.dp)
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

    @Test
    fun `test that drag selection extends over the media range when dragging after a long press`() {
        val selectedIds = mutableStateSetOf<Long>()
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 3,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = (0..2).associateWith { index -> photoNode(id = index + 1L) },
            ),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { node ->
                if (node.id in selectedIds) selectedIds.remove(node.id) else selectedIds.add(node.id)
            },
        )

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG)
            .onFirst()
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(width.toFloat() * 2, 0f))
                up()
            }

        assertThat(selectedIds).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `test that a pinch on the grid zooms in`() {
        var zoomIns = 0
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 3,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = (0..2).associateWith { index -> photoNode(id = index + 1L) },
            ),
            onZoomIn = { zoomIns++ },
        )

        // 1.55x the starting spread: past one step's ratio, short of two.
        composeRule.onNodeWithTag(TIMELINE_REVAMP_CONTENT_GRID_TAG).performTouchInput {
            spreadFingers(from = 40f, to = 62f)
        }

        assertThat(zoomIns).isEqualTo(1)
    }

    @Test
    fun `test that a pinch does not zoom when a drag selection is already active`() {
        val selectedIds = mutableStateSetOf<Long>()
        var zoomIns = 0
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 30,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = (0..29).associateWith { index -> photoNode(id = index + 1L) },
            ),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { node -> selectedIds.toggle(node.id) },
            onZoomIn = { zoomIns++ },
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_CONTENT_GRID_TAG).performTouchInput {
            val anchor = Offset(width * 0.2f, height * 0.5f)
            down(0, anchor)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            updatePointerTo(0, anchor + Offset(0f, 40f))
            move()
            // Second finger joins mid-drag-select and spreads far enough to have stepped the grid
            // twice; drag-to-select owns the gesture, so the pinch must stay out of it.
            down(1, anchor + Offset(40f, 0f))
            repeat(10) { step ->
                updatePointerTo(1, anchor + Offset(40f + 18f * (step + 1), 0f))
                move()
            }
            up(0)
            up(1)
        }

        assertThat(zoomIns).isEqualTo(0)
    }

    @Test
    fun `test that drag selection tracks MediaScreenDragToSelectStartedEvent once per gesture`() {
        val selectedIds = mutableStateSetOf<Long>()
        composeRule.setScreen(
            TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 3,
                    ),
                ),
                sectionStartOffsets = listOf(0),
                loadedNodes = (0..2).associateWith { index -> photoNode(id = index + 1L) },
            ),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { node ->
                if (node.id in selectedIds) selectedIds.remove(node.id) else selectedIds.add(node.id)
            },
        )

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG)
            .onFirst()
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(width.toFloat(), 0f))
                moveBy(Offset(width.toFloat(), 0f))
                up()
            }

        composeRule.runOnIdle {
            assertThat(analyticsRule.events).containsExactly(MediaScreenDragToSelectStartedEvent)
        }
    }

    @Test
    fun `test that swept cells are selected once their nodes are lazily loaded`() {
        val selectedIds = mutableStateSetOf<Long>()
        val initialState = TimelineRevampUiState.Data(
            sections = listOf(
                MediaTimelineSection(
                    groupId = "2026-06-15",
                    startDate = 1_781_481_600L,
                    endDate = 1_781_481_600L,
                    count = 3,
                ),
            ),
            sectionStartOffsets = listOf(0),
            loadedNodes = mapOf(0 to photoNode(id = 1L)),
        )
        var uiState by mutableStateOf(initialState)
        composeRule.setScreenContent(
            uiState = { uiState },
            selectedPhotoIds = selectedIds,
            onNodeSelected = { node ->
                if (node.id in selectedIds) selectedIds.remove(node.id) else selectedIds.add(node.id)
            },
        )

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG)
            .onFirst()
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(width.toFloat() * 2, 0f))
                up()
            }

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L)
        }
        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_SHIMMER_TAG).assertAll(isSelected())

        composeRule.runOnIdle {
            uiState = initialState.copy(
                loadedNodes = (0..2).associateWith { index -> photoNode(id = index + 1L) },
            )
        }

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 2L, 3L)
        }
    }

    @Test
    fun `test that the month header shows a select-all checkbox only in selection mode`() {
        val selectedIds = mutableStateSetOf<Long>()
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
        )

        composeRule.onAllNodesWithTag(MAY_HEADER_CHECKBOX_TAG, useUnmergedTree = true)
            .assertCountEquals(0)

        composeRule.runOnIdle { selectedIds.add(1L) }

        mayHeaderCheckbox().assertIsDisplayed()
        topHeaderCheckbox().assertIsDisplayed()
    }

    @Test
    fun `test that clicking the month header checkbox selects every photo in that month`() {
        val selectedIds = mutableStateSetOf(1L)
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
        )

        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 2L, 3L)
        }
    }

    @Test
    fun `test that clicking the month header checkbox tracks MediaScreenDateHeaderSelectAllPressedEvent`() {
        val selectedIds = mutableStateSetOf(1L)
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
        )

        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(analyticsRule.events)
                .containsExactly(MediaScreenDateHeaderSelectAllPressedEvent)
        }
    }

    @Test
    fun `test that the month header checkbox is checked when all photos in the month are selected and clicking it deselects them`() {
        val selectedIds = mutableStateSetOf(2L)
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
        )

        mayHeaderCheckbox().assertIsOff()

        composeRule.runOnIdle { selectedIds.add(3L) }

        mayHeaderCheckbox().assertIsOn()

        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(selectedIds).isEmpty()
        }
    }

    @Test
    fun `test that clicking the month header row toggles that month's selection in selection mode`() {
        val selectedIds = mutableStateSetOf<Long>()
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
        )
        val mayHeader = composeRule.onNodeWithTag("${TIMELINE_REVAMP_SECTION_HEADER_TAG}2026-5")

        mayHeader.performClick()
        composeRule.runOnIdle {
            assertThat(selectedIds).isEmpty()
        }

        composeRule.runOnIdle { selectedIds.add(1L) }

        mayHeader.performClick()
        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 2L, 3L)
        }

        mayHeader.performClick()
        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L)
        }
    }

    @Test
    fun `test that the top header checkbox selects the month at the top of the grid`() {
        val selectedIds = mutableStateSetOf(3L)
        composeRule.setScreen(
            uiState = twoMonthsUiState(),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
        )

        topHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 3L)
        }
    }

    @Test
    fun `test that month select-all loads the unloaded photos and selects them from the returned items`() {
        val selectedIds = mutableStateSetOf(1L)
        val loadRequests = mutableListOf<IntRange>()
        val loadGate = CompletableDeferred<Unit>()
        // June: 1 photo (loaded, id 1). May: 3 photos across two day sections, only the first
        // loaded (id 2); global indexes 2 and 3 are not loaded yet.
        composeRule.setScreen(
            uiState = TimelineRevampUiState.Data(
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
                        count = 2,
                    ),
                    MediaTimelineSection(
                        groupId = "2026-05-10",
                        startDate = 1_778_371_200L,
                        endDate = 1_778_371_200L,
                        count = 1,
                    ),
                ),
                sectionStartOffsets = listOf(0, 1, 3),
                loadedNodes = mapOf(0 to photoNode(id = 1L), 1 to photoNode(id = 2L)),
            ),
            selectedPhotoIds = selectedIds,
            onNodeSelected = { selectedIds.toggle(it.id) },
            loadMediaRange = { first, last ->
                loadRequests.add(first..last)
                loadGate.await()
                (first..last).associateWith { index -> photoNode(id = index + 1L) }
            },
        )

        mayHeaderCheckbox().performClick()

        // While the load is in flight, the loaded photo is selected and the unloaded cells render
        // as selected placeholders.
        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 2L)
            assertThat(loadRequests).containsExactly(1..3)
        }
        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_SHIMMER_TAG).assertAll(isSelected())

        loadGate.complete(Unit)

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L, 2L, 3L, 4L)
        }

        // Deselecting resolves the ids of photos that are not in the loaded window the same way.
        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(selectedIds).containsExactly(1L)
        }
    }

    /**
     * June 2026 with one photo (id 1) followed by two May 2026 day sections with one photo each
     * (ids 2 and 3), all loaded — May gets an inline month header, June is represented by the top
     * header.
     */
    private fun twoMonthsUiState() = TimelineRevampUiState.Data(
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
        loadedNodes = (0..2).associateWith { index -> photoNode(id = index + 1L) },
    )

    private fun photoNode(id: Long) = PhotosNodeContentItemV2(
        key = id,
        contentType = PhotosNodeContentType.PhotoNode,
        id = id,
        mediaType = MediaType.Image,
        day = 15,
        month = 6,
        year = 2026,
        fullModificationTime = 1_781_481_600L,
        thumbnailFilePath = null,
        previewFilePath = null,
        extension = "",
        isFavourite = false,
        isSensitive = false,
    )

    // The checkbox is not clickable itself (the header row is), so its nodes only exist in the
    // unmerged tree; the tag lands on several inner layers, hence onFirst.
    private fun mayHeaderCheckbox() = composeRule
        .onAllNodesWithTag(MAY_HEADER_CHECKBOX_TAG, useUnmergedTree = true)
        .onFirst()

    private fun topHeaderCheckbox() = composeRule
        .onAllNodesWithTag(TIMELINE_REVAMP_NON_STICKY_HEADER_CHECKBOX_TAG, useUnmergedTree = true)
        .onFirst()

    private fun MutableSet<Long>.toggle(id: Long) {
        if (id in this) remove(id) else add(id)
    }

    /** Spreads two horizontally-opposed pointers from [from] to [to] pixels either side of centre. */
    private fun TouchInjectionScope.spreadFingers(from: Float, to: Float) {
        down(0, center + Offset(-from, 0f))
        down(1, center + Offset(from, 0f))
        repeat(10) { step ->
            val spread = from + (to - from) * (step + 1) / 10
            updatePointerTo(0, center + Offset(-spread, 0f))
            updatePointerTo(1, center + Offset(spread, 0f))
            move()
        }
        up(0)
        up(1)
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: TimelineRevampUiState,
        selectedPhotoIds: Set<Long> = emptySet(),
        onNodeSelected: (PhotosNodeContentItemV2) -> Unit = {},
        loadMediaRange: suspend (firstIndex: Int, lastIndex: Int) -> Map<Int, PhotosNodeContentItemV2> =
            { _, _ -> emptyMap() },
        onZoomIn: () -> Unit = {},
        onPinchActiveChanged: (Boolean) -> Unit = {},
    ) = setScreenContent(
        { uiState },
        selectedPhotoIds,
        onNodeSelected,
        loadMediaRange,
        onZoomIn,
        onPinchActiveChanged,
    )

    private fun ComposeContentTestRule.setScreenContent(
        uiState: () -> TimelineRevampUiState,
        selectedPhotoIds: Set<Long> = emptySet(),
        onNodeSelected: (PhotosNodeContentItemV2) -> Unit = {},
        loadMediaRange: suspend (firstIndex: Int, lastIndex: Int) -> Map<Int, PhotosNodeContentItemV2> =
            { _, _ -> emptyMap() },
        onZoomIn: () -> Unit = {},
        onPinchActiveChanged: (Boolean) -> Unit = {},
    ) {
        setContent {
            TimelineRevampScreen(
                uiState = uiState(),
                mediaCameraUploadUiState = MediaCameraUploadUiState(),
                showEnableCameraUploadsPage = false,
                onVisibleRangeChanged = { _, _ -> },
                loadMediaRange = loadMediaRange,
                onGridSizeChange = {},
                onZoomIn = onZoomIn,
                onZoomOut = {},
                onPinchActiveChanged = onPinchActiveChanged,
                onMediaTimePeriodSelected = {},
                onNodeClicked = { _, _ -> },
                onNodeSelected = onNodeSelected,
                onScrollingChanged = {},
                selectedPhotoIds = selectedPhotoIds,
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
