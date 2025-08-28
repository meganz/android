package mega.privacy.android.app.main.drawer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.databinding.NavigationViewLayoutBinding
import mega.privacy.android.app.extensions.openTransfersAndConsumeErrorStatus
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.NavigationDrawerManager
import mega.privacy.android.app.main.mapper.UserChatStatusIconMapper
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.avatar.view.Avatar
import mega.privacy.android.app.presentation.extensions.spanABTextFontColour
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.UnreadUserAlertsCheckType
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.app.presentation.manager.model.UserInfoUiState
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
import mega.privacy.android.app.presentation.verification.SMSVerificationActivity
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DeviceCenterEntrypointButtonEvent
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager drawer fragment
 *
 */
@AndroidEntryPoint
internal class ManagerDrawerFragment : Fragment() {
    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var userChatStatusIconMapper: UserChatStatusIconMapper

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val outMetrics: DisplayMetrics by lazy { resources.displayMetrics }
    private lateinit var drawerManager: NavigationDrawerManager
    private var _binding: NavigationViewLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManagerDrawerViewModel by viewModels()
    private val userInfoViewModel: UserInfoViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()

    private val transfersManagementViewModel: TransfersManagementViewModel by activityViewModels()

    private val listener = object : DrawerLayout.DrawerListener {
        override fun onDrawerOpened(drawerView: View) {
            updateAccountDetailsVisibleInfo()
        }

        override fun onDrawerClosed(drawerView: View) {}

        override fun onDrawerStateChanged(newState: Int) {}

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            managerViewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        drawerManager = context as? NavigationDrawerManager
            ?: throw IllegalStateException("Activity must implement NavigationDrawerManager")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return NavigationViewLayoutBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        collectFlows()
        registerView()
        updateAccountDetailsVisibleInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        drawerManager.removeDrawerListener(listener)
        _binding = null
    }

