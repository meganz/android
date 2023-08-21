package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.core.ui.controls.buttons.ToggleMegaButton
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.grey_900
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white_alpha_087


/**
 * Waiting room View
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun WaitingRoomView(
    state: WaitingRoomState,
    onInfoClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    onMicToggleChange: (Boolean) -> Unit,
    onCameraToggleChange: (Boolean) -> Unit,
    onSpeakerToggleChange: (Boolean) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = grey_alpha_087
                )
            }
        }
    ) { paddingValues ->
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            val (closeButton, infoButton, titleText, timestampText, alertText, videoPreview, micButton, cameraButton, speakerButton) = createRefs()

            IconButton(
                onClick = onCloseClicked,
                modifier = Modifier.constrainAs(closeButton) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                    contentDescription = "Close waiting room button",
                    tint = white_alpha_087
                )
            }

            IconButton(
                onClick = onInfoClicked,
                modifier = Modifier.constrainAs(infoButton) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = ImageVector.vectorResource(mega.privacy.android.core.R.drawable.ic_info),
                    contentDescription = "Waiting room info button",
                    tint = white_alpha_087,
                )
            }

            Text(
                text = state.title,
                style = MaterialTheme.typography.h6,
                color = white_alpha_087,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(titleText) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top, 60.dp)
                }
            )

            Text(
                text = state.formattedTimestamp,
                style = MaterialTheme.typography.subtitle2,
                color = white_alpha_087,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(timestampText) {
                    top.linkTo(titleText.bottom, 2.dp)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                }
            )

            Chip(
                onClick = {},
                colors = ChipDefaults.chipColors(
                    backgroundColor = white_alpha_087,
                    contentColor = grey_alpha_087,
                ),
                modifier = Modifier.constrainAs(alertText) {
                    top.linkTo(timestampText.bottom, 24.dp)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                }
            ) {
                Text(
                    text = stringResource(R.string.meetings_waiting_room_wait_for_host_to_let_you_in_label),
                    style = MaterialTheme.typography.subtitle2medium,
                )
            }

            Box(modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color = grey_900)
                .constrainAs(videoPreview) {
                    linkTo(parent.start, parent.end, startMargin = 70.dp, endMargin = 70.dp)
                    top.linkTo(alertText.bottom, 24.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.ratio("55:83")
                }
            ) {
                if (state.cameraEnabled) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                } else {
                    Image(
                        modifier = Modifier
                            .size(88.dp)
                            .align(Alignment.Center),
                        bitmap = ImageBitmap.imageResource(R.drawable.ic_guest_avatar),
                        contentDescription = "Camera video feed",
                    )
                }
            }

            ToggleMegaButton(
                modifier = Modifier.constrainAs(micButton) {
                    top.linkTo(videoPreview.bottom, margin = 70.dp)
                    linkTo(videoPreview.start, cameraButton.start)
                },
                title = stringResource(R.string.general_mic),
                enable = state.micEnabled,
                enabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_mic_on,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_mic_off,
                onCheckedChange = onMicToggleChange,
            )

            ToggleMegaButton(
                modifier = Modifier.constrainAs(cameraButton) {
                    top.linkTo(videoPreview.bottom, margin = 70.dp)
                    linkTo(micButton.end, speakerButton.start)
                },
                title = stringResource(R.string.general_camera),
                enable = state.cameraEnabled,
                enabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_video_on,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_video_off,
                onCheckedChange = onCameraToggleChange,
            )

            ToggleMegaButton(
                modifier = Modifier.constrainAs(speakerButton) {
                    top.linkTo(videoPreview.bottom, margin = 70.dp)
                    linkTo(cameraButton.end, videoPreview.end)
                },
                title = stringResource(R.string.general_speaker),
                enable = state.speakerEnabled,
                enabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_max,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_waiting_room_off,
                onCheckedChange = onSpeakerToggleChange,
            )

            createHorizontalChain(
                micButton,
                cameraButton,
                speakerButton,
                chainStyle = ChainStyle.Spread,
            )
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                controller = LifecycleCameraController(context).apply {
                    this.bindToLifecycle(lifecycleOwner)
                    this.cameraSelector = cameraSelector
                }
            }
        }
    )
}

/**
 * Waiting Room View Preview
 */
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewWaitingRoomView() {
    WaitingRoomView(
        state = WaitingRoomState(
            chatId = -1,
            schedId = -1,
            title = "Test title"
        ),
        onInfoClicked = {},
        onCloseClicked = {},
        onMicToggleChange = {},
        onCameraToggleChange = {},
        onSpeakerToggleChange = {},
    )
}
