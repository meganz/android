package mega.privacy.android.feature.photos.presentation.component

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.components.ImagePhotosNode
import mega.privacy.android.feature.photos.components.PhotosNodeThumbnailData
import mega.privacy.android.feature.photos.components.VideoPhotosNode
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.PhotosNodeContentType.DateItem
import mega.privacy.android.feature.photos.model.PhotosNodeContentType.PhotoNodeItem
import mega.privacy.android.feature.photos.model.ZoomLevel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.time.Duration

@Composable
fun PhotosNodeGridView(
    items: ImmutableList<PhotosNodeContentType>,
    zoomLevel: ZoomLevel,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onClick: (node: PhotoNodeUiState) -> Unit,
    onLongClick: (node: PhotoNodeUiState) -> Unit,
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    header: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var userScrollEnabled by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val spanCount = remember(key1 = configuration.orientation, key2 = zoomLevel) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            zoomLevel.portrait
        } else {
            zoomLevel.landscape
        }
    }
    val isPreview by remember(configuration, zoomLevel) {
        derivedStateOf { isPreview(configuration, zoomLevel) }
    }

    FastScrollLazyVerticalGrid(
        totalItems = items.size,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier
            .fillMaxSize()
            .screenContentZoomGestureDetector(
                onZoom = { type ->
                    scope.launch {
                        userScrollEnabled = false
                        when (type) {
                            ZoomType.ZoomIn -> onZoomIn()
                            ZoomType.ZoomOut -> onZoomOut()
                        }
                        // Delay to disable user scrolling while the grid is being updated
                        delay(250)
                        userScrollEnabled = true
                    }
                }
            ),
        state = lazyGridState,
        tooltipText = { index ->
            val item = items.getOrNull(index)
            item?.let {
                val modificationTime = when (it) {
                    is DateItem -> it.time
                    is PhotoNodeItem -> it.node.photo.modificationTime
                }
                dateText(
                    modificationTime = modificationTime,
                    zoomLevel = zoomLevel,
                    locale = configuration.locales[0],
                )
            }.orEmpty()
        },
        userScrollEnabled = userScrollEnabled,
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

        items(
            items = items,
            key = { it.key },
            contentType = { it },
            span = { contentType ->
                when (contentType) {
                    is DateItem -> GridItemSpan(maxLineSpan)
                    is PhotoNodeItem -> GridItemSpan(1)
                }
            }
        ) { contentType ->
            when (contentType) {
                is DateItem -> {
                    DateBody(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag(PHOTOS_NODE_GRID_VIEW_DATE_BODY_TAG),
                        time = contentType.time,
                        zoomLevel = zoomLevel
                    )
                }

                is PhotoNodeItem -> {
                    PhotoNodeBody(
                        modifier = Modifier
                            .animateItem()
                            .combinedClickable(
                                onClick = { onClick(contentType.node) },
                                onLongClick = { onLongClick(contentType.node) }
                            ),
                        spanCount = spanCount,
                        node = contentType.node,
                        isPreview = isPreview,
                        isSelected = contentType.node.isSelected,
                        shouldShowFavourite = contentType.node.photo.isFavourite && zoomLevel == ZoomLevel.Grid_1
                                || contentType.node.photo.isFavourite && zoomLevel == ZoomLevel.Grid_3
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
private fun DateBody(
    time: LocalDateTime,
    zoomLevel: ZoomLevel,
    modifier: Modifier = Modifier,
) {
    val locales = LocalConfiguration.current.locales
    MegaText(
        modifier = modifier,
        text = dateText(
            modificationTime = time,
            zoomLevel = zoomLevel,
            locale = locales[0],
        ),
        style = AppTheme.typography.titleSmall,
        textColor = TextColor.Secondary
    )
}

@Composable
private fun PhotoNodeBody(
    spanCount: Int,
    node: PhotoNodeUiState,
    isPreview: Boolean,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val photosNodeSize = remember(spanCount) {
        with(density) {
            (windowInfo.containerSize.width / spanCount).toDp()
        }
    }
    val downloadResult by node.photo.downloadAsStateWithLifecycle(isPreview = isPreview)
    val thumbnailData by remember(downloadResult) {
        mutableStateOf(
            value = when (downloadResult) {
                is DownloadPhotoResult.Success -> {
                    val path = if (isPreview) {
                        node.photo.previewFilePath
                    } else {
                        node.photo.thumbnailFilePath
                    }
                    path?.let {
                        PhotosNodeThumbnailData.File(
                            path = it,
                            isSensitive = node.isSensitive
                        )
                    } ?: run {
                        PhotosNodeThumbnailData.Placeholder(imageResId = node.defaultIcon)
                    }
                }

                else -> PhotosNodeThumbnailData.Placeholder(imageResId = node.defaultIcon)
            }
        )
    }
    when (node.photo) {
        is PhotoUiState.Image -> {
            ImagePhotosNode(
                modifier = modifier
                    .size(photosNodeSize)
                    .testTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG),
                thumbnailData = thumbnailData,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite
            )
        }

        is PhotoUiState.Video -> {
            VideoPhotosNode(
                modifier = modifier
                    .size(photosNodeSize)
                    .testTag(VIDEO_NODE_BODY_IMAGE_NODE_TAG),
                duration = node.photo.duration,
                thumbnailData = thumbnailData,
                isSelected = isSelected,
                shouldShowFavourite = shouldShowFavourite
            )
        }
    }
}

private fun isPreview(configuration: Configuration, zoomLevel: ZoomLevel) =
    configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            && zoomLevel.portrait == ZoomLevel.Grid_1.portrait

enum class ZoomType {
    ZoomIn, ZoomOut
}

@SuppressLint("SuspiciousModifierThen")
private fun Modifier.screenContentZoomGestureDetector(onZoom: (type: ZoomType) -> Unit) = then(
    other = pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent(
                    pass = PointerEventPass.Initial
                )
                if (event.changes.any { it.isConsumed })
                    break
                val zoomChange = event.calculateZoom()
                if (zoomChange != 1.0f) {
                    if (zoomChange > 1.0f) {
                        onZoom(ZoomType.ZoomIn)
                    } else {
                        onZoom(ZoomType.ZoomOut)
                    }
                    // Consume event in case to trigger scroll
                    event.changes.map { it.consume() }
                    break
                }
            } while (event.changes.any { it.pressed })
        }
    }
)

private fun dateText(
    zoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (zoomLevel == ZoomLevel.Grid_1) {
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
        CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
            PhotosNodeGridView(
                items = persistentListOf(
                    DateItem(time = LocalDateTime.now()),
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
                            defaultIcon = mega.privacy.android.icon.pack.R.drawable.ic_3d_medium_solid,
                            isSensitive = false,
                            isSelected = true
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
                            defaultIcon = mega.privacy.android.icon.pack.R.drawable.ic_3d_medium_solid,
                            isSensitive = true,
                            isSelected = false
                        )
                    )
                ),
                zoomLevel = ZoomLevel.Grid_3,
                onZoomIn = {},
                onZoomOut = {},
                onClick = {},
                onLongClick = {}
            )
        }
    }
}

private const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
private const val DATE_FORMAT_MONTH = "LLLL"
private const val DATE_FORMAT_DAY = "dd"
private const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"

internal const val PHOTOS_NODE_GRID_VIEW_DATE_BODY_TAG = "photos_node_grid_view:date_body"
internal const val PHOTOS_NODE_BODY_IMAGE_NODE_TAG = "photos_node_body:image_photos_node"
internal const val VIDEO_NODE_BODY_IMAGE_NODE_TAG = "photos_node_body:video_photos_node"
