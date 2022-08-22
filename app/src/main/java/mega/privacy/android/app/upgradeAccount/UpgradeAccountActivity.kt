package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spanned
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityChooseUpgradeAccountBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.upgradeAccount.PaymentActivity.Companion.UPGRADE_TYPE
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.Constants.UPDATE_GET_PRICING
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.util.Locale

class UpgradeAccountActivity : PasscodeActivity(), Scrollable {

    companion object {
        const val SUBSCRIPTION_FROM_ITUNES = 10
        const val SUBSCRIPTION_FROM_ANDROID_PLATFORM = 11
        const val SUBSCRIPTION_FROM_OTHER_PLATFORM = 12
        private const val BILLING_WARNING_SHOWN = "BILLING_WARNING_SHOWN"
    }

    private lateinit var binding: ActivityChooseUpgradeAccountBinding
    private val viewModel by viewModels<ChooseUpgradeAccountViewModel>()
    private var subscriptionWarningDialog: VerticalLayoutButtonDialog? = null

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            manageUpdateReceiver(intent.getIntExtra(BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION))
        }
    }

    private fun manageUpdateReceiver(action: Int) {
        if (isFinishing) {
            return
        }

        when (action) {
            UPDATE_ACCOUNT_DETAILS -> showAvailableAccount()
            UPDATE_GET_PRICING -> setPricingInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseUpgradeAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.refreshAccountInfo()
        setupView()
        setupObservers()
        initPayments()

        if (savedInstanceState != null
            && savedInstanceState.getBoolean(BILLING_WARNING_SHOWN, false)
        ) {
            showBillingWarning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionWarningDialog?.dismiss()
        unregisterReceiver(updateMyAccountReceiver)
        destroyPayments()
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BILLING_WARNING_SHOWN, binding.billingWarningLayout.isVisible)
        super.onSaveInstanceState(outState)
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = StringResourcesUtils.getString(R.string.action_upgrade_account)
        }

        binding.billingWarningClose.setOnClickListener {
            binding.billingWarningLayout.isVisible = false
            checkScroll()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        binding.upgradeProliteLayout.setOnClickListener { onUpgradeClick(PRO_LITE) }
        binding.upgradeProILayout.setOnClickListener { onUpgradeClick(PRO_I) }
        binding.upgradeProIiLayout.setOnClickListener { onUpgradeClick(PRO_II) }
        binding.upgradeProIiiLayout.setOnClickListener { onUpgradeClick(PRO_III) }

        binding.titleMyAccountType.isVisible = true
        binding.chooseAccountFreeLayout.isVisible = false

        binding.lblCustomPlan.apply {
            isVisible = false

            var textToShow = StringResourcesUtils.getString(R.string.label_custom_plan)
            val strColor =
                Util.getHexValue(
                    ColorUtils.getThemeColor(
                        this@UpgradeAccountActivity,
                        R.attr.colorSecondary
                    )
                )

            textToShow = textToShow.replace("[A]", "<b><font color='$strColor'>")
                .replace("[/A]", "</font></b>")

            text = textToShow.toSpannedHtmlText()

            setOnClickListener {
                AlertsAndWarnings.askForCustomizedPlan(
                    this@UpgradeAccountActivity,
                    megaApi.myEmail,
                    viewModel.getAccountType()
                )
            }
        }

        refreshAccountInfo()
        checkScroll()
        setAccountDetails()
        showAvailableAccount()
    }

    /**
     * Shows a warning when the billing is not available.
     */
    fun showBillingWarning() {
        binding.billingWarningLayout.isVisible = true
        checkScroll()
    }

    private fun setupObservers() {
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
        viewModel.onUpgradeClick().observe(this) { upgradeType ->
            startActivity(
                Intent(this, PaymentActivity::class.java).putExtra(UPGRADE_TYPE, upgradeType)
            )
        }
        viewModel.onUpgradeClickWithSubscription().observe(this) { warning ->
            if (warning == null) return@observe

            showSubscriptionDialog(warning.first, warning.second)
        }
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
                || binding.billingWarningLayout.isVisible
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        ColorUtils.changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.apply {
            setBackgroundColor(
                if (Util.isDarkMode(this@UpgradeAccountActivity) && withElevation) ColorUtils.getColorForElevation(
                    this@UpgradeAccountActivity,
                    elevation
                )
                else ContextCompat.getColor(this@UpgradeAccountActivity, R.color.white_dark_grey)
            )

            setElevation(if (withElevation) elevation else 0f)
        }
    }

    private fun showAvailableAccount() {
        when (viewModel.getAccountType()) {
            PRO_I -> binding.upgradeProILayoutTransparent.isVisible = true
            PRO_II -> binding.upgradeProIiLayoutTransparent.isVisible = true
            PRO_III -> {
                binding.lblCustomPlan.isVisible = true
                binding.upgradeProIiiLayoutTransparent.isVisible = true
            }
            PRO_LITE -> binding.upgradeProliteLayoutTransparent.isVisible = true
        }
    }

    private fun setPricingInfo() {
        val productAccounts = viewModel.getProductAccounts()

        if (productAccounts == null) {
            Timber.d("productAccounts == null")
            viewModel.refreshPricing()
            return
        }

        setFreePlan()

        for (i in productAccounts.indices) {
            val account = productAccounts[i]

            if (account.months == 1) {
                val textToShow: Spanned = viewModel.getPriceString(this, account, true)
                val textStorage: Spanned = viewModel.generateByteString(
                    this,
                    account.storage.toLong(),
                    ChooseUpgradeAccountViewModel.TYPE_STORAGE_LABEL
                )

                val textTransfer: Spanned = viewModel.generateByteString(
                    this,
                    account.transfer.toLong(),
                    ChooseUpgradeAccountViewModel.TYPE_TRANSFER_LABEL
                )

                when (account.level) {
                    PRO_I -> {
                        binding.monthProI.text = textToShow
                        binding.storageProI.text = textStorage
                        binding.bandwidthProI.text = textTransfer
                    }
                    PRO_II -> {
                        binding.monthProIi.text = textToShow
                        binding.storageProIi.text = textStorage
                        binding.bandwidthProIi.text = textTransfer
                    }
                    PRO_III -> {
                        binding.monthProIii.text = textToShow
                        binding.storageProIii.text = textStorage
                        binding.bandwidthProIii.text = textTransfer
                    }
                    PRO_LITE -> {
                        binding.monthLite.text = textToShow
                        binding.storageLite.text = textStorage
                        binding.bandwidthLite.text = textTransfer
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setFreePlan() {
        //Currently the API side doesn't return this value, so we have to hardcode.
        var textToShowFreeStorage =
            StringResourcesUtils.getString(R.string.account_upgrade_storage_label, "20 GB+")

        try {
            textToShowFreeStorage = textToShowFreeStorage.replace(
                "[A]", "<font color='"
                        + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                        + "'>"
            ).replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        binding.storageFree.text =
            "$textToShowFreeStorage<sup><small><font color='#ff333a'>1</font></small></sup>"
                .toSpannedHtmlText()

        var textToShowFreeBandwidth =
            StringResourcesUtils.getString(R.string.account_choose_free_limited_transfer_quota)

        try {
            textToShowFreeBandwidth = textToShowFreeBandwidth
                .replace(
                    "[A]", "<font color='"
                            + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                            + "'>"
                ).replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        binding.bandwidthFree.text = textToShowFreeBandwidth.toSpannedHtmlText()
        binding.achievementsFree.text = "<sup><small><font color='#ff333a'>1</font></small></sup> ${
            StringResourcesUtils.getString(
                R.string.footnote_achievements
            )
        }".toSpannedHtmlText()
    }

    private fun setAccountDetails() {
        if (viewModel.isGettingInfo()) {
            binding.textOfMyAccount.text = StringResourcesUtils.getString(R.string.recovering_info)
            binding.textOfMyAccount.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.grey_054_white_054
                )
            )

            return
        }

        val textValue: Int
        val colorValue: Int

        when (viewModel.getAccountType()) {
            PRO_I -> {
                textValue = R.string.pro1_account
                colorValue = R.color.red_600_red_300
            }
            PRO_II -> {
                textValue = R.string.pro2_account
                colorValue = R.color.red_600_red_300
            }
            PRO_III -> {
                textValue = R.string.pro3_account
                colorValue = R.color.red_600_red_300
            }
            PRO_LITE -> {
                textValue = R.string.prolite_account
                colorValue = R.color.lite_account
            }
            else -> {
                textValue = R.string.free_account
                colorValue = R.color.green_500_green_400
            }
        }

        var text = StringResourcesUtils.getString(
            R.string.type_of_my_account,
            StringResourcesUtils.getString(textValue)
        )

        val color = ContextCompat.getColor(this, colorValue).toString()


        try {
            text = text.replace("[A]", "<font color='$color'>")
            text = text.replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        binding.textOfMyAccount.text = text.toSpannedHtmlText()
    }

    /**
     * Shows the selected payment plan.
     *
     * @param upgradeType Selected payment plan.
     */
     private fun onUpgradeClick(upgradeType: Int) {
        with(viewModel) {
            if (!isBillingAvailable()) {
                Timber.w("Billing not available")
                showBillingWarning()
                return
            }

            if (getPaymentBitSet() == null) {
                Timber.w("PaymentBitSet Null")
                return
            }

            refreshAccountInfo()
            subscriptionCheck(upgradeType)
        }
    }

    /**
     * Show the existing subscription dialog
     * @param upgradeType upgrade type
     * @param subscriptionMethod SubscriptionMethod
     */
    private fun showSubscriptionDialog(upgradeType: Int, subscriptionMethod: SubscriptionMethod?) {
        subscriptionMethod?.run {
            subscriptionWarningDialog = VerticalLayoutButtonDialog(
                context = this@UpgradeAccountActivity,
                title = StringResourcesUtils.getString(R.string.title_existing_subscription),
                message = when (platformType) {
                    SUBSCRIPTION_FROM_ANDROID_PLATFORM -> {
                        StringResourcesUtils.getString(
                            R.string.message_subscription_from_android_platform,
                            methodName
                        )
                    }
                    SUBSCRIPTION_FROM_ITUNES -> {
                        StringResourcesUtils.getString(R.string.message_subscription_from_itunes)
                    }
                    else -> {
                        StringResourcesUtils.getString(R.string.message_subscription_from_other_platform)
                    }
                },
                positiveButtonTitle = StringResourcesUtils.getString(R.string.button_buy_new_subscription),
                onPositiveButtonClicked = {
                    startActivity(
                        Intent(this@UpgradeAccountActivity, PaymentActivity::class.java)
                            .putExtra(UPGRADE_TYPE, upgradeType)
                    )
                    viewModel.dismissSubscriptionWarningClicked()
                    it.dismiss()
                },
                onDismissClicked = {
                    viewModel.dismissSubscriptionWarningClicked()
                    it.dismiss()
                }
            ).apply { show() }
        }
    }
}