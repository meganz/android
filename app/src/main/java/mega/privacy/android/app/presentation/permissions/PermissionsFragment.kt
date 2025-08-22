package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build
import android.os.Bundle
import android.provider.Settings.canDrawOverlays
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
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
import timber.log.Timber

/**
 * Fragment shown after the first installation to request required permissions.
 */
@AndroidEntryPoint
class PermissionsFragment : Fragment() {
    private val viewModel: PermissionsViewModel by viewModels()
    private var onlyShowNotificationPermission: Boolean = false

    private val readPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                getAudioPermissionByVersion(),
                getReadExternalStoragePermission()
            )
        } else {
            arrayOf(
                getImagePermissionByVersion(),
                getAudioPermissionByVersion(),
                getVideoPermissionByVersion(),
                getReadExternalStoragePermission()
            )
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            onlyShowNotificationPermission =
                it.getBoolean(IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION, false)
        }
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val isDarkTheme = uiState.themeMode.isDarkMode()
        val lifecycleOwner = LocalLifecycleOwner.current

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

        AndroidTheme(isDarkTheme) {
            NewPermissionsComposableScreen(
                uiState = uiState,
                askNotificationPermission = {
                    Analytics.tracker.trackEvent(EnableNotificationsCTAButtonPressedEvent)
                    askForNotificationsPermission(PERMISSIONS_FRAGMENT_NOTIFICATION_PERMISSION)
                },
                askCameraBackupPermission = {
                    Analytics.tracker.trackEvent(EnableCameraBackupsCTAButtonPressedEvent)
                    askForCameraBackupPermission()
                },
                onSkipNotificationPermission = {
                    Analytics.tracker.trackEvent(SkipNotificationsCTAButtonPressedEvent)
                    setNextPermission()
                },
                onSkipCameraBackupPermission = {
                    Analytics.tracker.trackEvent(SkipCameraBackupsCTAButtonPressedEvent)
                    setNextPermission()
                },
                closePermissionScreen = ::closePermissionScreen,
                resetFinishEvent = viewModel::resetFinishEvent,
                onPermissionPageShown = viewModel::setPermissionPageShown
            )
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            setupData()
        }
    }

    private fun setupData() {
        val missingPermission = mutableListOf<Pair<Permission, Boolean>>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    Pair(
                        Permission.Notifications, hasPermissions(
                            requireActivity(),
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                )
            }

            // Early exit if notification is the only permission needed to be shown
            if (onlyShowNotificationPermission) return@apply

            add(
                Pair(
                    Permission.DisplayOverOtherApps,
                    canDrawOverlays(requireContext())
                )
            )

            add(
                Pair(
                    Permission.Read,
                    hasPermissions(requireActivity(), *readPermissions)
                )
            )

            add(
                Pair(
                    Permission.Write,
                    hasPermissions(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            )

            add(
                Pair(
                    Permission.CameraBackup,
                    hasPermissions(
                        requireActivity(),
                        *getCameraUploadsPermissions().toTypedArray()
                    )
                )
            )

            add(
                Pair(
                    Permission.Camera,
                    hasPermissions(requireActivity(), Manifest.permission.CAMERA)
                )
            )

            add(
                Pair(
                    Permission.Microphone,
                    hasPermissions(requireActivity(), Manifest.permission.RECORD_AUDIO)
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    Pair(
                        Permission.Bluetooth,
                        hasPermissions(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
            }
        }
        viewModel.updateFirstTimeLoginStatus()
        viewModel.setData(missingPermission)
    }

    /**
     * Sets next permission.
     */
    fun setNextPermission() {
        viewModel.nextPermission()
    }

    fun onNotificationPermissionResult(permissionGranted: Boolean) {
        if (permissionGranted) {
            Analytics.tracker.trackEvent(AllowNotificationsCTAButtonPressedEvent)
        } else {
            Analytics.tracker.trackEvent(DontAllowNotificationsCTAButtonPressedEvent)
        }
    }

    fun onMediaPermissionResult(results: Map<String, Boolean>) {
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

    private fun closePermissionScreen() {
        val isCameraUploadsEnabled = viewModel.uiState.value.isCameraUploadsEnabled
        (requireActivity() as ManagerActivity)
            .destroyPermissionsFragment(isCameraUploadsEnabled)
    }

    /**
     * Asks for notifications permission.
     */
    private fun askForNotificationsPermission(requestCode: Int) {
        requestPermission(
            requireActivity(),
            requestCode,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    /**
     * Asks for camera backup permission.
     */
    private fun askForCameraBackupPermission() {
        Timber.d("CAMERA_BACKUP")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT_MEDIA_PERMISSION,
            *getCameraUploadsPermissions().toTypedArray()
        )
    }

    companion object {
        /**
         * Permissions fragment identifier.
         */
        const val PERMISSIONS_FRAGMENT = 666

        /**
         * Permissions fragment identifier for media permission - Design Revamp
         */
        const val PERMISSIONS_FRAGMENT_MEDIA_PERMISSION = 667

        /**
         * Permissions fragment identifier for notification permission - Design Revamp
         */
        const val PERMISSIONS_FRAGMENT_NOTIFICATION_PERMISSION = 668

        /**
         * Creates a new instance of [PermissionsFragment].
         *
         * @return The Fragment.
         */
        @JvmStatic
        fun newInstance(): PermissionsFragment = PermissionsFragment()
    }
}