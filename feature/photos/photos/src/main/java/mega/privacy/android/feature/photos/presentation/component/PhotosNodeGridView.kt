package mega.privacy.android.feature.photos.presentation.component

import android.content.res.Configuration
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.feature.photos.components.ImagePhotosNode
import mega.privacy.android.feature.photos.components.TimelineGridSizeSettingsMenu
import mega.privacy.android.feature.photos.components.VideoPhotosNode
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem.HeaderItem
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem.PhotoNodeItem
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.resources.R as sharedR
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

@Composable
fun PhotosNodeGridView(
    items: List<PhotosNodeContentItem>,
    selectedPhotoIds: Set<Long>,
    gridSize: TimelineGridSize,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onClick: (node: PhotoNodeUiState) -> Unit,
    onLongClick: (node: PhotoNodeUiState) -> Unit,
    modifier: Modifier = Modifier,
    disabledPhotoIds: Set<Long> = emptySet(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    header: (@Composable () -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val spanCount = remember(key1 = configuration.orientation, key2 = gridSize) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridSize.portrait
        } else {
            gridSize.landscape
        }
    }
    val isPreview by remember(configuration, gridSize) {
        derivedStateOf { isPreview(configuration, gridSize) }
    }

    FastScrollLazyVerticalGrid(
        totalItems = items.size,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = lazyGridState,
        tooltipText = { index ->
            val item = items.getOrNull(index)
            item?.let {
                val modificationTime = when (it) {
                    is HeaderItem -> it.time
                    is PhotoNodeItem -> it.node.photo.modificationTime
                }
                dateText(
                    modificationTime = modificationTime,
                    gridSize = gridSize,
                    locale = configuration.locales[0],
                )
            }.orEmpty()
        },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        header?.let {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "PhotosNodeGridView:Header"
            ) {
                it()
            }
        }

        itemsIndexed(
            items = items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.type },
            span = { _, item ->
                when (item) {
                    is HeaderItem -> GridItemSpan(maxLineSpan)
                    is PhotoNodeItem -> GridItemSpan(1)
                }
            }
        ) { index, contentType ->
            when (contentType) {
                is HeaderItem -> {
                    HeaderBody(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (index == 0) 16.dp else 8.dp,
                                bottom = 8.dp
                            )
                            .padding(horizontal = 16.dp)
                            .testTag(PHOTOS_NODE_GRID_VIEW_HEADER_BODY_TAG),
                        time = contentType.time,
                        shouldShowGridSizeSettings = index == 0,
                        gridSize = gridSize,
                        onGridSizeChange = onGridSizeChange
                    )
                }

                is PhotoNodeItem -> {
                    PhotoNodeBody(
                        modifier = Modifier.animateItem(),
                        spanCount = spanCount,
                        node = contentType.node,
                        isPreview = isPreview,
                        isSelected = contentType.node.photo.id in selectedPhotoIds,
                        shouldShowFavourite = contentType.node.photo.isFavourite,
                        enabled = contentType.node.photo.id !in disabledPhotoIds,
                        onClick = { onClick(contentType.node) },
                        onLongClick = { onLongClick(contentType.node) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBody(
    time: LocalDateTime,
    shouldShowGridSizeSettings: Boolean,
    gridSize: TimelineGridSize,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locales = LocalConfiguration.current.locales
    var isGridSizeMenuExpanded by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        MegaText(
            modifier = Modifier.weight(weight = 1F),
            text = dateText(
                modificationTime = time,
                gridSize = gridSize,
                locale = locales[0],
            ),
            style = AppTheme.typography.titleSmall,
            textColor = TextColor.Secondary
        )

        if (shouldShowGridSizeSettings) {
            Box {
                val gridSizeIcon = when (gridSize) {
                    TimelineGridSize.Large -> IconPack.Small.Thin.Outline.Square
                    TimelineGridSize.Default -> IconPack.Small.Thin.Outline.Grid4
                    TimelineGridSize.Compact -> IconPack.Small.Thin.Outline.Grid9
                }
                MegaIcon(
                    modifier = Modifier
                        .clickable {
                            isGridSizeMenuExpanded = !isGridSizeMenuExpanded
                        }
                        .testTag(PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_ICON_TAG),
                    imageVector = gridSizeIcon,
                    tint = IconColor.Secondary,
                    contentDescription = "Change grid size, current size is : ${gridSize.name}"
                )

                TimelineGridSizeSettingsMenu(
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag(PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_MENU_TAG),
                    expanded = isGridSizeMenuExpanded,
                    onDismissRequest = { isGridSizeMenuExpanded = false }
                ) {
                    MegaText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 6.dp),
                        text = stringResource(sharedR.string.timeline_tab_grid_size_menu_title),
                        style = AppTheme.typography.labelLarge,
                        textColor = TextColor.Secondary
                    )

                    TimelineGridSize.entries.reversed().forEach {
                        DropdownMenuItem(
                            text = {
                                MegaText(
                                    text = stringResource(it.nameResId),
                                    style = AppTheme.typography.bodyLarge,
                                    textColor = TextColor.Primary
                                )
                            },
                            leadingIcon = {
                                if (gridSize == it) {
                                    MegaIcon(
                                        imageVector = IconPack.Medium.Thin.Outline.Check,
                                        tint = IconColor.Primary,
                                        contentDescription = null
                                    )
                                } else {
                                    Box(modifier = Modifier.size(24.dp))
                                }
                            },
                            onClick = {
                                onGridSizeChange(it)
                                isGridSizeMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoNodeBody(
    spanCount: Int,
    node: PhotoNodeUiState,
    isPreview: Boolean,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val photosNodeSize = remember(spanCount) {
        with(density) {
            (windowInfo.containerSize.width / spanCount).toDp()
        }
    }
    when (node.photo) {
        is PhotoUiState.Image -> {
            ImagePhotosNode(
                modifier = modifier
                    .size(photosNodeSize)
                    .testTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG),
                thumbnailRequest = MediaThumbnailRequest(
                    id = node.photo.id,
                    isPreview = isPreview,
                    thumbnailFilePath = node.photo.thumbnailFilePath,
                    previewFilePath = node.photo.previewFilePath,
                    isPublicNode = false,
                    fileExtension = node.photo.fileTypeInfo.extension
                ),
                isSensitive = node.isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }

        is PhotoUiState.Video -> {
            VideoPhotosNode(
                modifier = modifier
                    .size(photosNodeSize)
                    .testTag(VIDEO_NODE_BODY_IMAGE_NODE_TAG),
                duration = node.photo.duration,
                thumbnailRequest = MediaThumbnailRequest(
                    id = node.photo.id,
                    isPreview = isPreview,
                    thumbnailFilePath = node.photo.thumbnailFilePath,
                    previewFilePath = node.photo.previewFilePath,
                    isPublicNode = false,
                    fileExtension = node.photo.fileTypeInfo.extension
                ),
                isSensitive = node.isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }
}

private fun isPreview(configuration: Configuration, gridSize: TimelineGridSize) =
    configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            && gridSize.portrait == TimelineGridSize.Large.portrait

private fun dateText(
    gridSize: TimelineGridSize,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (gridSize == TimelineGridSize.Large) {
        if (modificationTime.year == LocalDateTime.now().year) {
            getBestDateTimePattern(locale, "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY")
        } else {
            getBestDateTimePattern(
                locale,
                "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY $DATE_FORMAT_YEAR_WITH_MONTH"
            )
        }
    } else {
        if (modificationTime.year == LocalDateTime.now().year) {
            getBestDateTimePattern(locale, DATE_FORMAT_MONTH)
        } else {
            getBestDateTimePattern(locale, "$DATE_FORMAT_MONTH $DATE_FORMAT_YEAR_WITH_MONTH")
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

@CombinedThemePreviews
@Composable
private fun PhotosNodeGridViewPreview() {
    AndroidThemeForPreviews {
        PhotosNodeGridView(
            items = persistentListOf(
                HeaderItem(time = LocalDateTime.now()),
                PhotoNodeItem(
                    node = PhotoNodeUiState(
                        photo = PhotoUiState.Image(
                            id = 1L,
                            albumPhotoId = null,
                            parentId = 0L,
                            name = "",
                            isFavourite = false,
                            creationTime = LocalDateTime.now(),
                            modificationTime = LocalDateTime.now(),
                            thumbnailFilePath = null,
                            previewFilePath = null,
                            fileTypeInfo = StaticImageFileTypeInfo(
                                mimeType = "",
                                extension = "",
                            ),
                            size = 0L,
                            isTakenDown = false,
                            isSensitive = false,
                            isSensitiveInherited = false
                        ),
                        defaultIcon = R.drawable.ic_3d_medium_solid,
                        isSensitive = false,
                    )
                ),
                PhotoNodeItem(
                    node = PhotoNodeUiState(
                        photo = PhotoUiState.Video(
                            id = 2L,
                            albumPhotoId = null,
                            parentId = 0L,
                            name = "",
                            isFavourite = false,
                            creationTime = LocalDateTime.now(),
                            modificationTime = LocalDateTime.now(),
                            thumbnailFilePath = null,
                            previewFilePath = null,
                            fileTypeInfo = VideoFileTypeInfo(
                                mimeType = "",
                                extension = "",
                                duration = Duration.ZERO
                            ),
                            size = 0L,
                            isTakenDown = false,
                            isSensitive = false,
                            isSensitiveInherited = false
                        ),
                        defaultIcon = R.drawable.ic_3d_medium_solid,
                        isSensitive = true,
                    )
                )
            ),
            selectedPhotoIds = setOf(),
            gridSize = TimelineGridSize.Default,
            onGridSizeChange = {},
            onClick = {},
            onLongClick = {}
        )
    }
}

private const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
private const val DATE_FORMAT_MONTH = "LLLL"
private const val DATE_FORMAT_DAY = "dd"
private const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"

internal const val PHOTOS_NODE_GRID_VIEW_HEADER_BODY_TAG = "photos_node_grid_view:header_body"
internal const val PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_ICON_TAG =
    "header_body:icon_grid_size_settings"
internal const val PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_MENU_TAG =
    "header_body:menu_grid_size_settings"
internal const val PHOTOS_NODE_BODY_IMAGE_NODE_TAG = "photos_node_body:image_photos_node"
internal const val VIDEO_NODE_BODY_IMAGE_NODE_TAG = "photos_node_body:video_photos_node"
