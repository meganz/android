package mega.privacy.android.app.presentation.myaccount

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.myAccount.toAccountAttributes
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.ActiveFragment
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.businessUpdate
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.update
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.updateBusinessOrProFlexi
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChangeApiServerUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiAndroid
import java.io.File
import javax.inject.Inject

/**
 * My Account Fragment
 */
@AndroidEntryPoint
class MyAccountFragment : Fragment(), Scrollable {
    companion object {
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800
        private const val CLICKS_TO_CHANGE_API_SERVER = 5
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_DELAY = 500L
        private const val CHANGE_API_SERVER_SHOWN = "CHANGE_API_SERVER_SHOWN"
    }

    /**
     * Mega API
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * getFeatureFlagValueUseCase
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * Util to modify string
     */
    @Inject
    lateinit var stringUtilWrapper: StringUtilWrapper

    private var numOfClicksLastSession = 0

    @Deprecated("Should be removed later after refactor")
    private val activityViewModel: MyAccountViewModel by activityViewModels()
    private val viewModel: MyAccountHomeViewModel by viewModels()

    /**
     * Checks if business payment attention is needed.
     *
     * @return True if business payment attention is needed, false otherwise.
     */
    private val isBusinessPaymentAttentionNeeded: Boolean
        get() = viewModel.uiState.value.isBusinessAccount &&
                viewModel.uiState.value.isMasterBusinessAccount &&
                viewModel.uiState.value.isBusinessStatusActive.not()

    /**
     * Is already registered phone number
     *
     * @return
     */
    private val isAlreadyRegisteredPhoneNumber: Boolean
        get() = viewModel.uiState.value.verifiedPhoneNumber.isNullOrEmpty().not()

    private lateinit var binding: FragmentMyAccountBinding
    private lateinit var usageBinding: MyAccountUsageContainerBinding
    private lateinit var paymentAlertBinding: MyAccountPaymentInfoContainerBinding

    private var messageResultCallback: MessageResultCallback? = null

    private var changeApiServerDialog: AlertDialog? = null

    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null

