package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.mobile.analytics.event.AllowNotificationsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.CameraBackupsCTAScreenEvent
import mega.privacy.mobile.analytics.event.DontAllowCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.DontAllowNotificationsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.EnableCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.EnableNotificationsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullAccessCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.LimitedAccessCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.NotificationsCTAScreenEvent
import mega.privacy.mobile.analytics.event.SkipCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.SkipNotificationsCTAButtonPressedEvent

/**
 * Composable screen for handling permissions in the single activity implementation.
 * This replaces the PermissionsFragment functionality.
 */
@Composable
fun PermissionsScreenComposable(
    onPermissionsCompleted: (isCameraUploadsEnabled: Boolean) -> Unit,
    activity: ComponentActivity,
    onlyShowNotificationPermission: Boolean = false,
) {
    val viewModel: PermissionsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = uiState.themeMode.isDarkMode()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Analytics.tracker.trackEvent(AllowNotificationsCTAButtonPressedEvent)
        } else {
            Analytics.tracker.trackEvent(DontAllowNotificationsCTAButtonPressedEvent)
        }
        viewModel.nextPermission()
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        when {
            results.areMediaPermissionsPartiallyGranted() -> {
                Analytics.tracker.trackEvent(LimitedAccessCameraBackupsCTAButtonPressedEvent)
                viewModel.onMediaPermissionsGranted()
            }

            results.areMediaPermissionsGranted() -> {
                Analytics.tracker.trackEvent(FullAccessCameraBackupsCTAButtonPressedEvent)
                viewModel.onMediaPermissionsGranted()
            }

            else -> {
                Analytics.tracker.trackEvent(DontAllowCameraBackupsCTAButtonPressedEvent)
                viewModel.nextPermission()
            }
        }
    }

    LaunchedEffect(uiState.visiblePermission) {
        when (uiState.visiblePermission) {
            NewPermissionScreen.Notification -> {
                Analytics.tracker.trackEvent(NotificationsCTAScreenEvent)
            }

            NewPermissionScreen.CameraBackup -> {
                Analytics.tracker.trackEvent(CameraBackupsCTAScreenEvent)
            }

            NewPermissionScreen.Loading -> {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY && uiState.isCameraUploadsEnabled) {
                viewModel.startCameraUploadIfGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initialize permissions data
    LaunchedEffect(Unit) {
        val missingPermission = mutableListOf<Pair<Permission, Boolean>>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    Pair(
                        Permission.Notifications, hasPermissions(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                )
            }

            // Early exit if notification is the only permission needed to be shown
            if (onlyShowNotificationPermission) return@apply

            add(
                Pair(
                    Permission.CameraBackup,
                    hasPermissions(
                        activity,
                        *getCameraUploadsPermissions().toTypedArray()
                    )
                )
            )
        }
        viewModel.updateFirstTimeLoginStatus()
        viewModel.setData(missingPermission)
    }

    AndroidTheme(isDarkTheme) {
        NewPermissionsContent(
            uiState = uiState,
            askNotificationPermission = {
                Analytics.tracker.trackEvent(EnableNotificationsCTAButtonPressedEvent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            askCameraBackupPermission = {
                Analytics.tracker.trackEvent(EnableCameraBackupsCTAButtonPressedEvent)
                mediaPermissionLauncher.launch(getCameraUploadsPermissions().toTypedArray())
            },
            onSkipNotificationPermission = {
                Analytics.tracker.trackEvent(SkipNotificationsCTAButtonPressedEvent)
                viewModel.nextPermission()
            },
            onSkipCameraBackupPermission = {
                Analytics.tracker.trackEvent(SkipCameraBackupsCTAButtonPressedEvent)
                viewModel.nextPermission()
            },
            closePermissionScreen = {
                onPermissionsCompleted(uiState.isCameraUploadsEnabled)
            },
            resetFinishEvent = viewModel::resetFinishEvent,
            onPermissionPageShown = viewModel::setPermissionPageShown
        )
    }
}

private fun getCameraUploadsPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Only request the generic External Storage Permission on Devices below API 33
        add(READ_EXTERNAL_STORAGE)
    } else {
        // Request Granular Media Permissions beginning on API 33
        add(READ_MEDIA_IMAGES)
        add(READ_MEDIA_VIDEO)
    }.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Request Partial Media Permissions beginning on API 34
            add(READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }
}

/**
 * Checks if the User has granted the Media Permissions necessary to enable Camera Uploads. The
 * number of Permissions being checked will depend on the Device OS
 *
 * @return true if the Media Permissions are granted
 */
private fun Map<String, Boolean>.areMediaPermissionsGranted() =
    when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> this.getOrElse(
            READ_EXTERNAL_STORAGE
        ) { false }
        // Media Permissions are still granted if at least the Partial Media Permission is granted
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            (this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false })
                    || this.getOrElse(READ_MEDIA_VISUAL_USER_SELECTED) { false }
        }

        else -> this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false }
    }

private fun Map<String, Boolean>.areMediaPermissionsPartiallyGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false

    val mediaImagesGranted = getOrElse(READ_MEDIA_IMAGES) { false }
    val mediaVideoGranted = getOrElse(READ_MEDIA_VIDEO) { false }
    val partialMediaGranted = getOrElse(READ_MEDIA_VISUAL_USER_SELECTED) { false }

    return partialMediaGranted && !(mediaImagesGranted && mediaVideoGranted)
}
