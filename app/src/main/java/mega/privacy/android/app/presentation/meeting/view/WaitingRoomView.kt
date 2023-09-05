package mega.privacy.android.app.presentation.meeting.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.ToggleMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.core.ui.controls.video.MegaVideoTextureView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.grey_900
import org.jetbrains.anko.landscape
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
    onGuestNameChange: (String, String) -> Unit,
    videoStream: Flow<Pair<Size, ByteArray>>,
) {
    val isLandscape = LocalConfiguration.current.landscape
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    var showGuestUi by rememberSaveable { mutableStateOf(state.isGuestMode()) }
    var firstName by rememberSaveable { mutableStateOf(state.guestFirstName ?: "") }
    var lastName by rememberSaveable { mutableStateOf(state.guestLastName ?: "") }

    Scaffold(
        scaffoldState = rememberScaffoldState(),
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
                .verticalScroll(rememberScrollState(), false)
        ) {
            val (closeButton, infoButton, titleText, timestampText, alertText, videoPreview, guestBackground, guestInputs, controls, joinButton) = createRefs()
            val topGuideline = createGuidelineFromTop(if (isLandscape) 0.06f else 0.08f)
            val videoStartGuideline = createGuidelineFromStart(if (isLandscape) 0.29f else 0.16f)
            val videoEndGuideline = createGuidelineFromEnd(if (isLandscape) 0.29f else 0.16f)
            val guestGuideline = if (isLandscape) {
                timestampText.bottom
            } else {
                createGuidelineFromBottom(0.4f)
            }

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
                        top.linkTo(topGuideline)
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
                        visibility = if (showGuestUi)
                            Visibility.Invisible
                        else
                            Visibility.Visible
                    }
            ) {
                Text(
                    text = stringResource(
                        if (state.callStarted)
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
                    linkTo(videoStartGuideline, videoEndGuideline)
                    width = Dimension.fillToConstraints
                    if (isLandscape) {
                        height = Dimension.fillToConstraints
                        bottom.linkTo(parent.bottom)
                    } else {
                        height = Dimension.ratio("55:83")
                    }
                }
            ) {
                if (state.cameraEnabled) {
                    MegaVideoTextureView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("waiting_room:preview_camera"),
                        videoStream = videoStream,
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

            if (showGuestUi) {
                Box(modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .testTag("waiting_room:guest_background")
                    .constrainAs(guestBackground) {
                        top.linkTo(guestGuideline)
                        bottom.linkTo(parent.bottom)
                        linkTo(parent.start, parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
                )

                GuestNameInputText(
                    firstName = firstName,
                    lastName = lastName,
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    modifier = Modifier
                        .testTag("waiting_room:guest_name_inputs")
                        .constrainAs(guestInputs) {
                            top.linkTo(guestBackground.top, 8.dp)
                            linkTo(videoStartGuideline, videoEndGuideline)
                            width = Dimension.fillToConstraints
                        },
                )
            }

            ToggleControlsRow(
                micEnabled = state.micEnabled,
                cameraEnabled = state.cameraEnabled,
                speakerEnabled = state.speakerEnabled,
                onMicToggleChange = onMicToggleChange,
                onCameraToggleChange = onCameraToggleChange,
                onSpeakerToggleChange = onSpeakerToggleChange,
                modifier = Modifier
                    .testTag("waiting_room:toggle_controls")
                    .constrainAs(controls) {
                        if (showGuestUi) {
                            top.linkTo(guestInputs.bottom, 25.dp)
                        } else if (isLandscape) {
                            bottom.linkTo(parent.bottom, 12.dp)
                        } else {
                            top.linkTo(videoPreview.bottom, 65.dp)
                        }
                        linkTo(videoStartGuideline, videoEndGuideline)
                        width = Dimension.fillToConstraints
                    }
            )

            if (showGuestUi) {
                RaisedDefaultMegaButton(
                    textId = R.string.action_join,
                    enabled = firstName.isNotBlank() && lastName.isNotBlank(),
                    onClick = {
                        onGuestNameChange(firstName, lastName)
                        showGuestUi = false
                    },
                    modifier = Modifier
                        .testTag("waiting_room:button_join")
                        .constrainAs(joinButton) {
                            top.linkTo(controls.bottom, if (isLandscape) 30.dp else 40.dp)
                            linkTo(videoStartGuideline, videoEndGuideline)
                            width = Dimension.fillToConstraints
                        }
                )
            }
        }
    }

    when {
        showLeaveDialog -> MegaAlertDialog(
            modifier = Modifier.testTag("waiting_room:dialog_leave"),
            text = stringResource(R.string.meetings_leave_meeting_confirmation_dialog_title),
            confirmButtonText = stringResource(R.string.general_leave),
            cancelButtonText = stringResource(R.string.meetings__waiting_room_leave_meeting_dialog_cancel_button),
            onConfirm = onCloseClicked,
            onDismiss = { showLeaveDialog = false },
        )

        state.denyAccessDialog -> MegaAlertDialog(
            modifier = Modifier.testTag("waiting_room:dialog_deny_access"),
            title = stringResource(R.string.meetings_waiting_room_deny_user_dialog_title),
            text = stringResource(R.string.meetings_waiting_room_deny_user_dialog_description),
            confirmButtonText = stringResource(R.string.cloud_drive_media_discovery_banner_ok),
            cancelButtonText = null,
            onConfirm = onCloseClicked,
            onDismiss = {},
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )

        state.inactiveHostDialog -> MegaAlertDialog(
            modifier = Modifier.testTag("waiting_room:dialog_inactive_host"),
            title = stringResource(R.string.meetings_waiting_room_inactive_host_dialog_title),
            text = stringResource(R.string.meetings_waiting_room_inactive_host_dialog_description),
            confirmButtonText = stringResource(R.string.cloud_drive_media_discovery_banner_ok),
            cancelButtonText = null,
            onConfirm = onCloseClicked,
            onDismiss = {},
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    }
}

@Composable
private fun ToggleControlsRow(
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    speakerEnabled: Boolean,
    onMicToggleChange: (Boolean) -> Unit,
    onCameraToggleChange: (Boolean) -> Unit,
    onSpeakerToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(modifier = modifier.fillMaxWidth()) {
        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_mic"),
            checked = micEnabled,
            enabled = context.hasMicrophonePermissions(),
            title = stringResource(R.string.general_mic),
            enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_on,
            disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_off,
            onCheckedChange = onMicToggleChange,
        )

        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_camera"),
            checked = cameraEnabled,
            enabled = context.hasCameraPermissions(),
            title = stringResource(R.string.general_camera),
            enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_video_on,
            disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_video_off,
            onCheckedChange = onCameraToggleChange,
        )

        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_speaker"),
            checked = speakerEnabled,
            title = stringResource(R.string.general_speaker),
            enabledIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_max,
            disabledIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_off,
            onCheckedChange = onSpeakerToggleChange,
        )
    }
}

