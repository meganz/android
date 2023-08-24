package mega.privacy.android.app.presentation.meeting.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
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
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.core.ui.controls.buttons.ToggleMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.grey_900
import kotlin.random.Random


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
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    var showLeaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.onSurface
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
                onClick = { showLeaveDialog = true },
                modifier = Modifier
                    .testTag("waiting_room:button_close")
                    .constrainAs(closeButton) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(mega.privacy.android.core.R.drawable.ic_universal_close),
                    contentDescription = "Waiting Room Close button",
                    tint = MaterialTheme.colors.onSurface
                )
            }

            IconButton(
                onClick = onInfoClicked,
                modifier = Modifier
                    .testTag("waiting_room:button_info")
                    .constrainAs(infoButton) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(mega.privacy.android.core.R.drawable.ic_info),
                    contentDescription = "Waiting Room Info button",
                    tint = MaterialTheme.colors.onSurface,
                )
            }

            Text(
                text = state.title ?: stringResource(R.string.general_loading),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("waiting_room:text_title")
                    .constrainAs(titleText) {
                        top.linkTo(parent.top, 60.dp)
                        linkTo(parent.start, parent.end)
                    }
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = state.title.isNullOrBlank(),
                    ),
            )

            Text(
                text = state.formattedTimestamp ?: stringResource(R.string.unknown_name_label),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("waiting_room:text_timestamp")
                    .constrainAs(timestampText) {
                        top.linkTo(titleText.bottom, 2.dp)
                        linkTo(parent.start, parent.end)
                    }
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = state.formattedTimestamp.isNullOrBlank(),
                    ),
            )

            Chip(
                onClick = {},
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.onSurface,
                    contentColor = MaterialTheme.colors.surface,
                ),
                modifier = Modifier
                    .testTag("waiting_room:text_alert")
                    .constrainAs(alertText) {
                        top.linkTo(timestampText.bottom, 24.dp)
                        linkTo(parent.start, parent.end)
                    }
            ) {
                Text(
                    text = stringResource(
                        if (state.hasStarted)
                            R.string.meetings_waiting_room_wait_for_host_to_let_you_in_label
                        else
                            R.string.meetings_waiting_room_wait_for_host_to_start_meeting_label
                    ),
                    style = MaterialTheme.typography.subtitle2medium,
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp)
                )
            }

            Box(modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color = grey_900)
                .constrainAs(videoPreview) {
                    top.linkTo(alertText.bottom, 24.dp)
                    linkTo(parent.start, parent.end, startMargin = 70.dp, endMargin = 70.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.ratio("55:83")
                }
            ) {
                if (state.cameraEnabled) {
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("waiting_room:preview_camera")
                    )
                } else {
                    ChatAvatarView(
                        modifier = Modifier
                            .size(88.dp)
                            .align(Alignment.Center)
                            .testTag("waiting_room:image_avatar"),
                        avatars = state.avatar?.let(::listOf)
                    )
                }
            }

            ToggleMegaButton(
                modifier = Modifier
                    .testTag("waiting_room:button_mic")
                    .constrainAs(micButton) {
                        top.linkTo(videoPreview.bottom, margin = 70.dp)
                        linkTo(videoPreview.start, cameraButton.start)
                    },
                checked = state.micEnabled,
                enabled = context.hasMicrophonePermissions(),
                title = stringResource(R.string.general_mic),
                enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_on,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_off,
                onCheckedChange = onMicToggleChange,
            )

            ToggleMegaButton(
                modifier = Modifier
                    .testTag("waiting_room:button_camera")
                    .constrainAs(cameraButton) {
                        top.linkTo(videoPreview.bottom, margin = 70.dp)
                        linkTo(micButton.end, speakerButton.start)
                    },
                checked = state.cameraEnabled,
                enabled = context.hasCameraPermissions(),
                title = stringResource(R.string.general_camera),
                enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_video_on,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_video_off,
                onCheckedChange = onCameraToggleChange,
            )

            ToggleMegaButton(
                modifier = Modifier
                    .testTag("waiting_room:button_speaker")
                    .constrainAs(speakerButton) {
                        top.linkTo(videoPreview.bottom, margin = 70.dp)
                        linkTo(cameraButton.end, videoPreview.end)
                    },
                checked = state.speakerEnabled,
                title = stringResource(R.string.general_speaker),
                enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_max,
                disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_off,
                onCheckedChange = onSpeakerToggleChange,
            )

            createHorizontalChain(
                micButton,
                cameraButton,
                speakerButton,
                chainStyle = ChainStyle.Spread,
            )
        }

        if (showLeaveDialog) {
            MegaAlertDialog(
                modifier = Modifier.testTag("waiting_room:dialog_leave"),
                text = stringResource(R.string.meetings_leave_meeting_confirmation_dialog_title),
                confirmButtonText = stringResource(R.string.general_leave),
                cancelButtonText = stringResource(R.string.meetings__waiting_room_leave_meeting_dialog_cancel_button),
                onConfirm = onCloseClicked,
                onDismiss = { showLeaveDialog = false },
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

private fun Context.hasCameraPermissions(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED

private fun Context.hasMicrophonePermissions(): Boolean =
    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun PreviewWaitingRoomView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WaitingRoomView(
            state = WaitingRoomState(
                chatId = -1,
                schedId = -1,
                hasStarted = Random.nextBoolean(),
                title = "Book Club",
                formattedTimestamp = "Monday, 30 May · 10:25 -11:25",
            ),
            onInfoClicked = {},
            onCloseClicked = {},
            onMicToggleChange = {},
            onCameraToggleChange = {},
            onSpeakerToggleChange = {},
        )
    }
}
