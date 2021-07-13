package mega.privacy.android.app.myAccount

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.dialog_general_confirmation.*
import mega.privacy.android.app.R
import mega.privacy.android.app.SMSVerificationActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_EMAIL_UPDATED
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_NAME_UPDATED
import mega.privacy.android.app.databinding.ActivityMyAccountBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.listeners.GetUserDataListener
import mega.privacy.android.app.listeners.ResetPhoneNumberListener
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.phoneNumber.PhoneNumberBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.phoneNumber.PhoneNumberCallback
import mega.privacy.android.app.myAccount.editProfile.EditProfileActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.AlertsAndWarnings.showRemoveOrModifyPhoneNumberConfirmDialog
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.ChangeApiServerUtil.showChangeApiServerDialog
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.TimeUtils.DATE_MM_DD_YYYY_FORMAT
import mega.privacy.android.app.utils.TimeUtils.formatDate
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.canVoluntaryVerifyPhoneNumber
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_EXPIRED
import nz.mega.sdk.MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUser
import org.jetbrains.anko.contentView
import java.io.File

class MyAccountActivity : PasscodeActivity(), Scrollable, PhoneNumberCallback {

    companion object {
        private const val KILL_SESSIONS_SHOWN = "KILL_SESSIONS_SHOWN"
        private const val CANCEL_SUBSCRIPTIONS_SHOWN = "CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val TYPED_FEEDBACK = "TYPED_FEEDBACK"
        private const val CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN = "CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN"
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_DELAY = 500L
        private const val PHONE_NUMBER_CHANGE_DELAY = 3000L
    }

    private val viewModel: MyAccountViewModel by viewModels()

    private lateinit var binding: ActivityMyAccountBinding

    private var menu: Menu? = null

    private var killSessionsConfirmationDialog: AlertDialog? = null
    private var cancelSubscriptionsDialog: AlertDialog? = null
    private var cancelSubscriptionsConfirmationDialog: AlertDialog? = null
    private var removeOrModifyPhoneNumberDialog: AlertDialog? = null
    private var changeApiServerDialog: AlertDialog? = null

    private var cancelSubscriptionsFeedback: String? = null

    private var phoneNumberBottomSheet: PhoneNumberBottomSheetDialogFragment? = null

    private var gettingInfo = StringResourcesUtils.getString(R.string.recovering_info)

    private var isModify = false

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actionType = intent.getIntExtra(
                BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION
            )

