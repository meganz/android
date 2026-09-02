package mega.privacy.android.feature.photos.presentation.timeline.revamp

import android.content.res.Configuration
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.HeaderHorizontalInset
import mega.privacy.android.feature.photos.components.HeaderVerticalInset
import mega.privacy.android.feature.photos.components.StickySectionHeader
import mega.privacy.android.feature.photos.components.TimelineGridSizeSettingsMenu
import mega.privacy.android.feature.photos.extensions.isScrolledToEnd
import mega.privacy.android.feature.photos.extensions.isScrolledToTop
import mega.privacy.android.feature.photos.extensions.isScrollingDown
import mega.privacy.android.feature.photos.extensions.photosGridDragToSelectGesture
import mega.privacy.android.feature.photos.extensions.photosZoomGestureDetector
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.PhotoNodeBodyV2
import mega.privacy.android.feature.photos.presentation.timeline.TimelineDateCache
import mega.privacy.android.feature.photos.presentation.timeline.component.CameraUploadsBanner
import mega.privacy.android.feature.photos.presentation.timeline.component.EnableCameraUploadsContent
import mega.privacy.android.feature.photos.presentation.timeline.component.MediaSkeletonView
import mega.privacy.android.feature.photos.presentation.timeline.component.PeriodCardsSkeletonView
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosNodeListCardListView
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.feature.photos.presentation.timeline.rememberCameraUploadsBannerHandlers
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.nodes.dialog.TakeDownDialog
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MediaScreenDateHeaderSelectAllPressedEvent
import mega.privacy.mobile.analytics.event.MediaScreenDragToSelectStartedEvent
import timber.log.Timber
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TimelineRevampScreen(
    uiState: TimelineRevampUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    showEnableCameraUploadsPage: Boolean,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    loadMediaRange: suspend (firstIndex: Int, lastIndex: Int) -> Map<Int, PhotosNodeContentItemV2>,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    onNodeClicked: (PhotosNodeContentItemV2?, Int) -> Unit,
    onNodeSelected: (PhotosNodeContentItemV2) -> Unit,
    onScrollingChanged: (Boolean) -> Unit,
    selectedPhotoIds: Set<Long>,
    onTakenDownDialogEventConsumed: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateToMobileDataSettings: () -> Unit,
    onNavigateToUpgradeAccount: () -> Unit,
    onCameraUploadsBannerDismiss: (status: CUStatusUiState) -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onSelectorVisibleChanged: (Boolean) -> Unit = {},
    onPinchActiveChanged: (Boolean) -> Unit = {},
    onPendingSelectionCountChanged: (Int) -> Unit = {},
) {
    var showTakenDownDialog by rememberSaveable { mutableStateOf(false) }
    val takenDownDialogEvent =
        (uiState as? TimelineRevampUiState.Data)?.takenDownDialogEvent ?: consumed
    EventEffect(event = takenDownDialogEvent, onConsumed = onTakenDownDialogEventConsumed) {
        showTakenDownDialog = true
    }

    val cameraUploadsBannerHandlers = rememberCameraUploadsBannerHandlers(
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
        handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
        handleNotificationPermissionResult = handleNotificationPermissionResult,
    )

    val layoutDirection = LocalLayoutDirection.current
    val startInset = contentPadding.calculateStartPadding(layoutDirection)
    val endInset = contentPadding.calculateEndPadding(layoutDirection)
    val insetModifier = modifier.padding(start = startInset, end = endInset)

    when {
        showEnableCameraUploadsPage -> {
            EnableCameraUploadsContent(
                modifier = insetModifier
                    .padding(horizontal = 16.dp)
                    .testTag(TIMELINE_REVAMP_ENABLE_CU_CONTENT_TAG),
                onEnable = onNavigateToCameraUploadsSettings,
            )
        }

        uiState is TimelineRevampUiState.Loading -> {
            MediaSkeletonView(
                modifier = insetModifier.testTag(TIMELINE_REVAMP_LOADING_SKELETON_TAG),
            )
        }

        uiState is TimelineRevampUiState.Empty -> {
            EmptyStateView(
                modifier = Modifier
                    .padding(start = startInset, end = endInset)
                    .testTag(TIMELINE_REVAMP_EMPTY_VIEW_TAG),
                imagePainter = painterResource(R.drawable.il_glass_image),
                title = stringResource(sharedR.string.timeline_tab_empty_body_no_media_found)
            )
        }

        uiState is TimelineRevampUiState.Data -> {
            TimelineRevampContent(
                modifier = insetModifier.fillMaxSize(),
                contentPadding = contentPadding,
                sections = uiState.sections,
                sectionStartOffsets = uiState.sectionStartOffsets,
                loadedNodes = uiState.loadedNodes,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                gridSize = uiState.gridSize,
                selectedPeriod = uiState.selectedPeriod,
                periodCards = uiState.periodCards,
                arePeriodCardsLoading = uiState.arePeriodCardsLoading,
                onVisibleRangeChanged = onVisibleRangeChanged,
                loadMediaRange = loadMediaRange,
                onGridSizeChange = onGridSizeChange,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onPinchActiveChanged = onPinchActiveChanged,
                onMediaTimePeriodSelected = onMediaTimePeriodSelected,
                onNodeClicked = onNodeClicked,
                onNodeSelected = onNodeSelected,
                onScrollingChanged = onScrollingChanged,
                onSelectorVisibleChanged = onSelectorVisibleChanged,
                onPendingSelectionCountChanged = onPendingSelectionCountChanged,
                selectedPhotoIds = selectedPhotoIds,
                bannerContent = if (selectedPhotoIds.isEmpty()) {
                    {
                        CameraUploadsBanner(
                            status = mediaCameraUploadUiState.status,
                            onEnableCameraUploads = onNavigateToCameraUploadsSettings,
                            onDismissRequest = onCameraUploadsBannerDismiss,
                            onChangeCameraUploadsPermissions =
                                cameraUploadsBannerHandlers.onChangeCameraUploadsPermissions,
                            onRequestNotificationPermission =
                                cameraUploadsBannerHandlers.onRequestNotificationPermission,
                            onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                            onNavigateMobileDataSetting = onNavigateToMobileDataSettings,
                            onNavigateUpgradeScreen = onNavigateToUpgradeAccount,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    if (showTakenDownDialog) {
        TakeDownDialog(
            isFolder = false,
            onDismiss = { showTakenDownDialog = false },
        )
    }
}

@Composable
private fun TimelineRevampContent(
    sections: List<MediaTimelineSection>,
    sectionStartOffsets: List<Int>,
    loadedNodes: Map<Int, PhotosNodeContentItemV2>,
    isHiddenNodesEnabled: Boolean,
    gridSize: TimelineGridSize,
    selectedPeriod: MediaTimePeriod,
    periodCards: List<PhotosNodeListCard>,
    arePeriodCardsLoading: Boolean,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    loadMediaRange: suspend (firstIndex: Int, lastIndex: Int) -> Map<Int, PhotosNodeContentItemV2>,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPinchActiveChanged: (Boolean) -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    onNodeClicked: (PhotosNodeContentItemV2?, Int) -> Unit,
    onNodeSelected: (PhotosNodeContentItemV2) -> Unit,
    onScrollingChanged: (Boolean) -> Unit,
    onSelectorVisibleChanged: (Boolean) -> Unit,
    onPendingSelectionCountChanged: (Int) -> Unit,
    selectedPhotoIds: Set<Long>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    bannerContent: (@Composable () -> Unit)? = null,
) {
    val lazyGridState = rememberLazyGridState()
    val cardListState = rememberLazyListState()
    val configuration = LocalConfiguration.current

    val contentPaddingWithSelector = PaddingValues(
        top = contentPadding.calculateTopPadding(),
        bottom = contentPadding.calculateBottomPadding() + TIMELINE_REVAMP_SELECTOR_CLEARANCE,
    )
    val columns =
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridSize.portrait
        } else {
            gridSize.landscape
        }
    val locale = LocalLocale.current.platformLocale

    var pendingScroll by remember { mutableStateOf<PendingCardScroll?>(null) }

    // Year card picker effect
    LaunchedEffect(selectedPeriod, periodCards, pendingScroll) {
        val target = pendingScroll
        if (target is PendingCardScroll.ToYear &&
            selectedPeriod == MediaTimePeriod.Months &&
            periodCards.isNotEmpty()
        ) {
            val cardIndex = periodCards.indexOfFirst { it.year == target.year }
            if (cardIndex >= 0) cardListState.scrollToItem(cardIndex)
            pendingScroll = null
        }
    }

    // Month card picker effect
    LaunchedEffect(selectedPeriod, sections, pendingScroll) {
        val target = pendingScroll
        if (target is PendingCardScroll.ToMonth &&
            selectedPeriod.isGridPeriod() &&
            sections.isNotEmpty()
        ) {
            val sectionIndex = sections.indexOfFirst { it.isInMonth(target.year, target.month) }
            sectionStartOffsets.getOrNull(sectionIndex)?.let { offset ->
                lazyGridState.scrollToItem(sectionIndex + offset)
            }
            pendingScroll = null
        }
    }
    val selectorVisible by rememberSelectorVisibility(selectedPeriod, lazyGridState, cardListState)

    LaunchedEffect(Unit) {
        snapshotFlow { selectorVisible }
            .distinctUntilChanged()
            .collectLatest { visible ->
                if (!visible) delay(SELECTOR_HIDE_DEBOUNCE_MS)
                onSelectorVisibleChanged(visible)
            }
    }

    when (selectedPeriod) {
        MediaTimePeriod.Years, MediaTimePeriod.Months -> {
            if (arePeriodCardsLoading) {
                PeriodCardsSkeletonView(
                    modifier = modifier
                        .fillMaxSize()
                        .testTag(TIMELINE_REVAMP_CARD_LIST_SKELETON_TAG),
                    contentPadding = contentPaddingWithSelector,
                )
            } else {
                PhotosNodeListCardListView(
                    modifier = modifier
                        .fillMaxSize()
                        .testTag(TIMELINE_REVAMP_CARD_LIST_TAG),
                    photos = periodCards,
                    isHiddenNodesEnabled = isHiddenNodesEnabled,
                    state = cardListState,
                    contentPadding = contentPaddingWithSelector,
                    onClick = { card ->
                        when (card.period) {
                            PhotosNodeListCardPeriod.Year -> {
                                pendingScroll = PendingCardScroll.ToYear(card.year)
                                onMediaTimePeriodSelected(MediaTimePeriod.Months)
                            }

                            PhotosNodeListCardPeriod.Month -> {
                                pendingScroll = PendingCardScroll.ToMonth(card.year, card.month)
                                onMediaTimePeriodSelected(MediaTimePeriod.All)
                            }

                            PhotosNodeListCardPeriod.Day -> Unit
                        }
                    },
                )
            }
        }

        else -> {
            TimelineRevampGrid(
                sections = sections,
                sectionStartOffsets = sectionStartOffsets,
                loadedNodes = loadedNodes,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                gridSize = gridSize,
                columns = columns,
                locale = locale,
                lazyGridState = lazyGridState,
                onVisibleRangeChanged = onVisibleRangeChanged,
                loadMediaRange = loadMediaRange,
                onGridSizeChange = onGridSizeChange,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onPinchActiveChanged = onPinchActiveChanged,
                onNodeClicked = onNodeClicked,
                onNodeSelected = onNodeSelected,
                onScrollingChanged = onScrollingChanged,
                onPendingSelectionCountChanged = onPendingSelectionCountChanged,
                selectedPhotoIds = selectedPhotoIds,
                contentPadding = contentPaddingWithSelector,
                bannerContent = bannerContent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TimelineRevampGrid(
    sections: List<MediaTimelineSection>,
    sectionStartOffsets: List<Int>,
    loadedNodes: Map<Int, PhotosNodeContentItemV2>,
    isHiddenNodesEnabled: Boolean,
    gridSize: TimelineGridSize,
    columns: Int,
    locale: Locale,
    lazyGridState: LazyGridState,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    loadMediaRange: suspend (firstIndex: Int, lastIndex: Int) -> Map<Int, PhotosNodeContentItemV2>,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPinchActiveChanged: (Boolean) -> Unit,
    onNodeClicked: (PhotosNodeContentItemV2?, Int) -> Unit,
    onNodeSelected: (PhotosNodeContentItemV2) -> Unit,
    onScrollingChanged: (Boolean) -> Unit,
    onPendingSelectionCountChanged: (Int) -> Unit,
    selectedPhotoIds: Set<Long>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    bannerContent: (@Composable () -> Unit)? = null,
) {
    // Maps a section's groupId to its start offset, to recover a visible item's global index from its
    // section-relative key (see mediaKey).
    val offsetByGroupId = remember(sections, sectionStartOffsets) {
        sections.zip(sectionStartOffsets).associate { (section, offset) ->
            section.groupId to offset
        }
    }

    // The drag-to-select gesture keeps the lambdas it captures when the pointer input starts, so
    // they read the latest data through rememberUpdatedState instead of capturing it directly.
    // Drag-selecting a cell fires the same onNodeSelected event as long-pressing it; the live
    // selection makes that toggle idempotent — cells already in the target state are skipped.
    val currentOffsetByGroupId by rememberUpdatedState(offsetByGroupId)
    val currentLoadedNodes by rememberUpdatedState(loadedNodes)
    val currentSelectedPhotoIds by rememberUpdatedState(selectedPhotoIds)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)
    val currentLoadMediaRange by rememberUpdatedState(loadMediaRange)

    // Cells marked for selection while their node is still loading; their shimmer placeholders
    // render as selected and they join the selection once the node arrives. The value is the node
    // once a range fetch resolves the cell (its id is then in selectedPhotoIds), null while the
    // cell is selected but unresolved — so unresolved entries are exactly what the selection count
    // must add on top of selectedPhotoIds without double counting.
    val pendingSelection = remember { mutableStateMapOf<Int, PhotosNodeContentItemV2?>() }

    // Removes a cell's pending mark when it is deselected. An entry a range fetch resolved holds
    // its node and its id is already in the real selection, so toggle it back out too.
    fun unmarkPendingSelection(index: Int) {
        pendingSelection.remove(index)?.let { resolved ->
            if (resolved.id in currentSelectedPhotoIds) currentOnNodeSelected(resolved)
        }
    }

    LaunchedEffect(loadedNodes) {
        pendingSelection.keys.toList().forEach { index ->
            val node = loadedNodes[index] ?: return@forEach
            pendingSelection.remove(index)
            node.takeUnless { it.isTakenDown }
                ?.takeIf { it.id !in selectedPhotoIds }
                ?.let(onNodeSelected)
        }
    }

    LaunchedEffect(selectedPhotoIds.isEmpty()) {
        if (selectedPhotoIds.isEmpty()) pendingSelection.clear()
    }

    val currentOnPendingSelectionCountChanged by rememberUpdatedState(onPendingSelectionCountChanged)
    LaunchedEffect(Unit) {
        snapshotFlow { pendingSelection.count { it.value == null } }
            .collect { currentOnPendingSelectionCountChanged(it) }
    }
    DisposableEffect(Unit) {
        onDispose { currentOnPendingSelectionCountChanged(0) }
    }

    NotifyVisibleMediaRange(
        gridState = lazyGridState,
        offsetByGroupId = offsetByGroupId,
        onVisibleRangeChanged = onVisibleRangeChanged,
    )

    ReportScrollInProgress(
        gridState = lazyGridState,
        onScrollingChanged = onScrollingChanged,
    )

    val headerIndexes = remember(sections) {
        val seenMonths = HashSet<String>()
        buildSet {
            sections.forEachIndexed { index, section ->
                val monthAdded = seenMonths.add(monthKey(section.startDate))
                if (monthAdded && index != 0) {
                    add(index)
                }
            }
        }
    }
    val totalGridItems = remember(sections, headerIndexes, bannerContent != null) {
        val bannerItems = if (bannerContent != null) 1 else 0
        bannerItems + 1 + headerIndexes.size + sections.sumOf { it.count }.toInt()
    }

    val isSelectionMode = selectedPhotoIds.isNotEmpty()
    val scope = rememberCoroutineScope()

    // Global media-index range of each month (keyed by monthKey), backing the month headers'
    // select-all checkboxes.
    val monthRanges = remember(sections, sectionStartOffsets) {
        buildMap {
            var rangeStart = 0
            var rangeEnd = -1
            var currentMonth: String? = null
            sections.zip(sectionStartOffsets).forEach { (section, offset) ->
                val month = monthKey(section.startDate)
                if (month != currentMonth) {
                    currentMonth?.let { put(it, rangeStart..rangeEnd) }
                    currentMonth = month
                    rangeStart = offset
                }
                rangeEnd = offset + section.count.toInt() - 1
            }
            currentMonth?.let { put(it, rangeStart..rangeEnd) }
        }
    }

    // A range is fully selected when every cell is selected, pending selection, or taken down.
    fun isRangeSelected(range: IntRange): Boolean = range.all { index ->
        val node = loadedNodes[index]
        if (node == null) index in pendingSelection
        else node.isTakenDown || node.id in currentSelectedPhotoIds
    }

    // Applies the same per-node toggle a tap fires across the whole range. Cells not in
    // loadedNodes (which cannot represent a range larger than its cache) resolve through
    // loadMediaRange, marked pending meanwhile when selecting.
    fun toggleRangeSelection(range: IntRange) {
        Analytics.tracker.trackEvent(MediaScreenDateHeaderSelectAllPressedEvent)
        val selectAll = !isRangeSelected(range)
        var hasUnloadedCells = false
        range.forEach { index ->
            val node = loadedNodes[index]
            if (node == null) {
                hasUnloadedCells = true
                if (selectAll) {
                    if (index !in pendingSelection) pendingSelection[index] = null
                } else {
                    unmarkPendingSelection(index)
                }
            } else {
                if (!selectAll) pendingSelection.remove(index)
                node.takeUnless { it.isTakenDown }
                    ?.takeIf { (it.id in currentSelectedPhotoIds) != selectAll }
                    ?.let { currentOnNodeSelected(it) }
            }
        }
        if (hasUnloadedCells) {
            scope.launch {
                try {
                    loadMediaRange(range.first, range.last).forEach { (index, node) ->
                        // Leaving selection mode clears the pending marks; don't revive the selection.
                        if (!selectAll || index in pendingSelection) {
                            if (selectAll) pendingSelection[index] = node
                            node.takeUnless { it.isTakenDown }
                                ?.takeIf { (it.id in currentSelectedPhotoIds) != selectAll }
                                ?.let { currentOnNodeSelected(it) }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to resolve the section selection")
                }
            }
        }
    }

    // Cells drag-swept while unloaded only sit in pendingSelection, invisible to the selection
    // count and any action taken on the selection. Resolve them the way toggleRangeSelection does
    // as soon as the drag ends, instead of waiting for them to scroll into the loading window.
    fun resolvePendingDragSelection() {
        // Span only the unresolved cells: resolved entries linger (their nodes never reach
        // loadedNodes) and would otherwise stretch the fetch across every past drag/select-all.
        val unresolved = pendingSelection.filterValues { it == null }.keys
        val first = unresolved.minOrNull() ?: return
        val last = unresolved.max()
        scope.launch {
            try {
                currentLoadMediaRange(first, last).forEach { (index, node) ->
                    // Leaving selection mode clears the pending marks; don't revive the selection.
                    if (index in pendingSelection) {
                        pendingSelection[index] = node
                        node.takeUnless { it.isTakenDown }
                            ?.takeIf { it.id !in currentSelectedPhotoIds }
                            ?.let(currentOnNodeSelected)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to resolve the drag selection")
            }
        }
    }

    // The month owning the top-most visible media — the month the sticky/top header represents.
    val topVisibleMonthRange by remember(
        sections,
        sectionStartOffsets,
        monthRanges,
        offsetByGroupId
    ) {
        derivedStateOf {
            if (sections.isEmpty()) return@derivedStateOf null
            val minVisibleIndex = lazyGridState.layoutInfo.visibleItemsInfo
                .mapNotNull { globalMediaIndexOf(it.key, offsetByGroupId) }
                .minOrNull() ?: 0
            val sectionIndex = sectionIndexOfMedia(minVisibleIndex, sectionStartOffsets)
                .coerceIn(sections.indices)
            monthRanges[monthKey(sections[sectionIndex].startDate)]
        }
    }

    // Recomputes on scroll (reads lazyGridState.layoutInfo, a snapshot state) and is re-created when
    // the section layout or locale changes (the remember keys).
    val stickyLabel by remember(sections, sectionStartOffsets, offsetByGroupId, locale) {
        derivedStateOf {
            val visibleMediaIndices = lazyGridState.layoutInfo.visibleItemsInfo
                .mapNotNull { globalMediaIndexOf(it.key, offsetByGroupId) }
            stickyDayRangeLabel(visibleMediaIndices, sections, sectionStartOffsets, locale)
        }
    }

    // Pin the sticky header once the non-sticky header item scrolls past the top of the viewport:
    // either it is gone entirely, or its top edge has reached/crossed the viewport top (so it is
    // being clipped and the pinned overlay takes over seamlessly).
    val showStickyHeader by remember(sections) {
        derivedStateOf {
            if (sections.isEmpty()) return@derivedStateOf false
            val layoutInfo = lazyGridState.layoutInfo
            val headerInfo = layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == NON_STICKY_HEADER_ITEM }
            headerInfo == null || headerInfo.offset.y < layoutInfo.viewportStartOffset
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FastScrollLazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .photosGridDragToSelectGesture(
                    lazyGridState = lazyGridState,
                    mediaIndexOfKey = { key ->
                        globalMediaIndexOf(key, currentOffsetByGroupId)
                    },
                    isMediaSelected = { index ->
                        // An unloaded cell renders as checked through its pending mark, so treat
                        // it as selected too — anchoring a drag on it must deselect, not reselect.
                        currentLoadedNodes[index]
                            ?.let { it.id in currentSelectedPhotoIds }
                            ?: (index in pendingSelection)
                    },
                    onDragSelectStarted = {
                        Analytics.tracker.trackEvent(MediaScreenDragToSelectStartedEvent)
                    },
                    onDragSelectionChange = { index, selected ->
                        val node = currentLoadedNodes[index]
                        if (node == null) {
                            if (selected) {
                                if (index !in pendingSelection) pendingSelection[index] = null
                            } else {
                                unmarkPendingSelection(index)
                            }
                        } else {
                            if (!selected) pendingSelection.remove(index)
                            node.takeUnless { it.isTakenDown }
                                ?.takeIf { (it.id in currentSelectedPhotoIds) != selected }
                                ?.let { currentOnNodeSelected(it) }
                        }
                    },
                    onDragSelectEnded = { resolvePendingDragSelection() },
                )
                .photosZoomGestureDetector(
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onPinchActiveChanged = onPinchActiveChanged,
                )
                .testTag(TIMELINE_REVAMP_CONTENT_GRID_TAG),
            state = lazyGridState,
            totalItems = totalGridItems,
            contentPadding = contentPadding,
            fastScrollerVerticalOffset = 36.dp
        ) {
            bannerContent?.let {
                item(
                    key = ENABLE_CU_BANNER,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    it()
                }
            }

            item(
                key = NON_STICKY_HEADER_ITEM,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                val selectableRange = topVisibleMonthRange?.takeIf { isSelectionMode }
                StickySectionHeader(
                    modifier = Modifier
                        .testTag(TIMELINE_REVAMP_NON_STICKY_HEADER_TAG),
                    title = stickyLabel,
                    leadingContent = selectableRange?.let { range ->
                        {
                            SectionSelectAllCheckbox(
                                checked = isRangeSelected(range),
                                modifier = Modifier.testTag(
                                    TIMELINE_REVAMP_NON_STICKY_HEADER_CHECKBOX_TAG
                                ),
                            )
                        }
                    },
                    trailingContent = {
                        TimelineRevampGridSizeMenu(
                            gridSize = gridSize,
                            onGridSizeChange = onGridSizeChange,
                        )
                    },
                    onClick = selectableRange?.let { range ->
                        { toggleRangeSelection(range) }
                    },
                )
            }

            sections.zip(sectionStartOffsets).forEachIndexed { sectionIndex, (section, base) ->
                if (sectionIndex in headerIndexes) {
                    val monthHeaderKey = monthKey(section.startDate)
                    item(
                        key = "$HEADER_KEY_PREFIX$monthHeaderKey",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        val selectableRange = monthRanges[monthHeaderKey]
                            ?.takeIf { isSelectionMode }
                        StickySectionHeader(
                            modifier = Modifier
                                .testTag("$TIMELINE_REVAMP_SECTION_HEADER_TAG$monthHeaderKey"),
                            title = timelineMonthLabel(section.startDate, locale),
                            leadingContent = selectableRange?.let { range ->
                                {
                                    SectionSelectAllCheckbox(
                                        checked = isRangeSelected(range),
                                        modifier = Modifier.testTag(
                                            "$TIMELINE_REVAMP_SECTION_HEADER_CHECKBOX_TAG$monthHeaderKey"
                                        ),
                                    )
                                }
                            },
                            onClick = selectableRange?.let { range ->
                                { toggleRangeSelection(range) }
                            },
                        )
                    }
                }

                items(
                    count = section.count.toInt(),
                    key = { index -> mediaKey(section.groupId, index) },
                ) { index ->
                    val node = loadedNodes[base + index]
                    PhotoNodeBodyV2(
                        node = node,
                        modifier = Modifier
                            .animateItem()
                            .padding(all = 1.dp),
                        isSelected = if (node != null) {
                            node.id in selectedPhotoIds
                        } else {
                            base + index in pendingSelection
                        },
                        shouldShowFavourite = node?.isFavourite == true,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        onClick = { onNodeClicked(node, base + index) },
                        onLongClick = { node?.let(onNodeSelected) },
                    )
                }
            }
        }

        if (showStickyHeader) {
            val selectableRange = topVisibleMonthRange?.takeIf { isSelectionMode }
            StickySectionHeader(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .testTag(TIMELINE_REVAMP_STICKY_HEADER_TAG),
                title = stickyLabel,
                leadingContent = selectableRange?.let { range ->
                    {
                        SectionSelectAllCheckbox(
                            checked = isRangeSelected(range),
                            modifier = Modifier.testTag(
                                TIMELINE_REVAMP_STICKY_HEADER_CHECKBOX_TAG
                            ),
                        )
                    }
                },
                trailingContent = {
                    TimelineRevampGridSizeMenu(
                        gridSize = gridSize,
                        onGridSizeChange = onGridSizeChange,
                    )
                },
                onClick = selectableRange?.let { range ->
                    { toggleRangeSelection(range) }
                },
            )
        }
    }
}

/**
 * True for the day-level grid periods ([MediaTimePeriod.All] and [MediaTimePeriod.Days], which behave
 * identically in the revamp).
 */
private fun MediaTimePeriod.isGridPeriod(): Boolean =
    this == MediaTimePeriod.All || this == MediaTimePeriod.Days

private fun MediaTimelineSection.isInMonth(year: Int, month: Int): Boolean {
    val zonedDateTime = TimelineDateCache.get(startDate)
    return zonedDateTime.year == year && zonedDateTime.monthValue == month
}

private sealed interface PendingCardScroll {
    data class ToYear(val year: Int) : PendingCardScroll
    data class ToMonth(val year: Int, val month: Int) : PendingCardScroll
}

@Composable
private fun rememberSelectorVisibility(
    selectedPeriod: MediaTimePeriod,
    gridState: LazyGridState,
    listState: LazyListState,
): State<Boolean> {
    val isGrid = selectedPeriod.isGridPeriod()
    val isScrollingDown by if (isGrid) gridState.isScrollingDown() else listState.isScrollingDown()
    val isScrolledToEnd by if (isGrid) gridState.isScrolledToEnd() else listState.isScrolledToEnd()
    val isScrolledToTop by if (isGrid) gridState.isScrolledToTop() else listState.isScrolledToTop()
    return remember(selectedPeriod, gridState, listState) {
        derivedStateOf {
            val isScrollInProgress =
                if (isGrid) gridState.isScrollInProgress else listState.isScrollInProgress
            !isScrollInProgress || (!isScrollingDown && !isScrolledToEnd) || isScrolledToTop
        }
    }
}

/**
 * Builds the pinned sticky-header label: the day range of the on-screen items within the top-most
 * visible month, e.g. "June 20—26 2026" ("June 20 2026" for a single day). Falls back to the first
 * section's day when no media items are on screen yet.
 */
private fun stickyDayRangeLabel(
    visibleMediaIndices: List<Int>,
    sections: List<MediaTimelineSection>,
    sectionStartOffsets: List<Int>,
    locale: Locale,
): String {
    val topSection = sections.firstOrNull() ?: return ""
    if (visibleMediaIndices.isEmpty()) {
        val day = TimelineDateCache.get(topSection.startDate).dayOfMonth
        return dayRangeLabel(topSection.startDate, day, day, locale)
    }

    val topIndex = sectionIndexOfMedia(
        visibleMediaIndices.min(),
        sectionStartOffsets
    ).coerceIn(sections.indices)
    val bottomIndex = sectionIndexOfMedia(
        visibleMediaIndices.max(),
        sectionStartOffsets
    ).coerceIn(sections.indices)
    val top = TimelineDateCache.get(sections[topIndex].startDate)

    var minDay = Int.MAX_VALUE
    var maxDay = Int.MIN_VALUE
    for (index in topIndex..bottomIndex) {
        val date = TimelineDateCache.get(sections[index].startDate)
        if (date.year == top.year && date.monthValue == top.monthValue) {
            minDay = minOf(minDay, date.dayOfMonth)
            maxDay = maxOf(maxDay, date.dayOfMonth)
        }
    }
    return dayRangeLabel(sections[topIndex].startDate, minDay, maxDay, locale)
}

/**
 * Index of the section owning the global media slot [globalIndex], via [sectionStartOffsets].
 */
private fun sectionIndexOfMedia(globalIndex: Int, sectionStartOffsets: List<Int>): Int {
    if (sectionStartOffsets.isEmpty()) return 0
    return sectionStartOffsets.indexOfLast { it <= globalIndex }
        .coerceIn(0, sectionStartOffsets.lastIndex)
}

private fun dayRangeLabel(
    monthStartDateSeconds: Long,
    minDay: Int,
    maxDay: Int,
    locale: Locale,
): String {
    val zonedDateTime = TimelineDateCache.get(monthStartDateSeconds)
    val monthName = DateTimeFormatter
        .ofPattern(getBestDateTimePattern(locale, "LLLL"), locale)
        .format(zonedDateTime)
    val minDayText = String.format(locale, "%02d", minDay)
    val maxDayText = String.format(locale, "%02d", maxDay)
    val dayPart = if (minDay == maxDay) minDayText else "$minDayText$DAY_RANGE_SEPARATOR$maxDayText"
    return "$monthName $dayPart ${zonedDateTime.year}"
}

@Composable
private fun SectionSelectAllCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        checked = checked,
        onCheckStateChanged = {},
        clickable = false,
        tapTargetArea = false,
        modifier = modifier.semantics {
            toggleableState = ToggleableState(checked)
        },
    )
}

@Composable
private fun TimelineRevampGridSizeMenu(
    gridSize: TimelineGridSize,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        val gridSizeIcon = when (gridSize) {
            TimelineGridSize.Large -> IconPack.Small.Thin.Outline.Square
            TimelineGridSize.Default -> IconPack.Small.Thin.Outline.Grid4
            TimelineGridSize.Compact -> IconPack.Small.Thin.Outline.Grid9
        }
        val interactionSource = remember { MutableInteractionSource() }
        MegaIcon(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { expanded = !expanded }
                .padding(
                    start = GRID_SIZE_ICON_LEADING_TOUCH_PADDING,
                    end = HeaderHorizontalInset,
                    top = HeaderVerticalInset,
                    bottom = HeaderVerticalInset,
                )
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = GRID_SIZE_ICON_RIPPLE_RADIUS),
                )
                .testTag(TIMELINE_REVAMP_GRID_SIZE_ICON_TAG),
            imageVector = gridSizeIcon,
            tint = IconColor.Secondary,
            contentDescription = "Change grid size, current size is : ${gridSize.name}",
        )

        TimelineGridSizeSettingsMenu(
            modifier = Modifier
                .widthIn(min = 220.dp)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 6.dp),
                text = stringResource(sharedR.string.timeline_tab_grid_size_menu_title),
                style = AppTheme.typography.labelLarge,
                textColor = TextColor.Secondary,
            )

            TimelineGridSize.entries.reversed().forEach {
                DropdownMenuItem(
                    text = {
                        MegaText(
                            text = stringResource(it.nameResId),
                            style = AppTheme.typography.bodyLarge,
                            textColor = TextColor.Primary,
                        )
                    },
                    leadingIcon = {
                        if (gridSize == it) {
                            MegaIcon(
                                imageVector = IconPack.Medium.Thin.Outline.Check,
                                tint = IconColor.Primary,
                                contentDescription = null,
                            )
                        } else {
                            Box(modifier = Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        onGridSizeChange(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Observes which media slots are currently visible and reports the global index range to the
 * ViewModel so it can lazily load that window.
 */
@Composable
private fun NotifyVisibleMediaRange(
    gridState: LazyGridState,
    offsetByGroupId: Map<String, Int>,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
) {
    LaunchedEffect(gridState, offsetByGroupId, onVisibleRangeChanged) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .mapNotNull { globalMediaIndexOf(it.key, offsetByGroupId) }
        }
            .distinctUntilChanged()
            .collect { visibleIndices ->
                if (visibleIndices.isNotEmpty()) {
                    onVisibleRangeChanged(visibleIndices.min(), visibleIndices.max())
                }
            }
    }
}

/**
 * Reports whether the grid is actively scrolling, so the ViewModel can hold a silent refresh's
 * re-layout until the user is at rest.
 */
@Composable
private fun ReportScrollInProgress(
    gridState: LazyGridState,
    onScrollingChanged: (Boolean) -> Unit,
) {
    LaunchedEffect(gridState, onScrollingChanged) {
        try {
            snapshotFlow { gridState.isScrollInProgress }
                .distinctUntilChanged()
                .collect(onScrollingChanged)
        } finally {
            // To avoid indefinite suspend when leaving composition
            onScrollingChanged(false)
        }
    }
}

/**
 * Formats a month header label, mirroring the legacy tab: "May" for the current year and "May 2024"
 * otherwise, using the locale's best pattern for the skeleton.
 */
private fun timelineMonthLabel(startDateSeconds: Long, locale: Locale): String {
    val zonedDateTime = TimelineDateCache.get(startDateSeconds)
    val isCurrentYear = zonedDateTime.year == Year.now().value
    val skeleton = if (isCurrentYear) "LLLL" else "LLLL yyyy"
    val pattern = getBestDateTimePattern(locale, skeleton)
    return DateTimeFormatter.ofPattern(pattern, locale).format(zonedDateTime)
}

private const val DAY_RANGE_SEPARATOR = "—"

/** Bottom clearance so timeline content can scroll clear of the floating MediaTimePeriodSelector. */
private val TIMELINE_REVAMP_SELECTOR_CLEARANCE = 90.dp

/** Grace period before hiding the selector, so a short accidental scroll-down doesn't flicker it away. */
private const val SELECTOR_HIDE_DEBOUNCE_MS = 200L

/** Extra touch area on the leading side of the grid size action, where the header has room. */
private val GRID_SIZE_ICON_LEADING_TOUCH_PADDING = 32.dp

private val GRID_SIZE_ICON_RIPPLE_RADIUS = 20.dp

internal const val TIMELINE_REVAMP_CONTENT_GRID_TAG = "timeline_revamp_content:grid"
internal const val TIMELINE_REVAMP_STICKY_HEADER_TAG = "timeline_revamp_content:sticky_header"
internal const val TIMELINE_REVAMP_NON_STICKY_HEADER_TAG =
    "timeline_revamp_content:non_sticky_header"
internal const val TIMELINE_REVAMP_CARD_LIST_TAG = "timeline_revamp_content:card_list"
internal const val TIMELINE_REVAMP_CARD_LIST_SKELETON_TAG =
    "timeline_revamp_content:card_list_skeleton"
internal const val TIMELINE_REVAMP_SECTION_HEADER_TAG = "timeline_revamp_content:section_header_"
internal const val TIMELINE_REVAMP_SECTION_HEADER_CHECKBOX_TAG =
    "timeline_revamp_content:section_header_checkbox_"
internal const val TIMELINE_REVAMP_STICKY_HEADER_CHECKBOX_TAG =
    "timeline_revamp_content:sticky_header_checkbox"
internal const val TIMELINE_REVAMP_NON_STICKY_HEADER_CHECKBOX_TAG =
    "timeline_revamp_content:non_sticky_header_checkbox"
internal const val TIMELINE_REVAMP_GRID_SIZE_ICON_TAG = "timeline_revamp_content:grid_size_icon"
internal const val TIMELINE_REVAMP_LOADING_SKELETON_TAG = "timeline_revamp_content:loading_skeleton"
internal const val TIMELINE_REVAMP_EMPTY_VIEW_TAG = "timeline_revamp_content:empty_view"
internal const val TIMELINE_REVAMP_ENABLE_CU_CONTENT_TAG = "timeline_revamp_content:enable_cu"

@CombinedThemePreviews
@Composable
private fun TimelineRevampScreenPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 7,
                    ),
                    MediaTimelineSection(
                        groupId = "2026-05-10",
                        startDate = 1_778_371_200L,
                        endDate = 1_778_371_200L,
                        count = 4,
                    ),
                ),
                sectionStartOffsets = listOf(0, 7),
                loadedNodes = emptyMap(),
            ),
            onVisibleRangeChanged = { _, _ -> },
            loadMediaRange = { _, _ -> emptyMap() },
            onGridSizeChange = {},
            onZoomIn = {},
            onZoomOut = {},
            onMediaTimePeriodSelected = {},
            onNodeClicked = { _, _ -> },
            onNodeSelected = {},
            onScrollingChanged = {},
            selectedPhotoIds = emptySet(),
            onTakenDownDialogEventConsumed = {},
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            showEnableCameraUploadsPage = false,
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

@CombinedThemePreviews
@Composable
private fun TimelineRevampWithBannerPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "2026-06-15",
                        startDate = 1_781_481_600L,
                        endDate = 1_781_481_600L,
                        count = 7,
                    ),
                    MediaTimelineSection(
                        groupId = "2026-05-10",
                        startDate = 1_778_371_200L,
                        endDate = 1_778_371_200L,
                        count = 4,
                    ),
                ),
                sectionStartOffsets = listOf(0, 7),
                loadedNodes = emptyMap(),
            ),
            onVisibleRangeChanged = { _, _ -> },
            loadMediaRange = { _, _ -> emptyMap() },
            onGridSizeChange = {},
            onZoomIn = {},
            onZoomOut = {},
            onMediaTimePeriodSelected = {},
            onNodeClicked = { _, _ -> },
            onNodeSelected = {},
            onScrollingChanged = {},
            selectedPhotoIds = emptySet(),
            onTakenDownDialogEventConsumed = {},
            mediaCameraUploadUiState = MediaCameraUploadUiState(
                status = CUStatusUiState.Disabled(shouldNotifyUser = true),
            ),
            showEnableCameraUploadsPage = false,
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

@CombinedThemePreviews
@Composable
private fun TimelineRevampEmptyPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Empty,
            onVisibleRangeChanged = { _, _ -> },
            loadMediaRange = { _, _ -> emptyMap() },
            onGridSizeChange = {},
            onZoomIn = {},
            onZoomOut = {},
            onMediaTimePeriodSelected = {},
            onNodeClicked = { _, _ -> },
            onNodeSelected = {},
            onScrollingChanged = {},
            selectedPhotoIds = emptySet(),
            onTakenDownDialogEventConsumed = {},
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            showEnableCameraUploadsPage = false,
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
