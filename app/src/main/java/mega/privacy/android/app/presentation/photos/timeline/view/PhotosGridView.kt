package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.shared.original.core.ui.controls.layouts.FastScrollLazyVerticalGrid
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
const val DATE_FORMAT_MONTH = "LLLL"
const val DATE_FORMAT_DAY = "dd"
const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"

@Composable
fun PhotosGridView(
    modifier: Modifier,
    timelineViewState: TimelineViewState = TimelineViewState(),
    downloadPhoto: PhotoDownload,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    onEnableCameraUploads: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onCloseCameraUploadsLimitedAccess: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val spanCount = remember(configuration.orientation, timelineViewState.currentZoomLevel) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            timelineViewState.currentZoomLevel.portrait
        } else {
            timelineViewState.currentZoomLevel.landscape
        }
    }

    val enableCameraUploadsBanner =
        timelineViewState.enableCameraUploadButtonShowing
                && !timelineViewState.showCameraUploadsWarning
                && timelineViewState.selectedPhotoCount == 0
    val isCameraUploadsLimitedAccess = timelineViewState.isCameraUploadsLimitedAccess

    val context = LocalContext.current
    val uiPhotoList = timelineViewState.photosListItems
    val currentZoomLevel = timelineViewState.currentZoomLevel
    val potentialItems =
        if (enableCameraUploadsBanner) 1 else 0 + if (isCameraUploadsLimitedAccess) 1 else 0
    val totalItems = potentialItems + uiPhotoList.size + 1

    FastScrollLazyVerticalGrid(
        totalItems = totalItems,
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.fillMaxSize(),
        state = lazyGridState,
        tooltipText = { index ->
            val item = uiPhotoList.getOrNull(index)
            item?.let {
                val modificationTime = when (it) {
                    is PhotoListItem.Separator -> it.modificationTime
                    is PhotoListItem.PhotoGridItem -> it.photo.modificationTime
                }
                dateText(
                    modificationTime = modificationTime,
                    currentZoomLevel = currentZoomLevel,
                    locale = context.resources.configuration.locales[0],
                )
            } ?: ""
        },
    ) {
        if (isCameraUploadsLimitedAccess) {
            item(
                key = "camera-uploads-limited-access-banner",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                CameraUploadsLimitedAccess(
                    onClick = onChangeCameraUploadsPermissions,
                    onClose = onCloseCameraUploadsLimitedAccess,
                )
            }
        }

        if (enableCameraUploadsBanner) {
            item(
                key = "enable-camera-uploads-banner",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                NewEnableCameraUploadsButton(onClick = onEnableCameraUploads)
            }
        }

        this.items(
            items = uiPhotoList,
            key = {
                it.key
            },
            span = {
                if (it is PhotoListItem.Separator)
                    GridItemSpan(maxLineSpan)
                else GridItemSpan(1)
            },
        ) { item ->

            if (item is PhotoListItem.PhotoGridItem) {
                PhotoView(
                    photo = item.photo,
                    isSelected = item.isSelected,
                    currentZoomLevel = timelineViewState.currentZoomLevel,
                    accountType = timelineViewState.accountType,
                    onClick = onClick,
                    onLongPress = onLongPress,
                    downloadPhoto = downloadPhoto,
                )
            } else if (item is PhotoListItem.Separator) {
                Text(
                    text = dateText(
                        modificationTime = item.modificationTime,
                        currentZoomLevel = timelineViewState.currentZoomLevel,
                        locale = context.resources.configuration.locales[0],
                    ),
                    textAlign = TextAlign.Start,
                    color = colorResource(id = R.color.grey_087_white_087),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                )
            }
        }

        item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(50.dp)
            )
        }
    }
}

private fun dateText(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (currentZoomLevel == ZoomLevel.Grid_1) {
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

