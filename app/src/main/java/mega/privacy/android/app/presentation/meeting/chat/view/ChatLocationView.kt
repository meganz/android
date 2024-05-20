package mega.privacy.android.app.presentation.meeting.chat.view

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.EnableGeolocationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.INVALID_LOCATION_MESSAGE_ID
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openLocationPicker

/**
 * ChatLocationView where pick location action is managed.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatLocationView(
    isGeolocationEnabled: Boolean,
    onEnableGeolocation: () -> Unit,
    onSendLocationMessage: (Intent?) -> Unit,
    onDismissView: () -> Unit,
    msgId: Long = INVALID_LOCATION_MESSAGE_ID,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    var showEnableGeolocationDialog by rememberSaveable { mutableStateOf(!isGeolocationEnabled) }
    var waitingForLocation by rememberSaveable {
        mutableStateOf(false)
    }
    val locationPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            it.data?.let { location -> onSendLocationMessage(location) }
            onDismissView()
        }
    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { isGranted ->
            if (isGranted) {
                waitingForLocation = true
                openLocationPicker(context, locationPickerLauncher, msgId)
            } else {
                showPermissionNotAllowedSnackbar(
                    context,
                    coroutineScope,
                    scaffoldState.snackbarHostState,
                    R.string.chat_attach_location_deny_permission
                )
                onDismissView()
            }
        }

    Spacer(modifier = Modifier.testTag(CHAT_LOCATION_VIEW_TAG))

    if (!waitingForLocation) {
        when {
            !locationPermissionState.status.isGranted -> {
                SideEffect {
                    locationPermissionState.launchPermissionRequest()
                }
            }

            showEnableGeolocationDialog -> {
                EnableGeolocationDialog(
                    onConfirm = {
                        onEnableGeolocation()
                        waitingForLocation = true
                        openLocationPicker(context, locationPickerLauncher, msgId)
                    },
                    onDismiss = {
                        showEnableGeolocationDialog = false
                        onDismissView()
                    },
                )
            }

            else -> {
                SideEffect {
                    waitingForLocation = true
                    openLocationPicker(context, locationPickerLauncher, msgId)
                }
            }
        }
    }

}

internal const val CHAT_LOCATION_VIEW_TAG = "chat:location_view"