            when (actionType) {
                UPDATE_ACCOUNT_DETAILS -> setUpAccountDetails()
                UPDATE_CREDIT_CARD_SUBSCRIPTION -> refreshMenuOptionsVisibility()
            }
        }
    }

    private val showRemovePhoneNumberObserver = Observer<Boolean> { isModify ->
        showConfirmation(isModify)
    }

    private val profileAvatarUpdatedObserver = Observer<Boolean> { setUpAvatar(false) }

    private val nameUpdatedObserver =
        Observer<Boolean> { binding.nameText.text = viewModel.getName() }

    private val emailUpdatedObserver =
        Observer<Boolean> { binding.emailText.text = viewModel.getEmail() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateInfo()
        setupView()
        setupObservers()

        if (savedInstanceState != null) {
            when {
                savedInstanceState.getBoolean(KILL_SESSIONS_SHOWN, false) -> {
                    showConfirmationKillSessions()
                }
                savedInstanceState.getBoolean(CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    cancelSubscriptionsFeedback = savedInstanceState.getString(TYPED_FEEDBACK)
                    showCancelSubscriptions()
                }
                savedInstanceState.getBoolean(CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN, false) -> {
                    showConfirmationCancelSubscriptions()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KILL_SESSIONS_SHOWN, isAlertDialogShown(killSessionsConfirmationDialog))

        if (isAlertDialogShown(cancelSubscriptionsDialog)) {
            outState.putBoolean(CANCEL_SUBSCRIPTIONS_SHOWN, true)
            outState.putString(TYPED_FEEDBACK, cancelSubscriptionsFeedback)
        }

        outState.putBoolean(
            CONFIRM_CANCEL_SUBSCRIPTIONS_SHOWN,
            isAlertDialogShown(cancelSubscriptionsConfirmationDialog)
        )

        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val error = viewModel.manageActivityResult(this, requestCode, resultCode, data)
        if (!error.isNullOrEmpty()) showSnackbar(error)
    }

    override fun onResume() {
        super.onResume()
        app.refreshAccountInfo()
    }

    override fun onPostResume() {
        super.onPostResume()
        try {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(
                NOTIFICATION_STORAGE_OVERQUOTA
            )
        } catch (e: Exception) {
            LogUtil.logError("Exception NotificationManager - remove all notifications", e)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(updateMyAccountReceiver)

        LiveEventBus.get(EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION, Boolean::class.java)
            .removeObserver(showRemovePhoneNumberObserver)

        LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean::class.java)
            .removeObserver(profileAvatarUpdatedObserver)

        LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java)
            .removeObserver(nameUpdatedObserver)

        LiveEventBus.get(EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
            .removeObserver(emailUpdatedObserver)

        killSessionsConfirmationDialog?.dismiss()
        cancelSubscriptionsDialog?.dismiss()
        cancelSubscriptionsConfirmationDialog?.dismiss()
        removeOrModifyPhoneNumberDialog?.dismiss()
        changeApiServerDialog?.dismiss()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_kill_all_sessions -> showConfirmationKillSessions()
            R.id.action_change_pass -> viewModel.changePassword(this)
            R.id.action_export_MK -> viewModel.exportMK(this)
            R.id.action_refresh -> viewModel.refresh(this)
            R.id.action_upgrade_account -> viewModel.upgradeAccount(this)
            R.id.action_cancel_subscriptions -> showCancelSubscriptions()
            R.id.action_logout -> viewModel.logout(this)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_my_account, menu)
        this.menu = menu

        refreshMenuOptionsVisibility()

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        if (!isOnline(this)) {
            menu.toggleAllMenuItemsVisibility(false)
            return
        }

        menu.toggleAllMenuItemsVisibility(true)

        if (viewModel.thereIsNoSubscription()) {
            menu.findItem(R.id.action_cancel_subscriptions).isVisible = false
        }

        if (megaApi.isBusinessAccount) {
            menu.findItem(R.id.action_upgrade_account).isVisible = false
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)

        supportActionBar?.apply {
            title = null
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        ListenScrollChangesHelper().addViewToListen(
            binding.scrollView
        ) { _, _, _, _, _ -> checkScroll() }

        checkScroll()

        setUpAvatar(true)

        binding.myAccountThumbnail.setOnClickListener { viewModel.openQR(this) }

        binding.nameText.text = viewModel.getName()
        binding.emailText.text = viewModel.getEmail()

        setUpPhoneNumber()
        setUpAccountDetails()

        binding.backupRecoveryKeyLayout.setOnClickListener { viewModel.exportMK(this) }

        setUpAchievements()
        setUpLastSession()
        setUpContactConnections()
    }

    private fun updateInfo() {
        viewModel.checkVersions { refreshVersionsInfo() }
        app.refreshAccountInfo()
    }

    private fun setupObservers() {
        registerReceiver(
            updateMyAccountReceiver, IntentFilter(
                BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
            )
        )

        LiveEventBus.get(EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION, Boolean::class.java)
            .observeForever(showRemovePhoneNumberObserver)

        LiveEventBus.get(EVENT_AVATAR_CHANGE, Boolean::class.java)
            .observeForever(profileAvatarUpdatedObserver)

        LiveEventBus.get(EVENT_USER_NAME_UPDATED, Boolean::class.java)
            .observeForever(nameUpdatedObserver)

        LiveEventBus.get(EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
            .observeForever(emailUpdatedObserver)

        viewModel.onGetAvatarFinished().observe(this, ::setAvatar)
    }

    private fun showKillSessionsResult(success: Boolean) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (success) R.string.success_kill_all_sessions
                else R.string.error_kill_all_sessions
            )
        )
    }

    private fun showCancelSubscriptionsResult(success: Boolean) {
        showSnackbar(
            StringResourcesUtils.getString(
                if (success) R.string.cancel_subscription_ok
                else R.string.cancel_subscription_error
            )
        )

        app.askForCCSubscriptions()
    }

    private fun showConfirmationKillSessions() {
        if (isAlertDialogShown(killSessionsConfirmationDialog)) {
            return
        }

        killSessionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.confirmation_close_sessions_title))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_close_sessions_text))
            .setPositiveButton(StringResourcesUtils.getString(R.string.contact_accept)) { _, _ ->
                viewModel.killSessions { success -> showKillSessionsResult(success) }
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            .show()
    }

    private fun showCancelSubscriptions() {
        if (isAlertDialogShown(cancelSubscriptionsDialog)) {
            return
        }

        val builder = MaterialAlertDialogBuilder(this)

        cancelSubscriptionsDialog =
            builder.setView(R.layout.dialog_cancel_subscriptions)
                .setPositiveButton(
                    StringResourcesUtils.getString(R.string.send_cancel_subscriptions),
                    null
                )
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss), null)
                .create()

        cancelSubscriptionsDialog?.apply {
            setOnShowListener {
                val feedbackEditText = findViewById<EditText>(R.id.dialog_cancel_feedback)
                feedbackEditText?.apply {
                    setText(cancelSubscriptionsFeedback)

                    doAfterTextChanged {
                        cancelSubscriptionsFeedback = text.toString()
                    }
                }

                this.positive_button.setOnClickListener {
                    if (cancelSubscriptionsFeedback?.isEmpty() == true) {
                        showSnackbar(StringResourcesUtils.getString(R.string.reason_cancel_subscriptions))
                    } else {
                        showConfirmationCancelSubscriptions()
                    }
                }
            }

            show()
        }
    }

    private fun showConfirmationCancelSubscriptions() {
        if (isAlertDialogShown(cancelSubscriptionsConfirmationDialog)) {
            return
        }

        cancelSubscriptionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_cancel_subscriptions))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_yes)) { _, _ ->
                viewModel.cancelSubscriptions(cancelSubscriptionsFeedback) { success ->
                    showCancelSubscriptionsResult(success)
                }
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_no), null)
            .show()
    }

    private fun setupEditProfile(editable: Boolean) {
        binding.nameText.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (editable) R.drawable.ic_view_edit_profile else 0,
            0
        )

        binding.myAccountTextInfoLayout.setOnClickListener {
            if (editable) {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
        }
    }

    fun setUpAccountDetails() {
        binding.lastSessionSubtitle.text =
            if (viewModel.getLastSession().isNotEmpty()) viewModel.getLastSession()
            else gettingInfo

        if (megaApi.isBusinessAccount) {
            setUpBusinessAccount()
            return
        }

        setupEditProfile(true)

        binding.upgradeButton.apply {
            isEnabled = true
            text = StringResourcesUtils.getString(R.string.my_account_upgrade_pro)

            setOnClickListener { viewModel.upgradeAccount(this@MyAccountActivity) }
        }

        binding.accountTypeText.isVisible = true
        binding.upgradeButton.isVisible = true

        binding.accountTypeText.text = StringResourcesUtils.getString(
            when (viewModel.getAccountType()) {
                FREE -> R.string.free_account
                PRO_I -> R.string.pro1_account
                PRO_II -> R.string.pro2_account
                PRO_III -> R.string.pro3_account
                PRO_LITE -> R.string.prolite_account
                else -> R.string.recovering_info
            }
        )

        binding.accountTypeLayout.background = tintIcon(
            this, R.drawable.background_account_type, ContextCompat.getColor(
                this,
                when (viewModel.getAccountType()) {
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

        binding.transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedStorage().isEmpty()) {
            binding.storageProgressPercentage.isVisible = false
            binding.storageProgressBar.progress = 0
            binding.storageProgress.text = gettingInfo
        } else {
            binding.storageProgressPercentage.isVisible = true
            binding.storageProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedStoragePercentage()
            )

            binding.storageProgressBar.progress = viewModel.getUsedStoragePercentage()
            binding.storageProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )
        }

        if (viewModel.getUsedTransfer().isEmpty()) {
            binding.transferProgressPercentage.isVisible = false
            binding.transferProgressBar.progress = 0
            binding.transferProgress.text = gettingInfo
        } else {
            binding.transferProgressPercentage.isVisible = true
            binding.transferProgressPercentage.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedTransferPercentage()
            )

            binding.transferProgressBar.progress = viewModel.getUsedTransferPercentage()
            binding.transferProgress.text = StringResourcesUtils.getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
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
            this, R.drawable.background_account_type, ContextCompat.getColor(
                this,
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

                    expandPaymentInfoIfNeeded()
                }
                else -> setUpPaymentDetails() //BUSINESS_STATUS_ACTIVE
            }

            binding.businessAccountManagementText.isVisible = true
            setupEditProfile(true)
        } else {
            binding.businessAccountManagementText.isVisible = false
            setupEditProfile(false)
        }

        binding.storageProgressPercentage.isVisible = false
        binding.storageProgressBar.isVisible = false
        binding.businessStorageImage.isVisible = true

        binding.storageProgress.text =
            if (viewModel.getUsedStorage().isEmpty()) gettingInfo
            else viewModel.getUsedStorage()

        binding.transferProgressPercentage.isVisible = false
        binding.transferProgressBar.isVisible = false
        binding.businessTransferImage.isVisible = true

        binding.transferProgress.text =
            if (viewModel.getUsedTransfer().isEmpty()) gettingInfo
            else viewModel.getUsedTransfer()

        binding.achievementsLayout.isVisible = false
    }

    private fun setUpPaymentDetails() {
        binding.businessStatusText.isVisible = false

        if (viewModel.hasRenewableSubscription() || viewModel.hasExpirableSubscription()) {
            binding.renewExpiryText.isVisible = true
            binding.renewExpiryDateText.isVisible = true

            binding.renewExpiryText.text = StringResourcesUtils.getString(
                if (viewModel.hasRenewableSubscription()) R.string.renews_on else R.string.expires_on
            )

            binding.renewExpiryDateText.text = formatDate(
                if (viewModel.hasRenewableSubscription()) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                DATE_MM_DD_YYYY_FORMAT
            )

            expandPaymentInfoIfNeeded()
        }
    }

    fun expandPaymentInfoIfNeeded() {
        if (!viewModel.shouldShowPaymentInfo())
            return

        val v = binding.paymentInfoLayout
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

    private fun setUpPhoneNumber() {
        val canVerifyPhoneNumber = canVoluntaryVerifyPhoneNumber()
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
                if (canVoluntaryVerifyPhoneNumber()) {
                    startActivity(
                        Intent(
                            this@MyAccountActivity,
                            SMSVerificationActivity::class.java
                        )
                    )
                } else if (!isBottomSheetDialogShown(phoneNumberBottomSheet)) {
                    phoneNumberBottomSheet = PhoneNumberBottomSheetDialogFragment()
                    phoneNumberBottomSheet!!.show(
                        supportFragmentManager,
                        phoneNumberBottomSheet!!.tag
                    )
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

    private fun setUpAchievements() {
        binding.achievementsLayout.apply {
            isVisible = megaApi.isAchievementsEnabled

            if (isVisible) {
                setOnClickListener {
                    if (!isOnline(this@MyAccountActivity)) {
                        showSnackbar(StringResourcesUtils.getString(R.string.error_server_connection_problem))
                    } else {
                        startActivity(
                            Intent(
                                this@MyAccountActivity,
                                AchievementsActivity::class.java
                            )
                        )
                    }
                }
            }
        }
    }

    private fun setUpLastSession() {
        binding.lastSessionLayout.setOnClickListener {
            if (viewModel.incrementLastSessionClick()) {
                changeApiServerDialog = showChangeApiServerDialog(this, megaApi)
            }
        }
    }

    fun resetPass() {
        AccountController(this).resetPass(megaApi.myEmail)
    }

    fun updateNameView(fullName: String) {
        binding.nameText.text = fullName
    }

    fun setUpAvatar(retry: Boolean) {
        val avatar = buildAvatarFile(this, megaApi.myEmail + JPG_EXTENSION)

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

        if (retry) viewModel.getAvatar(this) else setDefaultAvatar()
    }

    private fun setDefaultAvatar() {
        binding.myAccountThumbnail.setImageBitmap(
            getDefaultAvatar(
                getColorAvatar(megaApi.myUser),
                viewModel.getName(),
                AVATAR_SIZE,
                true
            )
        )
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
            R.plurals.my_account_connections,
            visibleContacts.size,
            visibleContacts.size
        )
    }

    fun updateMailView(email: String) {
        binding.emailText.text = email

        if (!isFileAvailable(buildAvatarFile(this, email + JPG_EXTENSION))) {
            setDefaultAvatar()
        }
    }

    fun updateAddPhoneNumberLabel() {
        runDelay(PHONE_NUMBER_CHANGE_DELAY) {
            //work around - it takes time for megaApi.smsVerifiedPhoneNumber() to return value
            val registeredPhoneNumber = megaApi.smsVerifiedPhoneNumber()
            LogUtil.logDebug("updateAddPhoneNumberLabel $registeredPhoneNumber")

            binding.phoneText.apply {
                if (!isTextEmpty(registeredPhoneNumber)) {
                    isVisible = true
                    text = registeredPhoneNumber
                } else {
                    isVisible = false
                }
            }
        }
    }

    override fun showConfirmation(isModify: Boolean) {
        this.isModify = isModify
        removeOrModifyPhoneNumberDialog =
            showRemoveOrModifyPhoneNumberConfirmDialog(this, isModify, this)
    }

    override fun reset() {
        megaApi.resetSmsVerifiedPhoneNumber(ResetPhoneNumberListener(this, this))
    }

    override fun onReset(error: MegaError) {        /*
          Reset phone number successfully or the account has reset the phone number,
          but user data hasn't refreshed successfully need to refresh user data again.
        */
        if (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT) {
            // Have to getUserData to refresh, otherwise, phone number remains previous value.
            megaApi.getUserData(GetUserDataListener(this, this))
        } else {
            binding.phoneText.isClickable = true
            showSnackbar(StringResourcesUtils.getString(R.string.remove_phone_number_fail))
            logWarning("Reset phone number failed: " + getTranslatedErrorString(error))
        }
    }

    override fun onUserDataUpdate(error: MegaError) {
        binding.phoneText.isClickable = true

        if (error.errorCode == API_OK) {
            if (canVoluntaryVerifyPhoneNumber()) {
                binding.phoneText.text =
                    StringResourcesUtils.getString(R.string.add_phone_number_label)
                binding.phoneText.isVisible = true

//                if (this is ManagerActivityLollipop) {
//                    (this as ManagerActivityLollipop).showAddPhoneNumberInMenu();
//                }

                if (isModify) {
                    startActivity(Intent(this, SMSVerificationActivity::class.java))
                } else {
                    showSnackbar(StringResourcesUtils.getString(R.string.remove_phone_number_success))
                }
            }
        } else {
            showSnackbar(StringResourcesUtils.getString(R.string.remove_phone_number_fail))
            logWarning(
                "Get user data for updating phone number failed: " + getTranslatedErrorString(
                    error
                )
            )
        }
    }

    private fun refreshVersionsInfo() {

    }

    private fun setAvatar(error: MegaError) {
        if (error.errorCode == API_ENOENT) {
            setDefaultAvatar()
        } else {
            setUpAvatar(false)
        }
    }

    fun showSnackbar(text: String) {
        showSnackbar(contentView?.findViewById(R.id.container), text)
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        val isDark = Util.isDarkMode(this)
        val darkAndElevation = withElevation && isDark
        val background = ContextCompat.getColor(this, R.color.grey_020_grey_087)

        if (darkAndElevation) {
            ColorUtils.changeStatusBarColorForElevation(this, true)
        } else {
            window.statusBarColor = background
        }

        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.toolbar.setBackgroundColor(
            if (darkAndElevation) ColorUtils.getColorForElevation(
                this,
                elevation
            ) else background
        )

        supportActionBar?.elevation =
            if (withElevation && !isDark) elevation else 0F
    }
}