    private fun initView() {
        binding.navigationDrawerAccountInformationDisplayName.setMaxWidthEmojis(
            Util.dp2px(
                Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT.toFloat(),
                outMetrics
            )
        )
        drawerManager.addDrawerListener(listener)
        binding.navigationDrawerAddPhoneNumberButton.doOnLayout {
            val lineCount = binding.navigationDrawerAddPhoneNumberButton.layout?.lineCount ?: 0
            binding.navigationDrawerAddPhoneNumberIcon.isGone = lineCount > 1
        }
        managerViewModel.checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE)
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(
            userInfoViewModel.state,
        ) { state: UserInfoUiState ->
            updateUserNameNavigationView(state.fullName)
            setProfileAvatar(state.avatarContent)
            updateUserEmail(state.email)
            if (state.isTestPasswordRequired) {
                startActivity(Intent(requireActivity(), TestPasswordActivity::class.java))
                userInfoViewModel.onTestPasswordHandled()
            }
        }
        viewLifecycleOwner.collectFlow(
            targetFlow = viewModel.monitorMyAccountUpdateEvent,
            minActiveState = Lifecycle.State.CREATED
        ) {
            updateAccountDetailsVisibleInfo()
        }
        viewLifecycleOwner.collectFlow(viewModel.state) { uiState ->
            setContactStatus(uiState.userChatStatus)
            setDrawerLayout(uiState.isRootNodeExist && uiState.isConnected)
            binding.navigationDrawerAddPhoneNumberContainer.isVisible = uiState.canVerifyPhoneNumber
            binding.deviceCenterSection.isVisible = true
            binding.notificationSectionPromoTag.isVisible = uiState.showPromoTag
        }
        viewLifecycleOwner.collectFlow(managerViewModel.numUnreadUserAlerts) { result ->
            if (result.first != UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON) {
                setNotificationsTitleSection(result.second)
            }
        }
        viewLifecycleOwner.collectFlow(managerViewModel.incomingContactRequests) { pendingRequest ->
            setContactTitleSection(pendingRequest.size)
        }
    }

    private fun registerView() {
        binding.navigationDrawerAccountSection.setOnClickListener {
            openAccountScreen()
        }
        binding.myAccountSection.setOnClickListener { openAccountScreen() }
        binding.navigationDrawerAddPhoneNumberButton.setOnClickListener {
            val intent = Intent(requireActivity(), SMSVerificationActivity::class.java)
            startActivity(intent)
        }
        binding.settingsSection.setOnClickListener {
            drawerManager.closeDrawer()
            megaNavigator.openSettings(requireActivity())
        }
        binding.upgradeNavigationView.setOnClickListener {
            drawerManager.closeDrawer()
            megaNavigator.openUpgradeAccount(requireActivity())
            myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.MANAGER
        }
        binding.contactsSection.setOnClickListener {
            drawerManager.closeDrawer()
            startActivity(ContactsActivity.getListIntent(requireActivity()))
        }
        binding.notificationsSection.setOnClickListener { drawerManager.drawerItemClicked(DrawerItem.NOTIFICATIONS) }
        binding.deviceCenterSection.setOnClickListener {
            Analytics.tracker.trackEvent(DeviceCenterEntrypointButtonEvent)
            drawerManager.drawerItemClicked(DrawerItem.DEVICE_CENTER)
        }
        binding.transfersSection.setOnClickListener {
            megaNavigator.openTransfersAndConsumeErrorStatus(
                requireContext(),
                transfersManagementViewModel
            )
        }
        binding.rubbishBinSection.setOnClickListener { drawerManager.drawerItemClicked(DrawerItem.RUBBISH_BIN) }
        binding.offlineSection.setOnClickListener { drawerManager.drawerItemClicked(DrawerItem.OFFLINE) }
    }

    private fun updateUserEmail(email: String) {
        binding.navigationDrawerAccountInformationEmail.apply {
            isVisible = email.isNotEmpty()
            text = email
        }
    }

    private fun updateUserNameNavigationView(fullName: String?) {
        Timber.d("updateUserNameNavigationView")
        binding.navigationDrawerAccountInformationDisplayName.text = fullName
    }

    private fun setProfileAvatar(avatar: AvatarContent?) {
        avatar?.let {
            binding.navigationDrawerUserAccountPictureProfile.apply {
                setContent {
                    Avatar(modifier = Modifier.size(48.dp), content = avatar)
                }
            }
        }
    }

    private fun updateAccountDetailsVisibleInfo() {
        Timber.d("updateAccountDetailsVisibleInfo")
        val storageState = viewModel.getStorageState()
        if (isBusinessAccount) {
            binding.nvUsedSpaceLayout.visibility = View.GONE
            binding.upgradeNavigationView.visibility = View.GONE
            binding.settingsSeparator.visibility = View.GONE
            binding.businessLabel.visibility = View.VISIBLE
        } else {
            binding.businessLabel.visibility = View.GONE
            binding.upgradeNavigationView.isGone = myAccountInfo.accountType == Constants.PRO_FLEXI
            binding.proFlexiLabel.isVisible = binding.upgradeNavigationView.isGone
            binding.settingsSeparator.visibility = View.GONE
            var colorString = ColorUtils.getThemeColorHexString(
                requireContext(),
                com.google.android.material.R.attr.colorSecondary
            )
            when (storageState) {
                StorageState.Green -> {}
                StorageState.Orange -> colorString =
                    ColorUtils.getColorHexString(requireContext(), R.color.color_support_warning)

                StorageState.Red, StorageState.PayWall -> colorString =
                    ColorUtils.getColorHexString(requireContext(), R.color.color_support_error)

                else -> {}
            }
            val textToShow = String.format(
                resources.getString(R.string.used_space),
                myAccountInfo.usedFormatted,
                myAccountInfo.totalFormatted
            ).spanABTextFontColour(
                context = requireContext(),
                aColourHex = colorString,
                bColourHex = ColorUtils.getThemeColorHexString(
                    requireContext(),
                    android.R.attr.textColorPrimary
                ),
            )
            binding.navigationDrawerSpace.text = textToShow
            val progress: Int = myAccountInfo.usedPercentage
            val usedSpace: Long = myAccountInfo.usedStorage
            Timber.d("Progress: %d, Used space: %d", progress, usedSpace)
            binding.managerUsedSpaceBar.progress = progress
            binding.nvUsedSpaceLayout.isVisible =
                myAccountInfo.accountType != Constants.PRO_FLEXI && progress >= 0 && usedSpace >= 0
        }
        val resId = when (storageState) {
            StorageState.Orange -> R.drawable.custom_progress_bar_horizontal_warning
            StorageState.Red, StorageState.PayWall ->
                R.drawable.custom_progress_bar_horizontal_exceed

            else -> R.drawable.custom_progress_bar_horizontal_ok
        }
        val drawable = ResourcesCompat.getDrawable(resources, resId, null)
        binding.managerUsedSpaceBar.progressDrawable = drawable
    }

    private fun setNotificationsTitleSection(unread: Int) {
        binding.notificationSectionText.text = if (unread == 0) {
            getString(R.string.title_properties_chat_contact_notifications)
        } else {
            getString(R.string.section_notification_with_unread, unread)
                .spanABTextFontColour(
                    requireContext(),
                    ColorUtils.getColorHexString(requireContext(), R.color.color_button_brand)
                )
        }
    }

    private fun setContactStatus(status: UserChatStatus) {
        val isDarkTheme = Util.isDarkMode(requireContext())
        val resId = userChatStatusIconMapper(status, isDarkTheme)
        binding.contactState.isVisible = resId != 0
        if (resId != 0) {
            binding.contactState.setImageResource(resId)
        }
    }

    private val isBusinessAccount: Boolean
        get() = megaApi.isBusinessAccount && myAccountInfo.accountType == Constants.BUSINESS

    private fun openAccountScreen() {
        if (viewModel.isConnected) {
            drawerManager.closeDrawer()
            startActivity(Intent(requireActivity(), MyAccountActivity::class.java))
        }
    }

    private fun setDrawerLayout(isEnable: Boolean) {
        binding.nvUsedSpaceLayout.isVisible = isEnable
        binding.myAccountSection.isEnabled = isEnable
        binding.contactsSection.isEnabled = isEnable
        binding.rubbishBinSection.isEnabled = isEnable
        binding.upgradeNavigationView.isEnabled = isEnable
        binding.notificationsSection.isEnabled = isEnable
        binding.settingsSection.isEnabled = isEnable
        binding.deviceCenterSection.isEnabled = isEnable
        val alpha = if (isEnable) 1f else 0.38f
        with(alpha) {
            binding.myAccountSectionText.alpha = this
            binding.contactsSectionText.alpha = this
            binding.notificationSectionText.alpha = this
            binding.rubbishBinSectionText.alpha = this
            binding.settingsSectionText.alpha = this
            binding.deviceCenterSection.alpha = this
        }
    }

    private fun setContactTitleSection(pendingRequestCount: Int) {
        binding.contactsSectionText.text = if (pendingRequestCount == 0) {
            getString(sharedR.string.general_section_contacts)
        } else {
            getString(R.string.section_contacts_with_notification, pendingRequestCount)
                .spanABTextFontColour(
                    requireContext(),
                    ColorUtils.getColorHexString(requireContext(), R.color.color_button_brand)
                )
        }
    }
}