package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.amber_400
import mega.privacy.android.shared.original.core.ui.theme.blue_400
import mega.privacy.android.shared.original.core.ui.theme.green_400
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun CameraUploadsStatusSync(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = Color.Transparent,
        onClick = onClick,
        modifier = modifier,
        progress = 0f,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_sync),
                contentDescription = "Camera uploads status sync",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusUploading(
    modifier: Modifier = Modifier,
    progress: Float,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = blue_400,
        onClick = onClick,
        modifier = modifier,
        progress = progress,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_uploading),
                contentDescription = "Camera uploads status uploading",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusCompleted(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CameraUploadsStatus(
        loadingIndicatorColor = green_400,
        onClick = onClick,
        modifier = modifier,
        progress = 1f,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_fab_status_completed),
                contentDescription = "Camera uploads status completed",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
fun CameraUploadsStatusWarning(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    progress: Float,
) {
    CameraUploadsStatus(
        loadingIndicatorColor = amber_400,
        onClick = onClick,
        modifier = modifier,
        progress = progress,
        statusIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_cu_status_warning),
                contentDescription = "Camera uploads status warning",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
        },
    )
}

@Composable
private fun CameraUploadsStatus(
    modifier: Modifier = Modifier,
    loadingIndicatorColor: Color,
    onClick: () -> Unit = {},
    progress: Float = 0f,
    statusIcon: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        backgroundColor = MaterialTheme.colors.surface,
        content = {
            CircularProgressIndicator(
                progress = progress,
                color = loadingIndicatorColor,
                strokeWidth = 2.dp,
            )

            statusIcon()
        },
    )
}

@Composable
fun CameraUploadsNoFullAccessBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val description = buildAnnotatedString {
        append(stringResource(sharedR.string.camera_uploads_banner_no_full_access_message))
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.camera_uploads_change_permissions))
        }
    }

    CameraUploadsBanner(
        modifier = modifier
            .clickable { onClick() }
            .testTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG),
        statusIcon = R.drawable.ic_cu_status_warning,
        title = stringResource(sharedR.string.camera_uploads_banner_complete_title),
        description = description,
        endIcon = {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                contentDescription = "Camera uploads banner end icon",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() },
                tint = IconColor.Primary,
            )
        }
    )
}

@Composable
fun EnableCameraUploadsBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    CameraUploadsBanner(
        modifier = modifier
            .clickable { onClick() }
            .testTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG),
        statusIcon = R.drawable.ic_cu_status,
        title = stringResource(R.string.settings_camera_upload_on),
        description = stringResource(id = R.string.enable_cu_subtitle),
        endIcon = {
            MegaIcon(
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                contentDescription = "Camera uploads banner end icon",
                modifier = Modifier
                    .size(24.dp),
                tint = IconColor.Primary,
            )
        }
    )
}

@Composable
fun CameraUploadsCheckingUploadsBanner(
    modifier: Modifier = Modifier,
) {
    CameraUploadsBanner(
        modifier = modifier.testTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG),
        statusIcon = R.drawable.ic_cu_status_sync,
        title = stringResource(sharedR.string.camera_uploads_banner_checking_uploads_text),
        description = null
    )
}

@Composable
fun CameraUploadsPendingCountBanner(
    count: Int,
    isTransferScreenAvailable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CameraUploadsBanner(
        modifier = modifier
            .clickable { onClick() }
            .testTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG),
        statusIcon = R.drawable.ic_cu_status_uploading,
        title = stringResource(
            sharedR.string.camera_uploads_banner_uploading_pending_count_text,
            count
        ),
        description = null,
        endIcon = {
            if (isTransferScreenAvailable) {
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                    contentDescription = "Camera uploads banner end icon",
                    modifier = Modifier
                        .size(24.dp),
                    tint = IconColor.Primary,
                )
            }
        }
    )
}

@Composable
private fun CameraUploadsBanner(
    @DrawableRes statusIcon: Int,
    title: String?,
    description: Any?,
    modifier: Modifier = Modifier,
    endIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = statusIcon),
                contentDescription = "Camera uploads banner status icon",
                modifier = Modifier
                    .padding(end = 15.dp)
                    .size(24.dp),
                tint = Color.Unspecified,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    if (title != null && description != null) {
                        Arrangement.spacedBy(5.dp)
                    } else {
                        Arrangement.Top
                    }
            ) {
                title?.let {
                    MegaText(
                        modifier = Modifier,
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.subtitle1,
                        text = title
                    )
                }

                description?.let {
                    when (it) {
                        is String -> {
                            MegaText(
                                modifier = Modifier,
                                textColor = TextColor.Secondary,
                                style = MaterialTheme.typography.body2,
                                text = it
                            )
                        }

                        is AnnotatedString -> {
                            MegaText(
                                text = it,
                                maxLines = Int.MAX_VALUE,
                                modifier = Modifier,
                                textColor = TextColor.Secondary,
                                style = MaterialTheme.typography.body2,
                            )
                        }
                    }
                }
            }

            endIcon?.let {
                Box(modifier = Modifier.padding(start = 10.dp)) {
                    endIcon()
                }
            }
        }
        MegaDivider(
            dividerType = DividerType.FullSize,
            strong = true
        )
    }
}

@Preview
@Composable
fun PreviewCameraUploadsStatusSync() {
    CameraUploadsStatusSync(onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusUploading() {
    CameraUploadsStatusUploading(progress = 0.6f, onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusCompleted() {
    CameraUploadsStatusCompleted(onClick = { /*TODO*/ })
}

@Preview
@Composable
fun PreviewCameraUploadsStatusWarning() {
    CameraUploadsStatusWarning(progress = 0.4f, onClick = { /*TODO*/ })
}

/**
 * Test tag for enable camera uploads banner
 */
const val TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG =
    "timeline_enable_camera_uploads_banner_test_tag"

/**
 * Test tag for camera uploads checking uploads banner
 */
const val TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG =
    "timeline_camera_uploads_checking_uploads_banner_test_tag"

/**
 * Test tag for camera uploads pending count banner
 */
const val TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG =
    "timeline_camera_uploads_pending_count_banner_test_tag"

/**
 * Test tag for camera uploads no full access banner
 */
const val TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG =
    "timeline_camera_uploads_no_full_access_banner_test_tag"
