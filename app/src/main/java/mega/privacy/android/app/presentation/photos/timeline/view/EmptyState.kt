package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState

/**
 * Compose function to handle empty states
 */
@Composable
fun EmptyState(
    timelineViewState: TimelineViewState = TimelineViewState(),
    isNewCUEnabled: Boolean,
    setEnableCUPage: (Boolean) -> Unit = {},
    onEnableCameraUploads: () -> Unit = {},
) {
    val enableCameraUploadButtonShowing = timelineViewState.enableCameraUploadButtonShowing
    val currentMediaSource = timelineViewState.currentMediaSource

    if (enableCameraUploadButtonShowing && currentMediaSource != CLOUD_DRIVE) {
        setEnableCUPage(true)
    } else if (LocalConfiguration.current.orientation == ORIENTATION_PORTRAIT) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyStateContent(timelineViewState)
        }

        if (enableCameraUploadButtonShowing && isNewCUEnabled) {
            NewEnableCameraUploadsButton(
                onClick = onEnableCameraUploads,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (enableCameraUploadButtonShowing && isNewCUEnabled) {
                NewEnableCameraUploadsButton(
                    onClick = onEnableCameraUploads,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            EmptyStateContent(timelineViewState)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyStateContent(timelineViewState: TimelineViewState) {
    Image(
        imageVector = when (timelineViewState.currentFilterMediaType) {
            FilterMediaType.ALL_MEDIA, FilterMediaType.IMAGES ->
                ImageVector.vectorResource(id = R.drawable.ic_no_images)

            FilterMediaType.VIDEOS ->
                ImageVector.vectorResource(id = R.drawable.ic_no_videos)
        },
        contentDescription = "Empty",
        colorFilter = ColorFilter.tint(
            color = if (MaterialTheme.colors.isLight) {
                Color(0xFFDADADA)
            } else {
                Color(0xFFEAEFEF)
            }
        ),
        alpha = if (MaterialTheme.colors.isLight) 1F else 0.16F
    )

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(top = 42.dp)
    ) {

        val placeHolderStart = "[B]"
        val placeHolderEnd = "[/B]"

        val text: String = when (timelineViewState.currentFilterMediaType) {
            FilterMediaType.ALL_MEDIA ->
                stringResource(id = R.string.timeline_empty_media)

            FilterMediaType.IMAGES ->
                stringResource(id = R.string.timeline_empty_images)

            FilterMediaType.VIDEOS ->
                stringResource(id = R.string.timeline_empty_videos)
        }

        Text(
            text = text.substring(0, text.indexOf(placeHolderStart)),
            color = colorResource(id = R.color.grey_054_white_054),
        )

        Text(
            text = text.substring(
                text.indexOf(placeHolderStart),
                text.indexOf(placeHolderEnd)
            ).replace("[B]", ""),
            color = colorResource(id = R.color.grey_087_white_087),
            fontWeight = FontWeight.ExtraBold,
        )

        Text(
            text = text.substring(text.indexOf(placeHolderEnd)).replace("[/B]", ""),
            color = colorResource(id = R.color.grey_054_white_054),
        )
    }
}
