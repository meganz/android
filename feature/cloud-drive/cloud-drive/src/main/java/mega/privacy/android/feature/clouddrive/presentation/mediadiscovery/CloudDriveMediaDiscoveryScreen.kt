package mega.privacy.android.feature.clouddrive.presentation.mediadiscovery

import MediaGridViewItem
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
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
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    CloudDriveMediaDiscoveryScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

// Todo implement Years, Month, Days chip
@Composable
internal fun CloudDriveMediaDiscoveryScreen(
    uiState: CloudDriveMediaDiscoveryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fromFolderLink: Boolean = false,
) {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }

    val gridCells = remember(uiState.currentZoomLevel) {
        when (uiState.currentZoomLevel) {
            ZoomLevel.Grid_1 -> GridCells.Fixed(1)
            ZoomLevel.Grid_3 -> GridCells.Fixed(3)
            ZoomLevel.Grid_5 -> GridCells.Fixed(5)
        }
    }

    FastScrollLazyVerticalGrid(
        modifier = modifier,
        state = rememberLazyGridState(),
        totalItems = uiState.mediaListItemList.size,
        columns = gridCells,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = contentPadding
    ) {
        // Todo: Implement custom header item
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
                showSortOrder = true,
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
                            isPublicNode = fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            photo.name.substringAfterLast('.', ""),
                        ),
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = photo.isFavourite,
                        onClick = { },
                        onLongClick = { },
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
                            isPublicNode = fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            video.name.substringAfterLast('.', ""),
                        ),
                        duration = item.duration,
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = video.isFavourite,
                        onClick = { },
                        onLongClick = { },
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
            android.text.format.DateFormat.getBestDateTimePattern(
                locale, "dd MMMM"
            )
        } else {
            android.text.format.DateFormat.getBestDateTimePattern(
                locale, "dd MMMM yyyy"
            )
        }
    } else {
        if (modificationTime.year == LocalDateTime.now().year) {
            android.text.format.DateFormat.getBestDateTimePattern(locale, "LLLL")
        } else {
            android.text.format.DateFormat.getBestDateTimePattern(locale, "LLLL yyyy")
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
