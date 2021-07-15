package mega.privacy.android.app.myAccount

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.editProfile.EditProfileActivity
import mega.privacy.android.app.smsVerification.SMSVerificationActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaUser
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MyAccountFragment : Fragment(), Scrollable {

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_DELAY = 500L
        private const val CHANGE_API_SERVER_SHOWN = "CHANGE_API_SERVER_SHOWN"
    }

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val viewModel: MyAccountViewModel by activityViewModels()

    private lateinit var binding: FragmentMyAccountBinding
    private lateinit var usageBinding: MyAccountUsageContainerBinding
    private lateinit var paymentAlertBinding: MyAccountPaymentInfoContainerBinding

    private var messageResultCallback: MessageResultCallback? = null

    private var changeApiServerDialog: AlertDialog? = null

    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null

    private var gettingInfo = StringResourcesUtils.getString(R.string.recovering_info)

    private val profileAvatarUpdatedObserver = Observer<Boolean> { setupAvatar(false) }

    private val nameUpdatedObserver =
        Observer<Boolean> { binding.nameText.text = viewModel.getName() }

    private val emailUpdatedObserver =
        Observer<Boolean> { binding.emailText.text = viewModel.getEmail() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageResultCallback = activity as? MessageResultCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAccountBinding.inflate(layoutInflater)
        usageBinding = binding.usageView
        paymentAlertBinding = binding.paymentAlert
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupObservers()

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CHANGE_API_SERVER_SHOWN, false)) {
                showChangeAPIServerDialog()
            }
        }
    }

    private fun setupView() {
        ListenScrollChangesHelper().addViewToListen(
            binding.scrollView
        ) { _, _, _, _, _ -> checkScroll() }

        checkScroll()

        setupAvatar(true)

        binding.myAccountThumbnail.setOnClickListener { viewModel.openQR(requireActivity()) }

        binding.nameText.text = viewModel.getName()
        binding.emailText.text = viewModel.getEmail()

        setupPhoneNumber()
        setupAccountDetails()

        binding.backupRecoveryKeyLayout.setOnClickListener { viewModel.exportMK(requireContext()) }

        setupAchievements()
        setupLastSession()
        setupContactConnections()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        LiveEventBus.get(Constants.EVENT_AVATAR_CHANGE, Boolean::class.java)
            .observeForever(profileAvatarUpdatedObserver)

        LiveEventBus.get(EventConstants.EVENT_USER_NAME_UPDATED, Boolean::class.java)
            .observeForever(nameUpdatedObserver)

        LiveEventBus.get(EventConstants.EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
            .observeForever(emailUpdatedObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CHANGE_API_SERVER_SHOWN, isAlertDialogShown(changeApiServerDialog))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        LiveEventBus.get(Constants.EVENT_AVATAR_CHANGE, Boolean::class.java)
            .removeObserver(profileAvatarUpdatedObserver)

        LiveEventBus.get(EventConstants.EVENT_USER_NAME_UPDATED, Boolean::class.java)
            .removeObserver(nameUpdatedObserver)

        LiveEventBus.get(EventConstants.EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
            .removeObserver(emailUpdatedObserver)

        changeApiServerDialog?.dismiss()
    }

    /**
     * Checks if an avatar file already exist for the current account.
     *
     * @param retry True if should request for avatar if it's not available, false otherwise.
     */
    fun setupAvatar(retry: Boolean) {
        val avatar =
            CacheFolderManager.buildAvatarFile(
                requireContext(),
                megaApi.myEmail + FileUtil.JPG_EXTENSION
            )

        if (avatar != null) {
            setProfileAvatar(avatar, retry)
        } else {
            setDefaultAvatar()
        }
    }

    /**
     * Sets the avatar file if available.
     * If not, requests it if should retry, sets the default one if not.
     */
    fun setProfileAvatar(avatar: File, retry: Boolean) {
        val avatarBitmap: Bitmap?

        if (avatar.exists() && avatar.length() > 0) {
            avatarBitmap = BitmapFactory.decodeFile(avatar.absolutePath, BitmapFactory.Options())

            if (avatarBitmap == null) {
                avatar.delete()
            } else {
                binding.myAccountThumbnail.setImageBitmap(avatarBitmap)
                return
            }
        }

        if (retry) {
            viewModel.getAvatar(requireContext()) { success -> showAvatarResult(success) }
        } else setDefaultAvatar()
    }

    /**
     * Sets as avatar the default one.
     */
    private fun setDefaultAvatar() {
        binding.myAccountThumbnail.setImageBitmap(
            AvatarUtil.getDefaultAvatar(
                AvatarUtil.getColorAvatar(megaApi.myUser),
                viewModel.getName(),
                Constants.AVATAR_SIZE,
                true
            )
        )
    }

    /**
     * Sets as avatar the current avatar if has been get, the default one if not.
     */
    private fun showAvatarResult(success: Boolean) {
        if (success) {
            setupAvatar(false)
        } else {
            setDefaultAvatar()
        }
    }

    private fun setupAchievements() {
        binding.achievementsLayout.apply {
            isVisible = megaApi.isAchievementsEnabled

            if (!isVisible) {
                return@apply
            }

            setOnClickListener {
                if (Util.isOnline(requireContext())) {
                    startActivity(Intent(requireContext(), AchievementsActivity::class.java))
                } else {
                    messageResultCallback?.show(
                        StringResourcesUtils.getString(R.string.error_server_connection_problem)
                    )
                }
            }
        }
    }

    private fun setupLastSession() {
        binding.lastSessionLayout.setOnClickListener {
            if (viewModel.incrementLastSessionClick()) {
                showChangeAPIServerDialog()
            }
        }
    }

    private fun showChangeAPIServerDialog() {
        changeApiServerDialog =
            ChangeApiServerUtil.showChangeApiServerDialog(requireActivity(), megaApi)
    }

    fun setupContactConnections() {
        val contacts = megaApi.contacts
        val visibleContacts = ArrayList<MegaUser>()

        for (contact in contacts.indices) {
            if (contacts[contact].visibility == MegaUser.VISIBILITY_VISIBLE
                || megaApi.getInShares(contacts[contact]).size > 0
            ) {
                visibleContacts.add(contacts[contact])
            }
        }

        binding.contactsSubtitle.text = StringResourcesUtils.getQuantityString(
            R.plurals.my_account_connections,
            visibleContacts.size,
            visibleContacts.size
        )
    }

    /**
     * Updates the edit view.
     *
     * @param editable True if the account can be edited, false otherwise.
     */
    private fun setupEditProfile(editable: Boolean) {
        binding.nameText.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (editable) R.drawable.ic_view_edit_profile else 0,
            0
        )

        binding.myAccountTextInfoLayout.setOnClickListener {
            if (editable) {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
        }
    }

    fun setupAccountDetails() {
        binding.lastSessionSubtitle.text =
            if (viewModel.getLastSession().isNotEmpty()) viewModel.getLastSession()
            else gettingInfo

        if (megaApi.isBusinessAccount) {
            setupBusinessAccount()
            return
        }

        setupEditProfile(true)

        binding.upgradeButton.apply {
            isEnabled = true
            text = StringResourcesUtils.getString(R.string.my_account_upgrade_pro)

            setOnClickListener { viewModel.upgradeAccount(requireContext()) }
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = true

        binding.accountTypeText.text = StringResourcesUtils.getString(
            when (viewModel.getAccountType()) {
                Constants.FREE -> R.string.free_account
                Constants.PRO_I -> R.string.pro1_account
                Constants.PRO_II -> R.string.pro2_account
                Constants.PRO_III -> R.string.pro3_account
                Constants.PRO_LITE -> R.string.prolite_account
                else -> R.string.recovering_info
            }
        )

        binding.accountTypeLayout.background = ColorUtils.tintIcon(
            requireContext(), R.drawable.background_account_type, ContextCompat.getColor(
                requireContext(),
                when (viewModel.getAccountType()) {
                    Constants.FREE -> R.color.green_400_green_300
                    Constants.PRO_I -> R.color.orange_600_orange_300
                    Constants.PRO_II, Constants.PRO_III, Constants.PRO_LITE -> R.color.red_300_red_200
                    else -> R.color.white_black
                }
            )
        )

        setupPaymentDetails()

        usageBinding.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_my_account_to_my_account_usage,
                null,
                null,
                FragmentNavigatorExtras(usageBinding.root to usageBinding.root.transitionName)
            )
        }

        usageBinding.storageProgressPercentage.isVisible = true
        usageBinding.storageProgressBar.isVisible = true
        usageBinding.businessStorageImage.isVisible = false
        usageBinding.transferProgressPercentage.isVisible = true
        usageBinding.transferProgressBar.isVisible = true
        usageBinding.businessTransferImage.isVisible = false

        binding.businessAccountManagementText.isVisible = false

        usageBinding.transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedStorage().isEmpty()) {
            usageBinding.storageProgressPercentage.isVisible = false
            usageBinding.storageProgressBar.progress = 0
            usageBinding.storageProgress.text = gettingInfo
        } else {
            usageBinding.storageProgressPercentage.isVisible = true
            usageBinding.storageProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedStoragePercentage()
            )

            usageBinding.storageProgressBar.progress =
                viewModel.getUsedStoragePercentage()
            usageBinding.storageProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )
        }

        if (viewModel.getUsedTransfer().isEmpty()) {
            usageBinding.transferProgressPercentage.isVisible = false
            usageBinding.transferProgressBar.progress = 0
            usageBinding.transferProgress.text = gettingInfo
        } else {
            usageBinding.transferProgressPercentage.isVisible = true
            usageBinding.transferProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedTransferPercentage()
            )

            usageBinding.transferProgressBar.progress =
                viewModel.getUsedTransferPercentage()
            usageBinding.transferProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
            )
        }
    }

    private fun setupBusinessAccount() {
        binding.accountTypeText.text = StringResourcesUtils.getString(R.string.business_label)
        binding.upgradeButton.apply {
            isEnabled = false
            text = StringResourcesUtils.getString(
                if (megaApi.isMasterBusinessAccount) R.string.admin_label
                else R.string.user_label
            )
        }

        binding.accountTypeLayout.background = ColorUtils.tintIcon(
            requireContext(), R.drawable.background_account_type, ContextCompat.getColor(
                requireContext(),
                R.color.blue_400_blue_300
            )
        )

        if (megaApi.isMasterBusinessAccount) {
            when (megaApi.businessStatus) {
                MegaApiJava.BUSINESS_STATUS_EXPIRED, MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD -> {
                    paymentAlertBinding.renewExpiryText.isVisible = false
                    paymentAlertBinding.renewExpiryDateText.isVisible = false
                    paymentAlertBinding.businessStatusText.apply {
                        isVisible = true

                        text = StringResourcesUtils.getString(
                            if (megaApi.businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED) R.string.payment_overdue_label
                            else R.string.payment_required_label
                        )
                    }

                    expandPaymentInfoIfNeeded()
                }
                else -> setupPaymentDetails() //BUSINESS_STATUS_ACTIVE
            }

            binding.businessAccountManagementText.isVisible = true
            setupEditProfile(true)
        } else {
            binding.businessAccountManagementText.isVisible = false
            setupEditProfile(false)
        }

        usageBinding.storageProgressPercentage.isVisible = false
        usageBinding.storageProgressBar.isVisible = false
        usageBinding.businessStorageImage.isVisible = true

        usageBinding.storageProgress.text =
            if (viewModel.getUsedStorage().isEmpty()) gettingInfo
            else viewModel.getUsedStorage()

        usageBinding.transferProgressPercentage.isVisible = false
        usageBinding.transferProgressBar.isVisible = false
        usageBinding.businessTransferImage.isVisible = true

        usageBinding.transferProgress.text =
            if (viewModel.getUsedTransfer().isEmpty()) gettingInfo
            else viewModel.getUsedTransfer()

        binding.achievementsLayout.isVisible = false
    }

    private fun setupPaymentDetails() {
        paymentAlertBinding.businessStatusText.isVisible = false

        if (viewModel.hasRenewableSubscription() || viewModel.hasExpirableSubscription()) {
            paymentAlertBinding.renewExpiryText.isVisible = true
            paymentAlertBinding.renewExpiryDateText.isVisible = true

            paymentAlertBinding.renewExpiryText.text = StringResourcesUtils.getString(
                if (viewModel.hasRenewableSubscription()) R.string.renews_on else R.string.expires_on
            )

            paymentAlertBinding.renewExpiryDateText.text = TimeUtils.formatDate(
                if (viewModel.hasRenewableSubscription()) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                TimeUtils.DATE_MM_DD_YYYY_FORMAT
            )

            expandPaymentInfoIfNeeded()
        }
    }

    /**
     * Shows the payment info if the subscriptions is almost to renew or expiry.
     */
    fun expandPaymentInfoIfNeeded() {
        if (!viewModel.shouldShowPaymentInfo())
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

    private fun setupPhoneNumber() {
        val canVerifyPhoneNumber = Util.canVoluntaryVerifyPhoneNumber()
        val alreadyRegisteredPhoneNumber = viewModel.isAlreadyRegisteredPhoneNumber()

        binding.phoneText.apply {
            if (alreadyRegisteredPhoneNumber) {
                isVisible = true
                text = viewModel.getRegisteredPhoneNumber()
            } else {
                isVisible = false
            }
        }

        val addPhoneNumberVisible = canVerifyPhoneNumber && !alreadyRegisteredPhoneNumber

        binding.addPhoneNumberLayout.apply {
            isVisible = addPhoneNumberVisible

            setOnClickListener {
                if (Util.canVoluntaryVerifyPhoneNumber()) {
                    startActivity(Intent(requireContext(), SMSVerificationActivity::class.java))
                } else if (!ModalBottomSheetUtil.isBottomSheetDialogShown(phoneNumberBottomSheet)) {
                    phoneNumberBottomSheet = PhoneNumberBottomSheetDialogFragment()
                    activity?.supportFragmentManager?.let { fragmentManager ->
                        phoneNumberBottomSheet!!.show(
                            fragmentManager,
                            phoneNumberBottomSheet!!.tag
                        )
                    }
                }
            }
        }

        if (addPhoneNumberVisible) {
            binding.addPhoneSubtitle.text =
                if (megaApi.isAchievementsEnabled) StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_achievement_user,
//                    (mActivity as ManagerActivityLollipop).bonusStorageSMS
                    ""
                ) else StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_non_achievement_user
                )
        }
    }

    interface MessageResultCallback {
        fun show(message: String)
    }
}