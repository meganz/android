package mega.privacy.android.feature.photos.presentation.mediadiscovery

import MediaGridViewItem
import android.annotation.SuppressLint
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.feature.photos.presentation.mediadiscovery.model.MediaDiscoveryPeriod
import mega.privacy.android.feature.photos.presentation.mediadiscovery.view.MediaDiscoveryCardListView
import mega.privacy.android.feature.photos.presentation.mediadiscovery.view.MediaDiscoveryPeriodChip
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun CloudDriveMediaDiscoveryRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudDriveMediaDiscoveryViewModel = hiltViewModel<CloudDriveMediaDiscoveryViewModel, CloudDriveMediaDiscoveryViewModel.Factory>(
        creationCallback = { it.create() }
    ),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMoreOptionsClicked: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    CloudDriveMediaDiscoveryScreen(
        uiState = uiState,
        onBack = onBack,
        onItemClicked = { photo ->
            if (uiState.isInSelectionMode) {
                viewModel.selectPhoto(photo)
            } else {
                // Todo open photo or video preview
            }
        },
        onItemLongPressed = viewModel::selectPhoto,
        onPeriodSelected = viewModel::updatePeriod,
        selectAllItems = viewModel::selectAllPhotos,
        deselectAllItems = viewModel::clearSelectedPhotos,
        onCardClick = viewModel::selectPeriod,
        onMoreOptionsClicked = onMoreOptionsClicked,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveMediaDiscoveryScreen(
    uiState: CloudDriveMediaDiscoveryUiState,
    onBack: () -> Unit,
    onItemClicked: (Photo) -> Unit,
    onItemLongPressed: (Photo) -> Unit,
    selectAllItems: () -> Unit,
    deselectAllItems: () -> Unit,
    onMoreOptionsClicked: () -> Unit,
    onPeriodSelected: (MediaDiscoveryPeriod) -> Unit,
    onCardClick: (DateCard) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var isPeriodVisible by remember { mutableStateOf(true) }

    // Todo: Implement dropdown option
    var isDropdownVisible by remember { mutableStateOf(false) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedPhotosCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = selectAllItems,
                    onCancelSelectionClicked = deselectAllItems
                )
            } else {
                MegaTopAppBar(
                    title = uiState.folderName,
                    navigationType = AppBarNavigationType.Back(onBack),
                    actions = listOf(
                        MenuActionWithClick(
                            menuAction = object : MenuActionWithIcon {
                                @Composable
                                override fun getIconPainter(): Painter =
                                    rememberVectorPainter(IconPack.Medium.Thin.Outline.SlidersVertical02)

                                override val testTag: String =
                                    "CloudDriveMediaDiscoveryScreen:top_app_bar_filter"

                                @Composable
                                override fun getDescription(): String =
                                    stringResource(sharedR.string.general_action_filter)
                            },
                            onClick = {
                                // Todo: Implement dropdown option
                                isDropdownVisible = true
                            }
                        ),
                        MenuActionWithClick(CommonAppBarAction.More) {
                            onMoreOptionsClicked()
                        }
                    )
                )
            }
        },
        bottomBar = {
            // Todo: Implement NodeSelectionModeBottomBar to handle multi photos action
        }
    ) { containerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(containerPadding)
        ) {
            when (uiState.selectedPeriod) {
                MediaDiscoveryPeriod.All -> {
                    MediaDiscoveryGridView(
                        uiState = uiState,
                        onBack = onBack,
                        contentPadding = contentPadding,
                        onScrollingListener = { isScrolling ->
                            isPeriodVisible = !isScrolling
                        },
                        onItemClicked = onItemClicked,
                        onItemLongPressed = onItemLongPressed,
                    )
                }

                else -> {
                    val dateCards = when (uiState.selectedPeriod) {
                        MediaDiscoveryPeriod.Years -> uiState.yearsCardList
                        MediaDiscoveryPeriod.Months -> uiState.monthsCardList
                        MediaDiscoveryPeriod.Days -> uiState.daysCardList
                        else -> uiState.daysCardList
                    }
                    MediaDiscoveryCardListView(
                        dateCards = dateCards,
                        onCardClick = onCardClick,
                        fromFolderLink = uiState.fromFolderLink,
                        shouldApplySensitiveMode = uiState.isHiddenNodesEnabled &&
                                uiState.accountType?.isPaid == true &&
                                !uiState.isBusinessAccountExpired,
                    )
                }
            }

            MediaDiscoveryPeriodChip(
                selectedMediaDiscoveryPeriod = uiState.selectedPeriod,
                onTimeBarTabSelected = onPeriodSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter),
                isVisible = isPeriodVisible && !uiState.isInSelectionMode
            )
        }
    }
}

