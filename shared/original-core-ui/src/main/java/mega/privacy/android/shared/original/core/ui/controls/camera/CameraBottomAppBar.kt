package mega.privacy.android.shared.original.core.ui.controls.camera

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Camera bottom app bar
 *
 * @param isCaptureVideo true if the camera is in video mode
 * @param isRecording true if the camera is recording
 * @param modifier
 * @param onCameraAction action to be executed when the camera button is clicked
 * @param onToggleCaptureMode action to be executed when the capture mode is toggled
 * @param onToggleCamera action to be executed when the camera is toggled
 */
@Composable
fun CameraBottomAppBar(
    rotationDegree: Float,
    isCaptureVideo: Boolean,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onCameraAction: () -> Unit = {},
    onToggleCaptureMode: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onOpenGallery: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .background(color = DSTokens.colors.background.pageBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(top = 32.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InternalAnimatedContent(visible = !isRecording) {
                CameraButton(
                    modifier = Modifier.testTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_GALLERY),
                    rotationDegree = rotationDegree,
                    iconResId = R.drawable.ic_gallery,
                    onClick = onOpenGallery,
                )
            }
            CaptureIcon(
                modifier = Modifier.testTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_CAMERA),
                isCaptureVideo = isCaptureVideo,
                isRecording = isRecording,
                onClick = onCameraAction,
            )
            InternalAnimatedContent(visible = !isRecording) {
                CameraButton(
                    modifier = Modifier.testTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_ROTATE),
                    rotationDegree = rotationDegree,
                    iconResId = mega.privacy.android.icon.pack.R.drawable.ic_sync_2_medium_thin_outline,
                    onClick = onToggleCamera,
                )
            }
        }

        InternalAnimatedContent(visible = !isRecording) {
            val pillWidth = 84.dp
            val textWidth = 88.dp // Account for pill width and spacing
            val offset by animateDpAsState(
                targetValue = if (isCaptureVideo) -textWidth else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PhotoVideoScrollerOffset"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(36.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = DSTokens.colors.border.subtle,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .background(DSTokens.colors.background.surface2)
                )
                Row(
                    modifier = Modifier
                        .offset(x = offset),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(textWidth))
                    TextButton(
                        text = stringResource(id = R.string.camera_photo_button),
                        isSelected = !isCaptureVideo,
                        textWidth = textWidth,
                        onClick = onToggleCaptureMode,
                        modifier = Modifier.testTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO)
                    )
                    TextButton(
                        text = stringResource(id = sharedR.string.record_video_button_text),
                        isSelected = isCaptureVideo,
                        textWidth = textWidth,
                        onClick = onToggleCaptureMode,
                        modifier = Modifier.testTag(TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextButton(
    text: String,
    isSelected: Boolean,
    textWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = text,
        textColor = TextColor.Primary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body2medium.copy(
            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400
        ),
        modifier = modifier
            .width(textWidth)
            .clickable(
                enabled = !isSelected,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
    )
}

@Composable
private fun InternalAnimatedContent(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        content()
    }
}

@Composable
private fun CaptureIcon(
    isCaptureVideo: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(2.dp, DSTokens.colors.background.inverse, CircleShape)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .background(
                        color = DSTokens.colors.components.interactive,
                        shape = RoundedCornerShape(6.dp),
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(
                        color = DSTokens.colors.background.inverse,
                        shape = CircleShape
                    )
            ) {
                if (isCaptureVideo) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                            .background(
                                color = DSTokens.colors.components.interactive,
                                shape = CircleShape,
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraButton(
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
    rotationDegree: Float = 0f,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .rotate(rotationDegree)
            .size(48.dp)
            .clip(CircleShape)
            .background(color = DSTokens.colors.background.surface2, shape = CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = iconResId),
            contentDescription = "Camera button",
            tint = DSTokens.colors.icon.onColor
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraBottomAppBarPreview(
    @PreviewParameter(BooleanProvider::class) isVideoSelected: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CameraBottomAppBar(
            isCaptureVideo = isVideoSelected,
            isRecording = false,
            rotationDegree = 0f,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecordingCameraBottomAppBarPreview(
    @PreviewParameter(BooleanProvider::class) isVideoSelected: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CameraBottomAppBar(
            isCaptureVideo = isVideoSelected,
            isRecording = true,
            rotationDegree = 0f,
        )
    }
}

internal const val TEST_TAG_CAMERA_BOTTOM_APP_BAR_GALLERY = "camera_bottom_app_bar:gallery"
internal const val TEST_TAG_CAMERA_BOTTOM_APP_BAR_CAMERA = "camera_bottom_app_bar:camera"
internal const val TEST_TAG_CAMERA_BOTTOM_APP_BAR_ROTATE = "camera_bottom_app_bar:rotate"
internal const val TEST_TAG_CAMERA_BOTTOM_APP_BAR_PHOTO = "camera_bottom_app_bar:photo"
internal const val TEST_TAG_CAMERA_BOTTOM_APP_BAR_VIDEO = "camera_bottom_app_bar:video"
