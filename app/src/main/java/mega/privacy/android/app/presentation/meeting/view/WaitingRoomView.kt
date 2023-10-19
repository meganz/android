package mega.privacy.android.app.presentation.meeting.view

import android.Manifest
import android.content.res.Configuration
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.layoutId
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.ToggleMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.text.AutoSizeText
import mega.privacy.android.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.core.ui.controls.video.MegaVideoTextureView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.grey_900
import mega.privacy.android.core.ui.utils.rememberPermissionState
import mega.privacy.mobile.analytics.event.ScheduledMeetingJoinGuestButtonEvent
import mega.privacy.mobile.analytics.event.WaitingRoomLeaveButtonEvent
import org.jetbrains.anko.landscape
import kotlin.random.Random


/**
 * Waiting room View
 */
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
    val focusRequester = remember { FocusRequester() }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
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
            constraintSet = createWaitingRoomConstraintSet(isLandscape, state.guestMode),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState(), false)
        ) {
            IconButton(
                onClick = { showLeaveDialog = true },
                modifier = Modifier
                    .layoutId("closeButton")
                    .testTag("waiting_room:button_close")
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
                    .layoutId("infoButton")
                    .testTag("waiting_room:button_info")
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
                    .layoutId("titleText")
                    .testTag("waiting_room:text_title")
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
                    .layoutId("timestampText")
                    .testTag("waiting_room:text_timestamp")
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = state.formattedTimestamp.isNullOrBlank(),
                    ),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .testTag("waiting_room:text_alert")
                    .layoutId("alertText")
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.onSurface)
            ) {
                AutoSizeText(
                    text = stringResource(
                        if (state.callStarted)
                            R.string.meetings_waiting_room_wait_for_host_to_let_you_in_label
                        else
                            R.string.meetings_waiting_room_wait_for_host_to_start_meeting_label
                    ),
                    maxLines = 1,
                    maxTextSize = 14.sp,
                    style = MaterialTheme.typography.subtitle2medium,
                    color = MaterialTheme.colors.surface,
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .layoutId("videoPreview")
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = grey_900)
            ) {
                if (state.cameraEnabled) {
                    MegaVideoTextureView(
                        videoStream = videoStream,
                        mirrorEffect = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("waiting_room:preview_camera"),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .align(Alignment.Center)
                            .testTag("waiting_room:image_avatar")
                    ) {
                        if (state.avatar != null) {
                            ChatAvatarView(
                                modifier = Modifier.fillMaxSize(),
                                avatars = listOf(state.avatar)
                            )
                        } else {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = "Guest avatar",
                                painter = painterResource(R.drawable.ic_guest_avatar),
                            )
                        }
                    }
                }
            }

            if (state.guestMode) {
                Box(
                    modifier = Modifier
                        .layoutId("guestBackground")
                        .background(MaterialTheme.colors.surface)
                        .testTag("waiting_room:guest_background")
                )

                GuestNameInputText(
                    firstName = firstName,
                    lastName = lastName,
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    modifier = Modifier
                        .layoutId("guestInputs")
                        .testTag("waiting_room:guest_name_inputs")
                        .focusRequester(focusRequester)
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
                    .layoutId("controls")
                    .testTag("waiting_room:toggle_controls")
            )

            if (state.guestMode) {
                RaisedDefaultMegaButton(
                    textId = R.string.action_join,
                    enabled = firstName.isNotBlank() && lastName.isNotBlank(),
                    onClick = {
                        Analytics.tracker.trackEvent(ScheduledMeetingJoinGuestButtonEvent)
                        onGuestNameChange(firstName, lastName)
                    },
                    modifier = Modifier
                        .layoutId("joinButton")
                        .testTag("waiting_room:button_join")
                )
            }
        }
    }

    LaunchedEffect(state.guestMode) {
        delay(200)
        if (state.guestMode) {
            focusRequester.requestFocus()
        }
    }

    BackHandler { onCloseClicked() }

    when {
        showLeaveDialog -> MegaAlertDialog(
            modifier = Modifier.testTag("waiting_room:dialog_leave"),
            text = stringResource(R.string.meetings_leave_meeting_confirmation_dialog_title),
            confirmButtonText = stringResource(R.string.general_leave),
            cancelButtonText = stringResource(R.string.meetings__waiting_room_leave_meeting_dialog_cancel_button),
            onConfirm = {
                Analytics.tracker.trackEvent(WaitingRoomLeaveButtonEvent)
                onCloseClicked()
            },
            onDismiss = { showLeaveDialog = false },
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
    }
}

@OptIn(ExperimentalPermissionsApi::class)
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
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Row(modifier = modifier.fillMaxWidth()) {
        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_mic"),
            checked = !micEnabled,
            enabled = !micPermissionState.status.shouldShowRationale,
            title = stringResource(R.string.general_mic),
            uncheckedIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_on,
            checkedIcon = mega.privacy.android.core.R.drawable.ic_universal_mic_off,
            onCheckedChange = { checked ->
                if (micPermissionState.status.isGranted) {
                    onMicToggleChange(!checked)
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            },
        )

        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_camera"),
            checked = !cameraEnabled,
            enabled = !cameraPermissionState.status.shouldShowRationale,
            title = stringResource(R.string.general_camera),
            uncheckedIcon = mega.privacy.android.core.R.drawable.ic_universal_video_on,
            checkedIcon = mega.privacy.android.core.R.drawable.ic_universal_video_off,
            onCheckedChange = { checked ->
                if (cameraPermissionState.status.isGranted) {
                    onCameraToggleChange(!checked)
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            },
        )


        ToggleMegaButton(
            modifier = Modifier
                .weight(1f)
                .testTag("waiting_room:button_speaker"),
            checked = !speakerEnabled,
            title = stringResource(R.string.general_speaker),
            uncheckedIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_max,
            checkedIcon = mega.privacy.android.core.R.drawable.ic_universal_volume_off,
            onCheckedChange = { checked -> onSpeakerToggleChange(!checked) },
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
                guestMode = false,
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
                guestMode = true,
                callStarted = false,
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