@Composable
private fun MediaDiscoveryGridView(
    uiState: CloudDriveMediaDiscoveryUiState,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onScrollingListener: (Boolean) -> Unit,
    onItemClicked: (Photo) -> Unit,
    onItemLongPressed: (Photo) -> Unit,
) {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }
    val gridCells = remember(uiState.currentZoomLevel) {
        when (uiState.currentZoomLevel) {
            ZoomLevel.Grid_1 -> GridCells.Fixed(1)
            ZoomLevel.Grid_3 -> GridCells.Fixed(3)
            ZoomLevel.Grid_5 -> GridCells.Fixed(5)
        }
    }
    val lazyGridState = rememberSaveable(
        uiState.scrollStartIndex,
        uiState.scrollStartOffset,
        saver = LazyGridState.Saver,
    ) {
        LazyGridState(
            uiState.scrollStartIndex,
            uiState.scrollStartOffset,
        )
    }

    LaunchedEffect(uiState.scrollStartIndex) {
        if (uiState.scrollStartIndex > 0) {
            val startIndex = if (uiState.selectedPeriod == MediaDiscoveryPeriod.All) {
                uiState.scrollStartIndex
            } else {
                // Since CardListView has two headers, the index needs to be incremented by 2.
                uiState.scrollStartIndex + 2
            }
            lazyGridState.animateScrollToItem(startIndex)
        }
    }

    LaunchedEffect(lazyGridState.isScrollInProgress) {
        onScrollingListener(lazyGridState.isScrollInProgress)
    }

    FastScrollLazyVerticalGrid(
        state = lazyGridState,
        totalItems = uiState.mediaListItemList.size,
        columns = gridCells,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = contentPadding
    ) {
        item(
            key = "sort_header_item",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            NodeHeaderItem(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                onSortOrderClick = {},
                onChangeViewTypeClick = onBack,
                onEnterMediaDiscoveryClick = {},
                sortConfiguration = NodeSortConfiguration.default,
                isListView = false,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        items(
            key = { uiState.mediaListItemList[it].key },
            count = uiState.mediaListItemList.size,
            span = { index ->
                val item = uiState.mediaListItemList[index]
                if (item is MediaListItem.Separator) {
                    GridItemSpan(maxLineSpan)
                } else {
                    GridItemSpan(1)
                }
            }
        ) {
            when (val item = uiState.mediaListItemList[it]) {
                is MediaListItem.PhotoItem -> {
                    val photo = item.photo
                    val isSensitive = uiState.isHiddenNodesEnabled &&
                            uiState.showHiddenNodes &&
                            (photo.isSensitive || photo.isSensitiveInherited)

                    MediaGridViewItem(
                        thumbnailData = ThumbnailRequest(
                            id = NodeId(photo.id),
                            isPublicNode = uiState.fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            photo.name.substringAfterLast('.', ""),
                        ),
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = photo.isFavourite,
                        isSelected = photo.id in uiState.selectedPhotoIds,
                        onClick = { onItemClicked(photo) },
                        onLongClick = { onItemLongPressed(photo) },
                    )
                }

                is MediaListItem.VideoItem -> {
                    val video = item.video
                    val isSensitive = uiState.isHiddenNodesEnabled &&
                            uiState.showHiddenNodes &&
                            (video.isSensitive || video.isSensitiveInherited)
                    MediaGridViewItem(
                        thumbnailData = ThumbnailRequest(
                            id = NodeId(video.id),
                            isPublicNode = uiState.fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            video.name.substringAfterLast('.', ""),
                        ),
                        duration = item.duration,
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = video.isFavourite,
                        isSelected = video.id in uiState.selectedPhotoIds,
                        onClick = { onItemClicked(video) },
                        onLongClick = { onItemLongPressed(video) },
                    )
                }

                is MediaListItem.Separator -> {
                    MediaDiscoverySeparator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        currentZoomLevel = uiState.currentZoomLevel,
                        modificationTime = item.modificationTime,
                    )
                }
            }
        }

        // Bottom spacer so content is not hidden behind TimeSwitchBar
        item(
            key = "bottom_spacer",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
private fun MediaDiscoverySeparator(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val dateText = remember(modificationTime, currentZoomLevel, locale) {
        formatSeparatorDate(
            currentZoomLevel = currentZoomLevel,
            modificationTime = modificationTime,
            locale = locale,
        )
    }

    MegaText(
        text = dateText,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

private fun formatSeparatorDate(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (currentZoomLevel == ZoomLevel.Grid_1) {
        if (modificationTime.year == LocalDateTime.now().year) {
            DateFormat.getBestDateTimePattern(
                locale, "dd MMMM"
            )
        } else {
            DateFormat.getBestDateTimePattern(
                locale, "dd MMMM yyyy"
            )
        }
    } else {
        if (modificationTime.year == LocalDateTime.now().year) {
            DateFormat.getBestDateTimePattern(locale, "LLLL")
        } else {
            DateFormat.getBestDateTimePattern(locale, "LLLL yyyy")
        }
    }
    return SimpleDateFormat(datePattern, locale).format(
        Date.from(
            modificationTime
                .toLocalDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
    )
}
