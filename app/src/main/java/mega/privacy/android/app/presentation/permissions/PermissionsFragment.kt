package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.canDrawOverlays
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.arch.extensions.collectFlow
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
import mega.privacy.android.app.presentation.permissions.view.CameraBackupPermissionsScreen
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.mobile.analytics.event.OnboardingInitialPageNotNowButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingInitialPageSetUpMegaButtonPressedEvent
import timber.log.Timber

/**
 * Fragment shown after the first installation to request required permissions.
 */
@AndroidEntryPoint
class PermissionsFragment : Fragment() {
    private val viewModel: PermissionsViewModel by viewModels()
    private lateinit var binding: FragmentPermissionsBinding
    private lateinit var permissionBinding: PermissionsImageLayoutBinding

    private val readPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                getAudioPermissionByVersion(), getReadExternalStoragePermission()
            )
        } else {
            arrayOf(
                getImagePermissionByVersion(),
                getAudioPermissionByVersion(),
                getVideoPermissionByVersion(),
                getReadExternalStoragePermission()
            )
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
                .map { it.isOnboardingRevampEnabled }
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
            val pagerState = rememberPagerState(initialPage = PERMISSION_DEFAULT_PAGE) { 3 }

            LaunchedEffect(uiState.visiblePermission) {
                val page = when (uiState.visiblePermission) {
                    PermissionScreen.Camera -> PERMISSION_CAMERA_PAGE
                    PermissionScreen.Notifications -> PERMISSION_NOTIFICATION_PAGE
                    else -> PERMISSION_DEFAULT_PAGE
                }

                pagerState.animateScrollToPage(page)
            }

            AndroidTheme(isDarkTheme) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState,
                    userScrollEnabled = false,
                ) { page ->
                    when (page) {
                        PERMISSION_DEFAULT_PAGE -> {
                            // Show loading indicator when permissions is loading
                            Box(modifier = Modifier.fillMaxSize()) {
                                LargeHUD()
                            }
                        }

                        PERMISSION_NOTIFICATION_PAGE -> {
                            // Todo replace with notification permission screen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        setNextPermission()
                                    }
                            ) {
                                MegaText(
                                    text = "Empty Notification screen",
                                    textColor = TextColor.Primary
                                )
                            }
                        }

                        PERMISSION_CAMERA_PAGE -> {
                            CameraBackupPermissionsScreen(
                                modifier = Modifier.fillMaxSize(),
                                onEnablePermission = ::askForReadAndWritePermissions,
                                onSkipPermission = ::setNextPermission
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupLegacyView() {
        binding.notNowButton.setOnClickListener {
            Analytics.tracker.trackEvent(OnboardingInitialPageNotNowButtonPressedEvent)
            (requireActivity() as ManagerActivity).destroyPermissionsFragment()
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

    /**
     * Sets current permission screen.
     */
    private fun setCurrentPermissionScreen(currentPermission: PermissionScreen?) {
        if (currentPermission == null) {
            (requireActivity() as ManagerActivity).destroyPermissionsFragment()
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

        private const val PERMISSION_DEFAULT_PAGE = 0
        private const val PERMISSION_NOTIFICATION_PAGE = 1
        private const val PERMISSION_CAMERA_PAGE = 2

        /**
         * Creates a new instance of [PermissionsFragment].
         *
         * @return The Fragment.
         */
        @JvmStatic
        fun newInstance(): PermissionsFragment = PermissionsFragment()
    }
}