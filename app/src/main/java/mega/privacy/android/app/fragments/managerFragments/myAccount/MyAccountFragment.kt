package mega.privacy.android.app.fragments.managerFragments.myAccount

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.SMSVerificationActivity
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.MyAccountInfo
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil
import mega.privacy.android.app.modalbottomsheet.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.TimeUtils.formatDate
import mega.privacy.android.app.utils.Util.canVoluntaryVerifyPhoneNumber
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaUser
import java.io.File

class MyAccountFragment : BaseFragment(), Scrollable {

    companion object {
        private const val CLICKS_TO_STAGING = 5
        private const val STAGING_URL = "https://staging.api.mega.co.nz/"
        private const val PRODUCTION_URL = "https://g.api.mega.co.nz/"

        @JvmStatic
        fun newInstance(): MyAccountFragment {
            return MyAccountFragment()
        }
    }

    private lateinit var binding: FragmentMyAccountBinding
    private var accountInfo: MyAccountInfo? = null

    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null
    private var numOfClicksLastSession = 0

    private var staging = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAccountBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
    }

    private fun setUpView() {
        accountInfo = app.myAccountInfo

        ListenScrollChangesHelper().addViewToListen(
            binding.scrollView
        ) { _, _, _, _, _ -> checkScroll() }

        setUpAvatar(true)

        binding.myAccountThumbnail.setOnClickListener {
            (requireContext() as ManagerActivityLollipop).checkBeforeOpeningQR(false)
        }

        if (!isTextEmpty(accountInfo?.fullName)) {
            binding.nameText.text = accountInfo?.fullName
        }

        binding.emailText.text = megaApi.myEmail

        setUpPhoneNumber()
        setUpAccountDetails()

        binding.backupRecoveryKeyLayout.setOnClickListener {
            (requireContext() as ManagerActivityLollipop).showMKLayout()
        }

        setUpAchievements()
        setUpLastSession()
        setUpContactConnections()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        (requireContext() as ManagerActivityLollipop).changeMyAccountAppBarElevation(
            binding.scrollView.canScrollVertically(
                SCROLLING_UP_DIRECTION
            )
        )
    }

    private fun setupEditProfile(editable: Boolean) {
        binding.viewAndEditProfileIcon.isVisible = editable

        binding.myAccountTextInfoLayout.setOnClickListener {
            if (editable) {
                //Open edit my profile activity
            }
        }
    }

    fun setUpAccountDetails() {
        val gettingInfo = StringResourcesUtils.getString(R.string.recovering_info)

        binding.lastSessionSubtitle.text = if (isTextEmpty(accountInfo?.lastSessionFormattedDate)) {
            gettingInfo
        } else accountInfo?.lastSessionFormattedDate

        if (megaApi.isBusinessAccount) {
            setUpBusinessAccount()
            return
        }

        setupEditProfile(true)

        binding.upgradeButton.apply {
            isEnabled = true
            text = StringResourcesUtils.getString(R.string.my_account_upgrade_pro)

            setOnClickListener { (requireActivity() as ManagerActivityLollipop).showUpAF() }
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = true

        binding.accountTypeText.text = StringResourcesUtils.getString(
            when (accountInfo?.accountType) {
                FREE -> R.string.free_account
                PRO_I -> R.string.pro1_account
                PRO_II -> R.string.pro2_account
                PRO_III -> R.string.pro3_account
                PRO_LITE -> R.string.prolite_account
                else -> R.string.recovering_info
            }
        )

        binding.accountTypeLayout.background = tintIcon(
            requireContext(), R.drawable.background_account_type, ContextCompat.getColor(
                requireContext(),
                when (accountInfo?.accountType) {
                    FREE -> R.color.green_400_green_300
                    PRO_I -> R.color.orange_600_orange_300
                    PRO_II, PRO_III, PRO_LITE -> R.color.red_300_red_200
                    else -> R.color.white_black
                }
            )
        )

        setUpPaymentDetails()

        binding.storageProgressPercentage.isVisible = true
        binding.storageProgressBar.isVisible = true
        binding.businessStorageImage.isVisible = false
        binding.transferProgressPercentage.isVisible = true
        binding.transferProgressBar.isVisible = true
        binding.businessTransferImage.isVisible = false

        binding.businessAccountManagementText.isVisible = false

        binding.transferLayout.isVisible = accountInfo?.accountType != FREE

        if (accountInfo?.usedFormatted?.trim()?.isEmpty() == true) {
            binding.storageProgressPercentage.isVisible = false
            binding.storageProgressBar.progress = 0
            binding.storageProgress.text = gettingInfo
        } else {
            binding.storageProgressPercentage.isVisible = true
            binding.storageProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                accountInfo?.usedPerc?.toString()
            )

            binding.storageProgressBar.progress = accountInfo?.usedPerc ?: 0
            binding.storageProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                accountInfo?.usedFormatted,
                accountInfo?.totalFormatted
            )
        }

        if (accountInfo?.usedTransferFormatted?.trim()?.isEmpty() == true) {
            binding.transferProgressPercentage.isVisible = false
            binding.transferProgressBar.progress = 0
            binding.transferProgress.text = gettingInfo
        } else {
            binding.transferProgressPercentage.isVisible = true
            binding.transferProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                accountInfo?.usedTransferPerc?.toString()
            )

            binding.transferProgressBar.progress = accountInfo?.usedPerc ?: 0
            binding.transferProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                accountInfo?.usedTransferFormatted,
                accountInfo?.totalTansferFormatted
            )
        }
    }

    private fun setUpBusinessAccount() {
        binding.accountTypeText.text = StringResourcesUtils.getString(R.string.business_label)
        binding.upgradeButton.apply {
            isEnabled = false
            text = StringResourcesUtils.getString(
                if (megaApi.isMasterBusinessAccount) R.string.admin_label
                else R.string.user_label
            )
        }

        binding.accountTypeLayout.background = tintIcon(
            requireContext(), R.drawable.background_account_type, ContextCompat.getColor(
                requireContext(),
                R.color.blue_400_blue_300
            )
        )

        if (megaApi.isMasterBusinessAccount) {
            when (megaApi.businessStatus) {
                BUSINESS_STATUS_EXPIRED, BUSINESS_STATUS_GRACE_PERIOD -> {
                    binding.renewExpiryText.isVisible = false
                    binding.renewExpiryDateText.isVisible = false
                    binding.businessStatusText.apply {
                        isVisible = true
                        text = StringResourcesUtils.getString(
                            if (megaApi.businessStatus == BUSINESS_STATUS_EXPIRED) R.string.payment_overdue_label
                            else R.string.payment_required_label
                        )
                    }
                }
                else -> setUpPaymentDetails() //BUSINESS_STATUS_ACTIVE
            }

            binding.businessAccountManagementText.isVisible = true
            setupEditProfile(true)
        } else {
            binding.renewExpiryText.isVisible = false
            binding.renewExpiryDateText.isVisible = false
            binding.businessStatusText.isVisible = false
            binding.businessAccountManagementText.isVisible = false
            setupEditProfile(false)
        }

        binding.storageProgressPercentage.isVisible = false
        binding.storageProgressBar.isVisible = false
        binding.businessStorageImage.isVisible = true

        val gettingInfo = StringResourcesUtils.getString(R.string.recovering_info)

        binding.storageProgress.text = if (accountInfo?.usedFormatted?.trim()?.isEmpty() == true) {
            gettingInfo
        } else {
            accountInfo?.usedFormatted
        }

        binding.transferProgressPercentage.isVisible = false
        binding.transferProgressBar.isVisible = false
        binding.businessTransferImage.isVisible = true

        binding.transferProgress.text =
            if (accountInfo?.usedTransferFormatted?.trim()?.isEmpty() == true) {
                gettingInfo
            } else {
                accountInfo?.usedTransferFormatted
            }

        binding.achievementsLayout.isVisible = false
    }

    private fun setUpPaymentDetails() {
        binding.businessStatusText.isVisible = false

        val hasRenewableSubscription =
            accountInfo?.subscriptionStatus == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
                    && accountInfo?.subscriptionRenewTime!! > 0

        if (hasRenewableSubscription || accountInfo?.proExpirationTime!! > 0) {
            binding.renewExpiryText.isVisible = true
            binding.renewExpiryDateText.isVisible = true

            binding.renewExpiryText.text = StringResourcesUtils.getString(
                if (hasRenewableSubscription) R.string.renews_on else R.string.expires_on
            )

            binding.renewExpiryDateText.text = formatDate(
                if (hasRenewableSubscription) accountInfo?.subscriptionRenewTime!! else accountInfo?.proExpirationTime!!,
                TimeUtils.DATE_MM_DD_YYYY_FORMAT
            )
        } else {
            binding.renewExpiryText.isVisible = false
            binding.renewExpiryDateText.isVisible = false
        }
    }

    private fun setUpPhoneNumber() {
        val registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber()
        val alreadyRegisteredPhoneNumber = !isTextEmpty(registeredPhoneNumber)
        val canVerifyPhoneNumber = canVoluntaryVerifyPhoneNumber()

        binding.phoneText.apply {
            if (alreadyRegisteredPhoneNumber) {
                isVisible = true
                text = registeredPhoneNumber
            } else {
                isVisible = false
            }
        }

        binding.addPhoneNumberLayout.apply {
            isVisible = canVerifyPhoneNumber && !alreadyRegisteredPhoneNumber

            setOnClickListener {
                if (canVoluntaryVerifyPhoneNumber()) {
                    startActivity(Intent(context, SMSVerificationActivity::class.java))
                } else if (!ModalBottomSheetUtil.isBottomSheetDialogShown(phoneNumberBottomSheet) && activity != null) {
                    phoneNumberBottomSheet = PhoneNumberBottomSheetDialogFragment()
                    phoneNumberBottomSheet!!.show(
                        (requireContext() as ManagerActivityLollipop).supportFragmentManager,
                        phoneNumberBottomSheet!!.tag
                    )
                }
            }
        }

        if (canVerifyPhoneNumber) {
            binding.phoneText.text =
                if (megaApi.isAchievementsEnabled) StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_achievement_user,
                    (requireContext() as ManagerActivityLollipop).bonusStorageSMS
                ) else StringResourcesUtils.getString(
                    R.string.sms_add_phone_number_dialog_msg_non_achievement_user
                )
        }
    }

    private fun setUpAchievements() {
        binding.achievementsLayout.apply {
            isVisible = megaApi.isAchievementsEnabled

            if (isVisible) {
                setOnClickListener {
                    if (!isOnline(context)) {
                        (requireContext() as ManagerActivityLollipop).showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.error_server_connection_problem),
                            MEGACHAT_INVALID_HANDLE
                        )
                    } else {
                        startActivity(Intent(context, AchievementsActivity::class.java))
                    }
                }
            }
        }
    }

    private fun setUpLastSession() {
        binding.lastSessionLayout.setOnClickListener {
            numOfClicksLastSession++

            if (numOfClicksLastSession < CLICKS_TO_STAGING)
                return@setOnClickListener

            numOfClicksLastSession = 0
            staging = false

            if (dbH != null) {
                val attrs = dbH.attributes

                if (attrs != null && attrs.staging != null) {
                    staging = try {
                        java.lang.Boolean.parseBoolean(attrs.staging)
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            if (staging) {
                staging = false
                megaApi.changeApiUrl(PRODUCTION_URL)

                if (dbH != null) {
                    dbH.setStaging(false)
                }

                val intent = Intent(context, LoginActivityLollipop::class.java)
                intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT).action = ACTION_REFRESH_STAGING

                startActivityForResult(intent, REQUEST_CODE_REFRESH_STAGING)

                return@setOnClickListener
            }

            val builder = MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Mega_MaterialAlertDialog
            )

            builder.setTitle(StringResourcesUtils.getString(R.string.staging_api_url_title))
            builder.setMessage(StringResourcesUtils.getString(R.string.staging_api_url_text))
            builder.setPositiveButton(
                StringResourcesUtils.getString(R.string.general_yes)
            ) { _, _ ->
                staging = true
                megaApi.changeApiUrl(STAGING_URL)

                if (dbH != null) {
                    dbH.setStaging(true)
                }

                val intent = Intent(context, LoginActivityLollipop::class.java)
                intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                intent.action = ACTION_REFRESH_STAGING
                startActivityForResult(intent, REQUEST_CODE_REFRESH_STAGING)
            }

            builder.setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            builder.show()
        }
    }

    fun onBackPressed(): Int {
        return 0
    }

    fun resetPass() {
        AccountController(context).resetPass(megaApi.myEmail)
    }

    fun updateNameView(fullName: String) {
        binding.nameText.text = fullName
    }

    fun setUpAvatar(retry: Boolean) {
        val avatar = buildAvatarFile(requireActivity(), megaApi.myEmail + JPG_EXTENSION)

        if (avatar != null) {
            setProfileAvatar(avatar, retry)
        } else {
            setDefaultAvatar()
        }
    }

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
            megaApi.getUserAvatar(
                megaApi.myUser,
                buildAvatarFile(context, megaApi.myEmail).absolutePath,
                context as ManagerActivityLollipop
            )
        } else {
            setDefaultAvatar()
        }
    }

    private fun setDefaultAvatar() {
        binding.myAccountThumbnail.setImageBitmap(
            getDefaultAvatar(
                getColorAvatar(megaApi.myUser),
                accountInfo?.fullName,
                AVATAR_SIZE,
                true
            )
        )
    }

    fun refreshVersionsInfo() {

    }

    fun setUpContactConnections() {
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
            R.plurals.general_selection_num_contacts,
            visibleContacts.size,
            visibleContacts.size
        )
    }

    fun updateMailView(email: String) {
        binding.emailText.text = email

        if (!isFileAvailable(buildAvatarFile(context, email + JPG_EXTENSION))) {
            setDefaultAvatar()
        }
    }

    fun updateAddPhoneNumberLabel() {

    }
}