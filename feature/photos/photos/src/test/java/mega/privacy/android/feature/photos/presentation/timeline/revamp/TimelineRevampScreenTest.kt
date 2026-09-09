package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
        val longPressed = mutableListOf<Long>()
        val cellChanges = mutableListOf<Pair<Int, Boolean>>()
        var dragEnds = 0
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
            onNodeSelected = { longPressed += it.id },
            onCellSelectionChange = { index, selected -> cellChanges += index to selected },
            onDragSelectionEnded = { dragEnds++ },
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
            assertThat(longPressed).containsExactly(1L)
            assertThat(cellChanges.distinct()).containsExactly(0 to true, 1 to true, 2 to true)
            assertThat(dragEnds).isEqualTo(1)
        }
    }

    @Test
    fun `test that a drag anchored on a checked placeholder reports the swept cells as deselected`() {
        val cellChanges = mutableListOf<Pair<Int, Boolean>>()
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
                loadedNodes = mapOf(0 to photoNode(id = 1L)),
                selection = TimelineSelection(
                    selectedIds = setOf(1L),
                    pendingIndices = setOf(1, 2),
                    count = 3,
                ),
            ),
            onCellSelectionChange = { index, selected -> cellChanges += index to selected },
        )

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_SHIMMER_TAG).assertAll(isSelected())

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_SHIMMER_TAG)
            .onFirst()
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(width.toFloat(), 0f))
                up()
            }

        composeRule.runOnIdle {
            assertThat(cellChanges.distinct()).containsExactly(1 to false, 2 to false)
        }
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
    fun `test that a pinch on the grid does not zoom when pinch to zoom is disabled`() {
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
            isPinchToZoomEnabled = false,
        )

        composeRule.onNodeWithTag(TIMELINE_REVAMP_CONTENT_GRID_TAG).performTouchInput {
            spreadFingers(from = 40f, to = 62f)
        }

        assertThat(zoomIns).isEqualTo(0)
    }

    @Test
    fun `test that a pinch does not zoom when a drag selection is already active`() {
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
    fun `test that unloaded cells with a pending mark render as selected placeholders`() {
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
                loadedNodes = mapOf(0 to photoNode(id = 1L)),
                selection = TimelineSelection(pendingIndices = setOf(1, 2), count = 2),
            ),
        )

        composeRule.onAllNodesWithTag(PHOTOS_NODE_BODY_SHIMMER_TAG)
            .assertCountEquals(2)
            .assertAll(isSelected())
    }

    @Test
    fun `test that the month header shows a select-all checkbox only in selection mode`() {
        var uiState by mutableStateOf(twoMonthsUiState())
        composeRule.setScreenContent(uiState = { uiState })

        composeRule.onAllNodesWithTag(MAY_HEADER_CHECKBOX_TAG, useUnmergedTree = true)
            .assertCountEquals(0)

        composeRule.runOnIdle { uiState = twoMonthsUiState(selection = selectionOf(1L)) }

        mayHeaderCheckbox().assertIsDisplayed()
        topHeaderCheckbox().assertIsDisplayed()
    }

    @Test
    fun `test that clicking the month header checkbox toggles the selection of that month's range`() {
        val toggledRanges = mutableListOf<IntRange>()
        composeRule.setScreen(
            uiState = twoMonthsUiState(selection = selectionOf(1L)),
            onRangeSelectionToggle = { toggledRanges += it },
        )

        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(toggledRanges).containsExactly(1..2)
        }
    }

    @Test
    fun `test that clicking the month header checkbox tracks MediaScreenDateHeaderSelectAllPressedEvent`() {
        composeRule.setScreen(uiState = twoMonthsUiState(selection = selectionOf(1L)))

        mayHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(analyticsRule.events)
                .containsExactly(MediaScreenDateHeaderSelectAllPressedEvent)
        }
    }

    @Test
    fun `test that the month header checkbox is checked only when every photo in the month is selected`() {
        var uiState by mutableStateOf(twoMonthsUiState(selection = selectionOf(2L)))
        composeRule.setScreenContent(uiState = { uiState })

        mayHeaderCheckbox().assertIsOff()

        composeRule.runOnIdle { uiState = twoMonthsUiState(selection = selectionOf(2L, 3L)) }

        mayHeaderCheckbox().assertIsOn()
    }

    @Test
    fun `test that a pending placeholder counts towards the month header checkbox`() {
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
                ),
                sectionStartOffsets = listOf(0, 1),
                loadedNodes = mapOf(0 to photoNode(id = 1L), 1 to photoNode(id = 2L)),
                selection = TimelineSelection(
                    selectedIds = setOf(2L),
                    pendingIndices = setOf(2),
                    count = 2,
                ),
            ),
        )

        mayHeaderCheckbox().assertIsOn()
    }

    @Test
    fun `test that clicking the month header row toggles that month's range only in selection mode`() {
        val toggledRanges = mutableListOf<IntRange>()
        var uiState by mutableStateOf(twoMonthsUiState())
        composeRule.setScreenContent(
            uiState = { uiState },
            onRangeSelectionToggle = { toggledRanges += it },
        )
        val mayHeader = composeRule.onNodeWithTag("${TIMELINE_REVAMP_SECTION_HEADER_TAG}2026-5")

        mayHeader.performClick()
        composeRule.runOnIdle {
            assertThat(toggledRanges).isEmpty()
        }

        composeRule.runOnIdle { uiState = twoMonthsUiState(selection = selectionOf(1L)) }

        mayHeader.performClick()
        composeRule.runOnIdle {
            assertThat(toggledRanges).containsExactly(1..2)
        }
    }

    @Test
    fun `test that the top header checkbox toggles the month at the top of the grid`() {
        val toggledRanges = mutableListOf<IntRange>()
        composeRule.setScreen(
            uiState = twoMonthsUiState(selection = selectionOf(3L)),
            onRangeSelectionToggle = { toggledRanges += it },
        )

        topHeaderCheckbox().performClick()

        composeRule.runOnIdle {
            assertThat(toggledRanges).containsExactly(0..0)
        }
    }

    private fun selectionOf(vararg ids: Long) = TimelineSelection(
        selectedIds = ids.toSet(),
        count = ids.size,
    )

    /**
     * June 2026 with one photo (id 1) followed by two May 2026 day sections with one photo each
     * (ids 2 and 3), all loaded — May gets an inline month header, June is represented by the top
     * header.
     */
    private fun twoMonthsUiState(
        selection: TimelineSelection = TimelineSelection(),
    ) = TimelineRevampUiState.Data(
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
        selection = selection,
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
        onNodeSelected: (PhotosNodeContentItemV2) -> Unit = {},
        onCellSelectionChange: (index: Int, selected: Boolean) -> Unit = { _, _ -> },
        onRangeSelectionToggle: (IntRange) -> Unit = {},
        onDragSelectionEnded: () -> Unit = {},
        onZoomIn: () -> Unit = {},
        onPinchActiveChanged: (Boolean) -> Unit = {},
        isPinchToZoomEnabled: Boolean = true,
    ) = setScreenContent(
        { uiState },
        onNodeSelected,
        onCellSelectionChange,
        onRangeSelectionToggle,
        onDragSelectionEnded,
        onZoomIn,
        onPinchActiveChanged,
        isPinchToZoomEnabled,
    )

    private fun ComposeContentTestRule.setScreenContent(
        uiState: () -> TimelineRevampUiState,
        onNodeSelected: (PhotosNodeContentItemV2) -> Unit = {},
        onCellSelectionChange: (index: Int, selected: Boolean) -> Unit = { _, _ -> },
        onRangeSelectionToggle: (IntRange) -> Unit = {},
        onDragSelectionEnded: () -> Unit = {},
        onZoomIn: () -> Unit = {},
        onPinchActiveChanged: (Boolean) -> Unit = {},
        isPinchToZoomEnabled: Boolean = true,
    ) {
        setContent {
            TimelineRevampScreen(
                uiState = uiState(),
                mediaCameraUploadUiState = MediaCameraUploadUiState(),
                showEnableCameraUploadsPage = false,
                onVisibleRangeChanged = { _, _ -> },
                onGridSizeChange = {},
                onZoomIn = onZoomIn,
                onZoomOut = {},
                onPinchActiveChanged = onPinchActiveChanged,
                isPinchToZoomEnabled = isPinchToZoomEnabled,
                onMediaTimePeriodSelected = {},
                onNodeClicked = { _, _ -> },
                onNodeSelected = onNodeSelected,
                onCellSelectionChange = onCellSelectionChange,
                onRangeSelectionToggle = onRangeSelectionToggle,
                onDragSelectionEnded = onDragSelectionEnded,
                onScrollingChanged = {},
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
