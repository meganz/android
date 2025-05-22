package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.canDrawOverlays
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.FragmentPermissionsBinding
import mega.privacy.android.app.databinding.PermissionsImageLayoutBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.image
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.positiveButton
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.app.presentation.permissions.model.PermissionType
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.mobile.analytics.event.CameraBackupsCTAScreenEvent
import mega.privacy.mobile.analytics.event.EnableCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.EnableNotificationsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.NotificationsCTAScreenEvent
import mega.privacy.mobile.analytics.event.OnboardingInitialPageNotNowButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingInitialPageSetUpMegaButtonPressedEvent
import mega.privacy.mobile.analytics.event.SkipCameraBackupsCTAButtonPressedEvent
import mega.privacy.mobile.analytics.event.SkipNotificationsCTAButtonPressedEvent
import timber.log.Timber

/**
 * Fragment shown after the first installation to request required permissions.
 */
@AndroidEntryPoint
class PermissionsFragment : Fragment() {
    private val viewModel: PermissionsViewModel by viewModels()
    private lateinit var binding: FragmentPermissionsBinding
    private lateinit var permissionBinding: PermissionsImageLayoutBinding
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
            // Request Granular Media and Notifications Permissions beginning on API 33
            add(READ_MEDIA_IMAGES)
            add(READ_MEDIA_VIDEO)
            add(POST_NOTIFICATIONS)
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
    ): View {
        binding = FragmentPermissionsBinding.inflate(layoutInflater, container, false)
        permissionBinding = binding.permissionsImageLayout
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            setupData()
        }

        monitorOnboardingRevampState()
    }

    private fun monitorOnboardingRevampState() {
        collectFlow(
            viewModel
                .uiState
                .mapNotNull { it.isOnboardingRevampEnabled }
                .distinctUntilChanged()
        ) { state ->
            setupView(state)
            updatePermissionsLayoutVisibility(state)
        }
    }

    private fun setupView(isRevampEnabled: Boolean) {
        if (isRevampEnabled) {
            setupComposableView()
        } else {
            setupLegacyView()
            setupLegacyObservers()
        }
    }

    private fun updatePermissionsLayoutVisibility(isRevampEnabled: Boolean) {
        binding.legacyPermissionsLayout.isVisible = !isRevampEnabled
        binding.newPermissionsLayout.isVisible = isRevampEnabled
    }

    private fun setupComposableView() {
        binding.newPermissionsLayout.setContent {
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
                        askForNotificationsPermission()
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
    }

    private fun setupLegacyView() {
        binding.notNowButton.setOnClickListener {
            Analytics.tracker.trackEvent(OnboardingInitialPageNotNowButtonPressedEvent)
            (requireActivity() as ManagerActivity)
                .destroyPermissionsFragment(isCameraUploadsEnabled = false)
        }
        binding.setupButton.setOnClickListener {
            Analytics.tracker.trackEvent(OnboardingInitialPageSetUpMegaButtonPressedEvent)
            viewModel.grantAskForPermissions()
        }
        binding.notNowButton2.setOnClickListener { setNextPermission() }
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

    private fun setupLegacyObservers() {
        viewModel.shouldShowInitialSetupScreen().observe(viewLifecycleOwner, ::showScreen)
        viewModel.getCurrentPermission().observe(viewLifecycleOwner, ::setCurrentPermissionScreen)
        viewModel.onAskPermission().observe(viewLifecycleOwner, ::askForPermission)
    }

    /**
     * Sets next permission.
     */
    fun setNextPermission() {
        viewModel.nextPermission()
    }

    fun onMediaPermissionResult(results: Map<String, Boolean>) {
        if (results.areMediaPermissionsGranted()) {
            viewModel.onMediaPermissionsGranted()
        } else {
            viewModel.nextPermission()
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
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> this.getOrElse(READ_EXTERNAL_STORAGE) { false }
            // Media Permissions are still granted if at least the Partial Media Permission is granted
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                (this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false })
                        || this.getOrElse(READ_MEDIA_VISUAL_USER_SELECTED) { false }
            }

            else -> this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false }
        }

    private fun closePermissionScreen() {
        val isCameraUploadsEnabled = viewModel.uiState.value.isCameraUploadsEnabled
        (requireActivity() as ManagerActivity)
            .destroyPermissionsFragment(isCameraUploadsEnabled)
    }

    /**
     * Sets current permission screen.
     */
    private fun setCurrentPermissionScreen(currentPermission: PermissionScreen?) {
        if (currentPermission == null) {
            closePermissionScreen()
            return
        }

        permissionBinding.imagePermissions
            .setImageDrawable(ContextCompat.getDrawable(requireContext(), currentPermission.image))
        permissionBinding.titlePermissions.text =
            getString(currentPermission.title)
        permissionBinding.subtitlePermissions.text =
            getString(currentPermission.description)
        binding.enableButton.apply {
            text = getString(currentPermission.positiveButton)
            setOnClickListener { viewModel.askPermission() }
        }
    }

    /**
     * Asks for a [PermissionType].
     *
     * @param permissionType The [PermissionType] defining for which permissions have to ask.
     */
    private fun askForPermission(permissionType: PermissionType) {
        when (permissionType) {
            PermissionType.Notifications -> askForNotificationsPermission()
            PermissionType.DisplayOverOtherApps -> askForDisplayOverOtherAppsPermission()
            PermissionType.ReadAndWrite -> askForReadAndWritePermissions()
            PermissionType.Write -> askForWritePermission()
            PermissionType.Read -> askForReadPermission()
            PermissionType.Camera -> askForCameraPermission()
            PermissionType.MicrophoneAndBluetooth -> askForMicrophoneAndBluetoothPermissions()
            PermissionType.Microphone -> askForMicrophonePermission()
            PermissionType.Bluetooth -> askForBluetoothPermission()
            PermissionType.CameraBackup -> askForCameraBackupPermission()
        }
    }

    /**
     * Asks for notifications permission.
     */
    private fun askForNotificationsPermission() {
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    private fun askForDisplayOverOtherAppsPermission() {
        context?.let {
            Intent(
                ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${it.packageName}")
            ).also { intent -> startActivity(intent) }
        }
        lifecycleScope.launch {
            //Give some time to the setting screen to be opened
            delay(500)
            setNextPermission()
        }
    }

    /**
     * Asks for read and write storage permissions.
     */
    private fun askForReadAndWritePermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            getImagePermissionByVersion(),
            getAudioPermissionByVersion(),
            getVideoPermissionByVersion(),
            getReadExternalStoragePermission()
        )
        requestPermission(requireActivity(), PERMISSIONS_FRAGMENT, *permissions)
    }

    /**
     * Asks for write storage permission.
     */
    private fun askForWritePermission() {
        Timber.d("WRITE_EXTERNAL_STORAGE")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * Asks for read storage permission.
     */
    private fun askForReadPermission() {
        Timber.d("READ_EXTERNAL_STORAGE")
        requestPermission(requireActivity(), PERMISSIONS_FRAGMENT, *readPermissions)
    }

    /**
     * Asks for camera permission.
     */
    private fun askForCameraPermission() {
        Timber.d("CAMERA")
        requestPermission(requireActivity(), PERMISSIONS_FRAGMENT, Manifest.permission.CAMERA)
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

    /**
     * Asks for microphone and bluetooth permissions.
     */
    private fun askForMicrophoneAndBluetoothPermissions() {
        Timber.d("RECORD_AUDIO && BLUETOOTH_CONNECT")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    /**
     * Asks for microphone permission.
     */
    private fun askForMicrophonePermission() {
        Timber.d("RECORD_AUDIO")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * Asks for bluetooth permission.
     */
    private fun askForBluetoothPermission() {
        Timber.d("BLUETOOTH_CONNECT")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    private fun showScreen(showInitialSetupScreen: Boolean) {
        binding.setupFragmentContainer.isVisible = showInitialSetupScreen
        binding.allowAccessFragmentContainer.isVisible = !showInitialSetupScreen
    }

    companion object {
        /**
         * Permissions fragment identifier.
         */
        const val PERMISSIONS_FRAGMENT = 666

        /**
         * Permissions fragment identifier for media permission.
         */
        const val PERMISSIONS_FRAGMENT_MEDIA_PERMISSION = 667

        /**
         * Creates a new instance of [PermissionsFragment].
         *
         * @return The Fragment.
         */
        @JvmStatic
        fun newInstance(): PermissionsFragment = PermissionsFragment()
    }
}