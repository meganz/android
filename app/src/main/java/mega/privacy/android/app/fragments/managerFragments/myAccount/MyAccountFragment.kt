package mega.privacy.android.app.fragments.managerFragments.myAccount

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.MyAccountInfo
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.canVoluntaryVerifyPhoneNumber
import nz.mega.sdk.MegaUser
import java.io.File

class MyAccountFragment : BaseFragment(), Scrollable {

    private lateinit var binding: FragmentMyAccountBinding
    private var accountInfo: MyAccountInfo? = null

    companion object {
        @JvmStatic
        fun newInstance(): MyAccountFragment {
            return MyAccountFragment()
        }
    }

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
        setAccountDetails()

        binding.myAccountTextInfoLayout.setOnClickListener {
            //Open edit my profile activity
        }

        updateAvatar(true)

        if (!isTextEmpty(accountInfo?.fullName)) {
            binding.nameText.text = accountInfo?.fullName
        }

        binding.emailText.text = megaApi.myEmail

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
                //Open add phone number activity
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

        binding.backupRecoveryKeyLayout.setOnClickListener {
            //Open backup recovery key activity
        }

        binding.achievementsLayout.setOnClickListener {
            //Open achievements activity
        }

        updateContactsCount()
    }

    override fun checkScroll() {
        (requireActivity() as ManagerActivityLollipop).changeAppBarElevation(
            binding.scrollView.canScrollVertically(
                -1
            )
        )
    }

    fun setAccountDetails() {
        binding.lastSessionSubtitle.text = if (isTextEmpty(accountInfo?.lastSessionFormattedDate)) {
            StringResourcesUtils.getString(R.string.recovering_info)
        } else accountInfo?.lastSessionFormattedDate

        if (megaApi.isBusinessAccount) {

            return
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = true

        binding.achievementsLayout.isVisible = megaApi.isAchievementsEnabled

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
                    else -> R.color.blue_400_blue_300
                }
            )
        )

        if (accountInfo?.accountType == FREE) {
            binding.renewExpiryText.isVisible = false
            binding.renewExpiryDateText.isVisible = false
        } else {
            binding.renewExpiryText.isVisible = true
            binding.renewExpiryDateText.isVisible = true
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

    fun updateAvatar(retry: Boolean) {
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

    fun updateContactsCount() {
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