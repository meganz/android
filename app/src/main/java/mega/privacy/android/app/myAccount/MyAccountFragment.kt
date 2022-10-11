package mega.privacy.android.app.myAccount

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH_PHONE_NUMBER
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_EMAIL_UPDATED
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.ActiveFragment
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.updateBusinessOrProFlexi
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.businessUpdate
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.update
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ChangeApiServerUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.PRO_FLEXI
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
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

    private val gettingInfo by lazy { StringResourcesUtils.getString(R.string.recovering_info) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageResultCallback = activity as? MessageResultCallback
    }

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
        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        checkScroll()

        setupAvatar(true)

        binding.myAccountThumbnail.setOnClickListener { viewModel.openQR(requireActivity()) }

        binding.myAccountTextInfoLayout.setOnClickListener {
            findNavController().navigate(R.id.action_my_account_to_edit_profile)
        }

        binding.nameText.text = viewModel.getName()
        binding.emailText.text = viewModel.getEmail()

        setupPhoneNumber()
        setupAccountDetails()

        binding.backupRecoveryKeyLayout.setOnClickListener {
            findNavController().navigate(R.id.action_my_account_to_export_recovery_key)
        }

        setupAchievements()
        setupLastSession()
        setupContactConnections()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.onMyAvatarFileChanged.flowWithLifecycle(viewLifecycleOwner.lifecycle,
                Lifecycle.State.STARTED)
                .collect {
                    setupAvatar(true)
                }
        }

        LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java)
            .observe(viewLifecycleOwner) { binding.nameText.text = viewModel.getName() }

        LiveEventBus.get(EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
            .observe(viewLifecycleOwner) { binding.emailText.text = viewModel.getEmail() }

        LiveEventBus.get(EVENT_REFRESH_PHONE_NUMBER, Boolean::class.java)
            .observe(viewLifecycleOwner) { setupPhoneNumber() }

        viewModel.onUpdateAccountDetails().observe(viewLifecycleOwner) { setupAccountDetails() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CHANGE_API_SERVER_SHOWN, isAlertDialogShown(changeApiServerDialog))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        changeApiServerDialog?.dismiss()
    }

    /**
     * Checks if an avatar file already exist for the current account.
     *
     * @param retry True if should request for avatar if it's not available, false otherwise.
     */
    private fun setupAvatar(retry: Boolean) {
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
    private fun setProfileAvatar(avatar: File, retry: Boolean) {
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
                AVATAR_SIZE,
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
                    findNavController().navigate(R.id.action_my_account_to_achievements)
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

    private fun setupContactConnections() {
        binding.contactsLayout.setOnClickListener {
            startActivity(ContactsActivity.getListIntent(requireContext()))
        }

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

    private fun setupAccountDetails() {
        binding.lastSessionSubtitle.text =
            if (viewModel.getLastSession().isNotEmpty()) viewModel.getLastSession()
            else gettingInfo

        usageBinding.root.setOnClickListener {
            findNavController().navigate(
                R.id.action_my_account_to_my_account_usage,
                null,
                null,
                FragmentNavigatorExtras(usageBinding.root to usageBinding.root.transitionName)
            )
        }

        if (megaApi.isBusinessAccount) {
            setupBusinessAccount()
            return
        }

        binding.upgradeButton.apply {
            isEnabled = true
            text = StringResourcesUtils.getString(R.string.my_account_upgrade_pro)

            setOnClickListener {
                findNavController().navigate(R.id.action_my_account_to_upgrade)
                viewModel.setOpenUpgradeFrom()
            }
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = !viewModel.isProFlexiAccount()

        binding.accountTypeIcon.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                when (viewModel.getAccountType()) {
                    FREE -> R.drawable.ic_free_account
                    PRO_I -> R.drawable.ic_pro_i_account
                    PRO_II -> R.drawable.ic_pro_ii_account
                    PRO_III -> R.drawable.ic_pro_iii_account
                    PRO_LITE -> R.drawable.ic_lite_account
                    PRO_FLEXI -> R.drawable.ic_pro_flexi_account
                    else -> R.drawable.ic_free_account
                }
            )
        )

        binding.accountTypeText.text = StringResourcesUtils.getString(
            when (viewModel.getAccountType()) {
                FREE -> R.string.free_account
                PRO_I -> R.string.pro1_account
                PRO_II -> R.string.pro2_account
                PRO_III -> R.string.pro3_account
                PRO_LITE -> R.string.prolite_account
                PRO_FLEXI -> R.string.pro_flexi_account
                else -> R.string.recovering_info
            }
        )

        binding.accountTypeLayout.background = ColorUtils.tintIcon(
            requireContext(), R.drawable.background_account_type, ContextCompat.getColor(
                requireContext(),
                when (viewModel.getAccountType()) {
                    FREE -> R.color.green_400_green_300
                    PRO_LITE -> R.color.orange_600_orange_300
                    PRO_I, PRO_II, PRO_III, PRO_FLEXI -> R.color.red_300_red_200
                    else -> R.color.white_black
                }
            )
        )

        setupPaymentDetails()

        binding.businessAccountManagementText.isVisible = false

        if (viewModel.isProFlexiAccount()) {
            usageBinding.updateBusinessOrProFlexi(viewModel)
        } else {
            usageBinding.update(viewModel)
        }
    }

    private fun setupBusinessAccount() {
        binding.accountTypeIcon.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_business_account
            )
        )

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
                    paymentAlertBinding.businessUpdate(
                        megaApi,
                        viewModel,
                        false,
                        ActiveFragment.MY_ACCOUNT
                    )
                    expandPaymentInfoIfNeeded()
                }
                else -> setupPaymentDetails() //BUSINESS_STATUS_ACTIVE
            }

            binding.businessAccountManagementText.isVisible = true
        } else {
            binding.businessAccountManagementText.isVisible = false
        }

        usageBinding.updateBusinessOrProFlexi(viewModel)

        binding.achievementsLayout.isVisible = false
    }

    private fun setupPaymentDetails() {
        if (paymentAlertBinding.update(viewModel, ActiveFragment.MY_ACCOUNT)) {
            expandPaymentInfoIfNeeded()
        }
    }

    /**
     * Shows the payment info if the subscriptions is almost to renew or expiry.
     */
    private fun expandPaymentInfoIfNeeded() {
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
                    findNavController().navigate(R.id.action_my_account_to_add_phone_number)
                } else if (!phoneNumberBottomSheet.isBottomSheetDialogShown()) {
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
                    viewModel.getBonusStorageSMS()
                ) else StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_non_achievement_user
                )
        }
    }

    interface MessageResultCallback {
        fun show(message: String)
    }
}