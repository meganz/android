package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR

@Composable
fun CameraUploadsStatusIcon(
    type: CameraUploadsStatusType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    Box(modifier = modifier.clickable(onClick = onClick)) {
        CameraUploadsIcon(
            type = type,
            progress = progress
        )

        StatusIcon(
            modifier = Modifier.align(Alignment.BottomEnd),
            type = type,
            progress = progress
        )
    }
}

@Composable
private fun CameraUploadsIcon(
    type: CameraUploadsStatusType,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        progress?.let {
            val color = when (type) {
                CameraUploadsStatusType.Uploading -> {
                    if (it() == 1F) {
                        DSTokens.colors.support.success
                    } else DSTokens.colors.support.info
                }

                else -> Color.Transparent
            }
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                progress = it,
                color = color,
                strokeWidth = 1.5.dp,
                trackColor = Color.Transparent
            )
        } ?: run {
            Box(modifier = Modifier.size(28.dp))
        }

        val iconResId = when (type) {
            CameraUploadsStatusType.Default -> IconPackR.drawable.ic_cu_status_default
            else -> IconPackR.drawable.ic_cu_status_medium_thin_outline
        }
        MegaIcon(
            modifier = Modifier.size(
                size = if (type == CameraUploadsStatusType.Uploading) 22.dp else 24.dp
            ),
            imageVector = ImageVector.vectorResource(iconResId),
            tint = IconColor.Primary,
        )
    }
}

@Composable
private fun StatusIcon(
    type: CameraUploadsStatusType,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    if (type != CameraUploadsStatusType.Default) {
        val color = when (type) {
            CameraUploadsStatusType.Uploading -> {
                if (progress?.invoke() == 1F) {
                    DSTokens.colors.support.success
                } else DSTokens.colors.support.info
            }

            CameraUploadsStatusType.Warning -> DSTokens.colors.support.warning
            else -> DSTokens.colors.icon.secondary
        }
        val iconResId = when (type) {
            CameraUploadsStatusType.Sync -> IconPackR.drawable.ic_cu_status_sync_medium_thin_solid
            CameraUploadsStatusType.Uploading -> {
                if (progress?.invoke() == 1F) {
                    IconPackR.drawable.ic_cu_status_check_medium_thin_solid
                } else IconPackR.drawable.ic_cu_status_arrow_up_medium_thin_solid
            }

            CameraUploadsStatusType.Complete -> IconPackR.drawable.ic_cu_status_check_medium_thin_solid
            CameraUploadsStatusType.Pause -> IconPackR.drawable.ic_cu_status_pause_medium_thin_solid
            else -> IconPackR.drawable.ic_cu_status_warning_medium_thin_solid
        }
        Icon(
            modifier = modifier
                .background(
                    color = DSTokens.colors.background.pageBackground,
                    shape = CircleShape
                )
                .padding(1.dp)
                .size(12.dp),
            imageVector = ImageVector.vectorResource(iconResId),
            tint = color,
            contentDescription = null
        )
    }
}

enum class CameraUploadsStatusType {
    Default,
    Sync,
    Uploading,
    Pause,
    Complete,
    Warning
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsStatusIconPreview(
    @PreviewParameter(CameraUploadsStatusTypePreviewParameterProvider::class) type: CameraUploadsStatusType,
) {
    AndroidThemeForPreviews {
        CameraUploadsStatusIcon(
            type = type,
            onClick = {},
            progress = if (type == CameraUploadsStatusType.Uploading) {
                { 0.5F }
            } else null
        )
    }
}

private class CameraUploadsStatusTypePreviewParameterProvider :
    PreviewParameterProvider<CameraUploadsStatusType> {
    override val values = CameraUploadsStatusType.entries.asSequence()
}

@CombinedThemePreviews
@Composable
private fun CameraUploadsStatusIcon100Preview() {
    AndroidThemeForPreviews {
        CameraUploadsStatusIcon(
            type = CameraUploadsStatusType.Uploading,
            onClick = {},
            progress = { 1F }
        )
    }
}
