package mega.privacy.android.app.main

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication.Companion.isLoggingOut
import mega.privacy.android.app.R
import mega.privacy.android.app.TourImageAdapter
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.DialogRecoveryKeyBinding
import mega.privacy.android.app.databinding.FragmentTourBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.meeting.fragments.PasteMeetingLinkGuestDialogFragment
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.usecase.account.SetSecureFlag
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * Tour Fragment.
 */
@AndroidEntryPoint
class TourFragment : Fragment() {

    private var _binding: FragmentTourBinding? = null

    private val binding get() = _binding!!

    private lateinit var joinMeetingAsGuestLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var setSecureFlag: SetSecureFlag

    private val selectedCircle by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.selection_circle_page_adapter)
    }
    private val notSelectedCircle by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.not_selection_circle_page_adapter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")
        _binding = FragmentTourBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        // For small screen like nexus one or bigger screen, this is to force the scroll view to bottom to show buttons
        // Meanwhile, tour image glide could also be shown
        with(binding.tourFragmentBaseContainer) {
            post { fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        arguments?.getString(EXTRA_RECOVERY_KEY_URL, null)?.let { recoveryKeyUrl ->
            Timber.d("Link to resetPass: $recoveryKeyUrl")
            showRecoveryKeyDialog(recoveryKeyUrl)
        }

        arguments?.getString(EXTRA_PARK_ACCOUNT_URL, null)?.let { parkAccountUrl ->
            Timber.d("Link to parkAccount: $parkAccountUrl")
            showParkAccountDialog(parkAccountUrl)
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _: View?, insets: WindowInsetsCompat ->
            val insetsBottom =
                insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, 0, 0, insetsBottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupView() = with(binding) {
        buttonLoginTour.setOnClickListener {
            Timber.d("onLoginClick")
            (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Login)
        }
        buttonRegisterTour.setOnClickListener {
            Timber.d("onRegisterClick")
            (requireActivity() as LoginActivity).showFragment(LoginFragmentType.CreateAccount)
        }
        joinMeetingAsGuest.setOnClickListener {
            Timber.d("onJoinMeetingAsGuestClick")
            if (requestBluetoothPermission()) {
                joinMeetingAsGuestLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                isLoggingOut = false
                PasteMeetingLinkGuestDialogFragment().show(childFragmentManager,
                    PasteMeetingLinkGuestDialogFragment.TAG)
            }
        }

        setItemSelected(firstItem.id)

        with(pager) {
            adapter = TourImageAdapter(requireActivity())
            currentItem = 0
            setOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> setItemSelected(firstItem.id)
                        1 -> setItemSelected(secondItem.id)
                        2 -> setItemSelected(thirdItem.id)
                        3 -> setItemSelected(fourthItem.id)
                        4 -> setItemSelected(fifthItem.id)
                    }
                }
            })
        }

        viewLifecycleOwner.lifecycleScope.launch {
            setSecureFlag(getFeatureFlagValueUseCase(AppFeatures.SetSecureFlag))
        }
    }

    /**
     * Sets the current item of the tour as selected.
     *
     * @param itemId The id of the view to set as selected.
     */
    private fun setItemSelected(itemId: Int) = with(binding) {
        firstItem.setSelected(itemId)
        secondItem.setSelected(itemId)
        thirdItem.setSelected(itemId)
        fourthItem.setSelected(itemId)
        fifthItem.setSelected(itemId)
    }

    /**
     * Sets the an item of the tour as selected.
     *
     * @param itemId The id of the view to set as selected.
     */
    private fun ImageView.setSelected(itemId: Int) =
        setImageDrawable(if (itemId == id) selectedCircle else notSelectedCircle)

    /**
     * Shows a dialog for reset password with a recovery key ur.
     *
     * @param recoveryKeyUrl Recovery key link.
     */
    private fun showRecoveryKeyDialog(recoveryKeyUrl: String?) {
        Timber.d("link: %s", recoveryKeyUrl)

        val dialogBinding = DialogRecoveryKeyBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(
            requireContext())
            .setView(dialogBinding.root)
            .setTitle(R.string.title_dialog_insert_MK)
            .setMessage(R.string.text_dialog_insert_MK)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        with(dialog) {
            setOnShowListener {
                dialogBinding.editRecoveryKey.setOnEditorActionListener { _: TextView, actionId: Int, _: KeyEvent? ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        dialogBinding.performAction(recoveryKeyUrl)
                        true
                    } else {
                        Timber.d("Other IME%s", actionId)
                        false
                    }
                }

                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    dialogBinding.performAction(recoveryKeyUrl)
                }
            }
            show()
        }
    }

    /**
     * Checks if the typed data is correct.
     * If so, launches ChangePasswordActivity.
     * If not, shows a warning.
     *
     * @param recoveryKeyUrl Recovery key link.
     */
    private fun DialogRecoveryKeyBinding.performAction(recoveryKeyUrl: String?) {
        val key = editRecoveryKey.text.toString()

        if (key.isEmpty()) {
            inputRecoveryKey.error = getString(R.string.invalid_string)
            editRecoveryKey.requestFocus()
        } else {
            startChangePasswordActivity(Uri.parse(recoveryKeyUrl), key.trim { it <= ' ' })
        }
    }

    /**
     * Shows a dialog for parking account.
     *
     * @param parkAccountUrl Park account link.
     */
    private fun showParkAccountDialog(parkAccountUrl: String?) =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.park_account_dialog_title)
            .setMessage(R.string.park_account_text_last_step)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.park_account_button
            ) { _: DialogInterface?, _: Int ->
                startChangePasswordActivity(Uri.parse(parkAccountUrl), null)
            }
            .create()
            .show()

    /**
     * Launches ChangePasswordActivity.
     *
     * @param dataUri Park account or recovery key links.
     * @param key     Recovery key if required.
     */
    private fun startChangePasswordActivity(dataUri: Uri, key: String?) =
        startActivity(Intent(requireContext(), ChangePasswordActivity::class.java).apply {
            data = dataUri

            action = if (key != null) {
                putExtra(IntentConstants.EXTRA_MASTER_KEY, key)
                Constants.ACTION_RESET_PASS_FROM_LINK
            } else {
                Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT
            }
        })

    /**
     * Request Bluetooth Connect Permission for Meeting and Call when SDK >= 31
     *
     * @return false : permission granted, needn't request / true: should request permission
     */
    private fun requestBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission =
                hasPermissions(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasPermission) {
                return true
            }
        }
        return false
    }

    override fun onAttach(context: Context) {
        Timber.d("onAttach")
        super.onAttach(context)

        joinMeetingAsGuestLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
                if (result) {
                    Timber.d("onActivityResult: PERMISSION GRANTED")
                    isLoggingOut = false
                    PasteMeetingLinkGuestDialogFragment().show(childFragmentManager,
                        PasteMeetingLinkGuestDialogFragment.TAG)
                } else {
                    Timber.d("onActivityResult: PERMISSION DENIED")
                    (requireActivity() as BaseActivity).showSnackbar(Constants.PERMISSIONS_TYPE,
                        getString(R.string.meeting_bluetooth_connect_required_permissions_warning),
                        MegaApiJava.INVALID_HANDLE)
                }
            }
    }

    companion object {
        private const val EXTRA_RECOVERY_KEY_URL = "EXTRA_RECOVERY_KEY_URL"
        private const val EXTRA_PARK_ACCOUNT_URL = "EXTRA_PARK_ACCOUNT_URL"

        /**
         * Creates a new instance of the fragments and adds arguments.
         *
         * @param recoveryKeyUrl Recovery key link.
         * @param parkAccountUrl Park account link.
         * @return new instance.
         */
        fun newInstance(recoveryKeyUrl: String?, parkAccountUrl: String?): TourFragment {
            val fragment = TourFragment()
            val arguments = Bundle()

            recoveryKeyUrl?.let { arguments.putString(EXTRA_RECOVERY_KEY_URL, recoveryKeyUrl) }
            parkAccountUrl?.let { arguments.putString(EXTRA_PARK_ACCOUNT_URL, parkAccountUrl) }

            if (!arguments.isEmpty) {
                fragment.arguments = arguments
            }

            return fragment
        }
    }
}
