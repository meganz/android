package mega.privacy.android.app.presentation.myaccount

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.myaccount.view.MyAccountHomeView
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChangeApiServerUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * My Account Fragment
 */
@AndroidEntryPoint
class MyAccountFragment : Fragment(), MyAccountHomeViewActions {
    companion object {
        private const val CHANGE_API_SERVER_SHOWN = "CHANGE_API_SERVER_SHOWN"
    }

    private var _binding: FragmentMyAccountBinding? = null
    private val binding get() = _binding!!

    @Deprecated("Should be removed later after refactor")
    private val activityViewModel: MyAccountViewModel by activityViewModels()
    private val viewModel: MyAccountHomeViewModel by viewModels()
    private var changeApiServerDialog: AlertDialog? = null
    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null

    /**
     * Get system's default theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMyAccountBinding.inflate(layoutInflater)
        val view = binding.root

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    MyAccountHomeView(
                        uiState = uiState,
                        uiActions = this@MyAccountFragment,
                        navController = findNavController()
                    )
                }
            }
        }

        return view
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CHANGE_API_SERVER_SHOWN, false)) {
                showApiServerDialog()
            }
        }
    }

    /**
     * onResume
     */
    override fun onResume() {
        super.onResume()
        viewModel.refreshAccountInfo()
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        super.onDestroy()
        changeApiServerDialog?.dismiss()
    }

    private fun setupObservers() {
        activityViewModel.onUpdateAccountDetails()
            .observe(viewLifecycleOwner) { viewModel.refreshAccountInfo() }
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CHANGE_API_SERVER_SHOWN, isAlertDialogShown(changeApiServerDialog))
        super.onSaveInstanceState(outState)
    }

    override val isPhoneNumberDialogShown: Boolean
        get() = phoneNumberBottomSheet.isBottomSheetDialogShown()

    override fun showApiServerDialog() {
        changeApiServerDialog =
            ChangeApiServerUtil.showChangeApiServerDialog(requireActivity())
    }

    override fun onClickUserAvatar() {
        if (CallUtil.isNecessaryDisableLocalCamera() != Constants.INVALID_VALUE.toLong()) {
            CallUtil.showConfirmationOpenCamera(requireActivity(), Constants.ACTION_OPEN_QR, false)
        } else {
            val intent = if (activityViewModel.isFeatureEnabled(AppFeatures.QRCodeCompose)) {
                Intent(requireContext(), QRCodeComposeActivity::class.java)
            } else {
                Intent(requireContext(), QRCodeActivity::class.java)
            }
            intent.putExtra(Constants.OPEN_SCAN_QR, false)
            startActivity(intent)
        }
    }

    override fun onEditProfile() =
        findNavController().navigate(R.id.action_my_account_to_edit_profile)

    override fun onClickUsageMeter() =
        findNavController().navigate(R.id.action_my_account_to_my_account_usage)

    override fun onUpgradeAccount() {
        findNavController().navigate(R.id.action_my_account_to_upgrade)
        activityViewModel.setOpenUpgradeFrom()
    }

    override fun onAddPhoneNumber() =
        findNavController().navigate(R.id.action_my_account_to_add_phone_number)

    override fun showPhoneNumberDialog() {
        if (isPhoneNumberDialogShown.not()) {
            phoneNumberBottomSheet = PhoneNumberBottomSheetDialogFragment()
            activity?.supportFragmentManager?.let { fragmentManager ->
                phoneNumberBottomSheet?.let { fragment ->
                    fragment.show(
                        fragmentManager,
                        fragment.tag
                    )
                }
            }
        }
    }

    override fun onBackupRecoveryKey() =
        findNavController().navigate(R.id.action_my_account_to_export_recovery_key)

    override fun onClickContacts() =
        startActivity(ContactsActivity.getListIntent(requireContext()))

    override fun onClickAchievements() = viewModel.navigateToAchievements()

    override fun onPageScroll(isAtTop: Boolean) {
        activityViewModel.setElevation(isAtTop.not())
    }

    override fun resetAchievementsNavigationEvent() = viewModel.resetNavigationToAchievements()

    override fun resetUserMessage() = viewModel.resetUserMessage()
}