@Composable
private fun GuestNameInputText(
    firstName: String,
    lastName: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        val textFieldColors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Unspecified,
            focusedIndicatorColor = Color.Unspecified,
            unfocusedIndicatorColor = Color.Unspecified,
            cursorColor = MaterialTheme.colors.secondary,
        )
        GenericTextField(
            placeholder = stringResource(R.string.first_name_text),
            text = firstName,
            colors = textFieldColors,
            imeAction = ImeAction.Next,
            onTextChange = { onFirstNameChange(it.take(15)) },
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:text_first_name"),
        )
        GenericTextField(
            placeholder = stringResource(R.string.lastname_text),
            text = lastName,
            colors = textFieldColors,
            onTextChange = { onLastNameChange(it.take(15)) },
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:text_last_name"),
        )
    }
}

private fun Context.hasCameraPermissions(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PERMISSION_GRANTED

private fun Context.hasMicrophonePermissions(): Boolean =
    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 720,
    heightDp = 360
)
@Composable
internal fun PreviewWaitingRoomView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WaitingRoomView(
            state = WaitingRoomState(
                chatId = -1,
                schedId = -1,
                callStarted = Random.nextBoolean(),
                title = "Book Club",
                formattedTimestamp = "Monday, 30 May · 10:25 - 11:25",
            ),
            onInfoClicked = {},
            onCloseClicked = {},
            onMicToggleChange = {},
            onCameraToggleChange = {},
            onSpeakerToggleChange = {},
            onGuestNameChange = { _, _ -> },
            videoStream = emptyFlow(),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 720,
    heightDp = 360
)
@Composable
internal fun PreviewGuestWaitingRoomView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WaitingRoomView(
            state = WaitingRoomState(
                chatId = -1,
                schedId = -1,
                callStarted = false,
                title = "Book Club",
                formattedTimestamp = "Monday, 30 May · 10:25 - 11:25",
                guestFirstName = "Bell",
                guestLastName = "Hooks",
            ),
            onInfoClicked = {},
            onCloseClicked = {},
            onMicToggleChange = {},
            onCameraToggleChange = {},
            onSpeakerToggleChange = {},
            onGuestNameChange = { _, _ -> },
            videoStream = emptyFlow(),
        )
    }
}