    private val gettingInfo by lazy { getString(R.string.recovering_info) }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.refreshAccountInfo()
        messageResultCallback = activity as? MessageResultCallback
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyAccountBinding.inflate(layoutInflater)
        usageBinding = binding.usageView
        paymentAlertBinding = binding.paymentAlert
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            setupView()
            setupObservers()
            collectFlow()
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CHANGE_API_SERVER_SHOWN, false)) {
                showChangeAPIServerDialog()
            }
        }
    }

    private fun setupView() {
        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }
        checkScroll()

        usageBinding.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_my_account_to_my_account_usage,
                null,
                null,
                FragmentNavigatorExtras(usageBinding.root to usageBinding.root.transitionName)
            )
        }
        with(binding) {
            lastSessionLayout.setOnClickListener {
                if (incrementLastSessionClick()) {
                    showChangeAPIServerDialog()
                }
            }
            myAccountThumbnail.setOnClickListener { openQR() }

            myAccountTextInfoLayout.setOnClickListener {
                findNavController().navigate(R.id.action_my_account_to_edit_profile)
            }
            backupRecoveryKeyLayout.setOnClickListener {
                findNavController().navigate(R.id.action_my_account_to_export_recovery_key)
            }
            contactsLayout.setOnClickListener {
                startActivity(ContactsActivity.getListIntent(requireContext()))
            }
        }
    }

    /**
     * checkScroll
     */
    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        activityViewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        activityViewModel.onUpdateAccountDetails()
            .observe(viewLifecycleOwner) { viewModel.refreshAccountInfo() }
    }

    private fun collectFlow() {
        viewLifecycleOwner.collectFlow(viewModel.uiState) { state ->
            binding.nameText.text = state.name
            binding.emailText.text = state.email

            setupPhoneNumber(
                canVerifyPhoneNumber = state.canVerifyPhoneNumber,
                alreadyRegisteredPhoneNumber = state.verifiedPhoneNumber != null,
                registeredPhoneNumber = state.verifiedPhoneNumber,
                isAchievementsEnabled = state.isAchievementsEnabled,
                bonusStorageSms = state.bonusStorageSms
            )
            setAccountDetails(state)
            setVisibleContacts(state.visibleContacts)
            setProfileAvatar(state.avatar, state.avatarColor)
            setAchievements(state.isAchievementsEnabled)
            setLastSession(state.lastSession)
        }
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CHANGE_API_SERVER_SHOWN, isAlertDialogShown(changeApiServerDialog))
        super.onSaveInstanceState(outState)
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

    private fun setProfileAvatar(avatar: File?, avatarColor: Int?) {
        avatar?.takeIf { avatar.exists() && avatar.length() > 0 }
            ?.let { BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options()) }
            ?.let { bitmap -> binding.myAccountThumbnail.setImageBitmap(bitmap) }
            ?: run { setDefaultAvatar(avatarColor) }
    }

    private fun setDefaultAvatar(avatarColor: Int?) {
        if (avatarColor == null) return

        binding.myAccountThumbnail.setImageBitmap(
            AvatarUtil.getDefaultAvatar(
                avatarColor,
                viewModel.uiState.value.name,
                AVATAR_SIZE,
                true
            )
        )
    }

    private fun setAccountDetails(state: MyAccountHomeUIState) {
        if (state.accountType == null) return

        if (state.isMasterBusinessAccount) {
            when (state.isBusinessStatusActive) {
                true -> setupPaymentDetails(state)
                false -> {
                    paymentAlertBinding.businessUpdate(
                        requireContext(),
                        state.subscriptionRenewTime,
                        state.proExpirationTime,
                        state.hasRenewableSubscription,
                        state.hasExpireAbleSubscription,
                        megaApi,
                        false,
                        ActiveFragment.MY_ACCOUNT
                    )
                    expandPaymentInfoIfNeeded(state)
                }
            }
        }

        if (state.isBusinessAccount.not()) {
            setupPaymentDetails(state)
        }

        if (state.isBusinessAccount || state.accountType == AccountType.PRO_FLEXI) {
            usageBinding.updateBusinessOrProFlexi(
                requireContext(),
                formatSize(state.usedStorage),
                formatSize(100000)
            )
        } else {
            usageBinding.update(
                context = requireContext(),
                isFreeAccount = state.accountType == AccountType.FREE,
                totalStorage = formatSize(state.totalStorage),
                totalTransfer = formatSize(state.totalTransfer),
                usedStorage = formatSize(state.usedStorage),
                usedStoragePercentage = state.usedStoragePercentage,
                usedTransfer = formatSize(state.usedTransfer),
                usedTransferPercentage = state.usedTransferPercentage,
            )
        }

        binding.achievementsLayout.isVisible = state.isBusinessAccount.not()
        binding.businessAccountManagementText.isVisible =
            state.isBusinessAccount && state.isMasterBusinessAccount
        binding.upgradeButton.isVisible =
            state.isBusinessAccount.not() && state.isProFlexiAccount.not()

        state.accountType.toAccountAttributes().let { account ->
            binding.upgradeButton.apply {
                isEnabled = !state.isBusinessAccount
                text = getString(
                    when {
                        state.isBusinessAccount && state.isMasterBusinessAccount -> R.string.admin_label
                        state.isBusinessAccount && !state.isMasterBusinessAccount -> R.string.user_label
                        else -> R.string.my_account_upgrade_pro
                    }
                )
                if (state.isBusinessAccount.not()) {
                    setOnClickListener {
                        findNavController().navigate(R.id.action_my_account_to_upgrade)
                        activityViewModel.setOpenUpgradeFrom()
                    }
                }
            }
            binding.accountTypeText.text = getString(account.description)
            binding.accountTypeLayout.background = ColorUtils.tintIcon(
                context = requireContext(),
                drawableId = R.drawable.background_account_type,
                color = ContextCompat.getColor(
                    requireContext(),
                    account.background
                )
            )
            binding.accountTypeIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    account.icon
                )
            )
        }
    }

    private fun formatSize(size: Long?): String {
        return Util.getSizeString(size ?: 0, requireContext())
    }

    private fun setVisibleContacts(visibleContacts: Int?) {
        visibleContacts?.let {
            binding.contactsSubtitle.text = resources.getQuantityString(
                R.plurals.my_account_connections,
                it,
                it
            )
        }
    }

    private fun setAchievements(isAchievementsEnabled: Boolean) {
        binding.achievementsLayout.apply {
            isVisible = isAchievementsEnabled

            if (!isVisible) {
                return@apply
            }

            setOnClickListener {
                if (Util.isOnline(requireContext())) {
                    findNavController().navigate(R.id.action_my_account_to_achievements)
                } else {
                    messageResultCallback?.show(
                        getString(R.string.error_server_connection_problem)
                    )
                }
            }
        }
    }

    private fun setLastSession(lastSession: Long?) {
        binding.lastSessionSubtitle.text =
            lastSession?.let {
                TimeUtils.formatDateAndTime(
                    context,
                    it,
                    TimeUtils.DATE_LONG_FORMAT
                )
            } ?: gettingInfo
    }

    /**
     * Increment last session click
     *
     * @return
     */
    private fun incrementLastSessionClick(): Boolean {
        numOfClicksLastSession++

        if (numOfClicksLastSession < CLICKS_TO_CHANGE_API_SERVER)
            return false

        numOfClicksLastSession = 0
        return true
    }

    private fun showChangeAPIServerDialog() {
        changeApiServerDialog =
            ChangeApiServerUtil.showChangeApiServerDialog(requireActivity())
    }

    private fun setupPaymentDetails(state: MyAccountHomeUIState) {
        if (paymentAlertBinding.update(
                renewTime = state.subscriptionRenewTime,
                expirationTime = state.proExpirationTime,
                hasRenewableSubscription = state.hasRenewableSubscription,
                hasExpirableSubscription = state.hasExpireAbleSubscription,
                fragment = ActiveFragment.MY_ACCOUNT
            )
        ) {
            expandPaymentInfoIfNeeded(state)
        }
    }

    private fun expandPaymentInfoIfNeeded(state: MyAccountHomeUIState) {
        if (!shouldShowPaymentInfo(state))
            return

        val v = paymentAlertBinding.root
        v.isVisible = false

        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)

        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)

        val targetHeight = v.measuredHeight

        v.layoutParams.height = 1

        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) LinearLayout.LayoutParams.WRAP_CONTENT
                    else (targetHeight * interpolatedTime).toInt()

                v.isVisible = true
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = ANIMATION_DURATION
        a.startOffset = ANIMATION_DELAY
        v.startAnimation(a)
    }

    private fun shouldShowPaymentInfo(state: MyAccountHomeUIState): Boolean {
        val timeToCheck =
            if (state.hasRenewableSubscription) state.subscriptionRenewTime else state.proExpirationTime

        val currentTime = System.currentTimeMillis() / 1000

        return isBusinessPaymentAttentionNeeded || timeToCheck.minus(currentTime) <= TIME_TO_SHOW_PAYMENT_INFO
    }


    private fun setupPhoneNumber(
        canVerifyPhoneNumber: Boolean = Util.canVoluntaryVerifyPhoneNumber(),
        alreadyRegisteredPhoneNumber: Boolean = isAlreadyRegisteredPhoneNumber,
        registeredPhoneNumber: String?,
        isAchievementsEnabled: Boolean,
        bonusStorageSms: Long,
    ) {
        binding.phoneText.apply {
            if (alreadyRegisteredPhoneNumber) {
                isVisible = true
                text = registeredPhoneNumber
            } else {
                isVisible = false
            }
        }

        val addPhoneNumberVisible = canVerifyPhoneNumber && !alreadyRegisteredPhoneNumber

        binding.addPhoneNumberLayout.apply {
            isVisible = addPhoneNumberVisible

            setOnClickListener {
                if (canVerifyPhoneNumber) {
                    findNavController().navigate(R.id.action_my_account_to_add_phone_number)
                } else if (!phoneNumberBottomSheet.isBottomSheetDialogShown()) {
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
        }

        if (addPhoneNumberVisible) {
            binding.addPhoneSubtitle.text =
                if (isAchievementsEnabled) getString(
                    R.string.sms_add_phone_number_dialog_msg_achievement_user,
                    stringUtilWrapper.getSizeString(
                        size = bonusStorageSms,
                        context = requireContext()
                    )
                ) else getString(
                    R.string.sms_add_phone_number_dialog_msg_non_achievement_user
                )
        }
    }

    private fun openQR() {
        if (CallUtil.isNecessaryDisableLocalCamera() != Constants.INVALID_VALUE.toLong()) {
            CallUtil.showConfirmationOpenCamera(requireActivity(), Constants.ACTION_OPEN_QR, false)
        } else {
            startActivity(
                Intent(requireContext(), QRCodeActivity::class.java)
                    .putExtra(Constants.OPEN_SCAN_QR, false)
            )
        }
    }

    /**
     * MessageResultCallback
     */
    interface MessageResultCallback {
        /**
         * show message
         */
        fun show(message: String)
    }
}