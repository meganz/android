package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.databinding.FragmentPermissionsBinding
import mega.privacy.android.app.databinding.PermissionsImageLayoutBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.image
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPermissionsBinding.inflate(layoutInflater, container, false)
        permissionBinding = binding.permissionsImageLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            setupData()
        }

        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.notNowButton.setOnClickListener {
            (requireActivity() as ManagerActivity).destroyPermissionsFragment()
        }
        binding.setupButton.setOnClickListener { viewModel.grantAskForPermissions() }
        binding.notNowButton2.setOnClickListener { setNextPermission() }
        binding.enableButton.setOnClickListener { viewModel.askPermission() }
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

            add(
                Pair(
                    Permission.Contacts,
                    hasPermissions(requireActivity(), Manifest.permission.READ_CONTACTS)
                )
            )
        }
        viewModel.updateFirstTimeLoginStatus()
        viewModel.setData(missingPermission)
    }

    private fun setupObservers() {
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
        permissionBinding.subtitleExplanation.isVisible =
            currentPermission == PermissionScreen.Contacts
    }

    /**
     * Asks for a [PermissionType].
     *
     * @param permissionType The [PermissionType] defining for which permissions have to ask.
     */
    private fun askForPermission(permissionType: PermissionType) {
        when (permissionType) {
            PermissionType.Notifications -> askForNotificationsPermission()
            PermissionType.ReadAndWrite -> askForReadAndWritePermissions()
            PermissionType.Write -> askForWritePermission()
            PermissionType.Read -> askForReadPermission()
            PermissionType.Camera -> askForCameraPermission()
            PermissionType.MicrophoneAndBluetooth -> askForMicrophoneAndBluetoothPermissions()
            PermissionType.Microphone -> askForMicrophonePermission()
            PermissionType.Bluetooth -> askForBluetoothPermission()
            PermissionType.Contacts -> askForContactsPermissions()
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

    /**
     * Asks for contacts permission.
     */
    private fun askForContactsPermissions() {
        Timber.d("Ask for CONTACT permission")
        requestPermission(
            requireActivity(),
            PERMISSIONS_FRAGMENT,
            Manifest.permission.READ_CONTACTS
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
         * Creates a new instance of [PermissionsFragment].
         *
         * @return The Fragment.
         */
        @JvmStatic
        fun newInstance(): PermissionsFragment = PermissionsFragment()
    }
}