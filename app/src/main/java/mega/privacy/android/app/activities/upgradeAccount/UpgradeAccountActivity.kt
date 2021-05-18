package mega.privacy.android.app.activities.upgradeAccount

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_upgrade_account.*
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.upgradeAccount.UpgradeAccountViewModel.Companion.TYPE_STORAGE_LABEL
import mega.privacy.android.app.activities.upgradeAccount.UpgradeAccountViewModel.Companion.TYPE_TRANSFER_LABEL
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.ActivityUpgradeAccountBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.service.iab.BillingManagerImpl.*
import mega.privacy.android.app.utils.AlertsAndWarnings.askForCustomizedPlan
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET

open class UpgradeAccountActivity : PasscodeActivity(), Scrollable {

    private lateinit var binding: ActivityUpgradeAccountBinding
    private val viewModel by viewModels<UpgradeAccountViewModel>()

    private var displayedAccountType = INVALID_VALUE

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isFinishing) {
                return
            }

            when (intent.getIntExtra(
                BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION
            )) {
                UPDATE_GET_PRICING -> setPricingInfo()
                UPDATE_ACCOUNT_DETAILS -> showAvailableAccount()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUpgradeAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.initPayments(this)
        setUpView()
        setUpObservers()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(updateMyAccountReceiver)
        viewModel.destroyPayments()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.manageActivityResult(requestCode, resultCode, intent)
    }

    private fun setUpView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        ListenScrollChangesHelper().addViewToListen(
            binding.scrollView
        ) { _: View?, _: Int, _: Int, _: Int, _: Int ->
            checkScroll()
        }

        binding.upgradeProliteLayout.setOnClickListener { onUpgradeClick(PRO_LITE) }
        binding.upgradeProILayout.setOnClickListener { onUpgradeClick(PRO_I) }
        binding.upgradeProIiLayout.setOnClickListener { onUpgradeClick(PRO_II) }
        binding.upgradeProIiiLayout.setOnClickListener { onUpgradeClick(PRO_III) }

        binding.lblCustomPlan.setOnClickListener {
            askForCustomizedPlan(this, megaApi.myEmail, viewModel.getAccountType())
        }

        binding.semitransparentLayer.setOnClickListener { cancelClick() }

        setAccountDetails()
        refreshAccountInfo()
        showAvailableAccount()
        checkScroll()
    }

    private fun cancelClick() {
        checkScroll()

        if (!isDarkMode(this)) {
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        }

        binding.semitransparentLayer.isVisible = false
        binding.availablePaymentMethods.isVisible = false
    }

    private fun setUpObservers() {
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )

        viewModel.getQueryPurchasesMessage().observe(this, ::showQueryPurchasesResult)
        viewModel.onUpdatePricing().observe(this, ::updatePricing)
    }

    private fun showQueryPurchasesResult(message: String?) {
        if (isTextEmpty(message)) {
            return
        }

        showAlert(this, message, null)
        viewModel.resetQueryPurchasesMessage()
    }

    private fun updatePricing(update: Boolean) {
        if (update) {
            setPricingInfo()
            viewModel.resetUpdatePricing()
        }
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.apply {
            setBackgroundColor(
                if (isDarkMode(this@UpgradeAccountActivity) && withElevation) getColorForElevation(
                    this@UpgradeAccountActivity,
                    elevation
                )
                else ContextCompat.getColor(this@UpgradeAccountActivity, R.color.white_dark_grey)
            )

            setElevation(if (withElevation) elevation else 0f)
        }
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
            FREE -> {
                textValue = R.string.free_account
                colorValue = R.color.green_500_green_400
            }
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
            logWarning("Exception formatting string", e)
        }

        binding.textOfMyAccount.text =
            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun setPricingInfo() {
        val productAccounts = viewModel.getProductAccounts()

        if (productAccounts == null) {
            logDebug("productAccounts == null")
            app.askForPricing()
            return
        }

        for (i in productAccounts.indices) {
            val account = productAccounts[i]

            if (account.months == 1) {
                val textToShow: Spanned = viewModel.getPriceString(this, account, true)
                val textStorage: Spanned = viewModel.generateByteString(
                    this,
                    account.storage.toLong(),
                    TYPE_STORAGE_LABEL
                )

                val textTransfer: Spanned = viewModel.generateByteString(
                    this,
                    account.transfer.toLong(),
                    TYPE_TRANSFER_LABEL
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

        if (displayedAccountType != INVALID_VALUE) {
            onUpgradeClick(displayedAccountType)
        }
    }

    private fun showAvailableAccount() {
        when (viewModel.getAccountType()) {
            PRO_I -> binding.upgradeProILayoutTransparent.isVisible = true
            PRO_II -> binding.upgradeProIiLayoutTransparent.isVisible = true
            PRO_III -> binding.upgradeProIiiLayoutTransparent.isVisible = true
            PRO_LITE -> binding.upgradeProliteLayoutTransparent.isVisible = true
        }
    }

    private fun onUpgradeClick(upgradeType: Int) {
        if (viewModel.getPaymentBitSet() == null) {
            logWarning("PaymentBitSet Null")
            return
        }

        binding.availablePaymentMethods.isVisible = true

        var color = 0
        var title = 0

        when (upgradeType) {
            PRO_LITE -> {
                color = R.color.lite_account
                title = R.string.prolite_account
            }
            PRO_I -> {
                color = R.color.red_600_red_300
                title = R.string.pro1_account
            }
            PRO_II -> {
                color = R.color.red_600_red_300
                title = R.string.pro2_account
            }
            PRO_III -> {
                color = R.color.red_600_red_300
                title = R.string.pro3_account
            }
        }

        binding.paymentTextPaymentTitle.apply {
            setTextColor(ContextCompat.getColor(this@UpgradeAccountActivity, color))
            text = StringResourcesUtils.getString(title)
        }

        binding.paymentMethodGoogleWalletLayer.isVisible = false

        var textGoogleWallet = StringResourcesUtils.getString(PAY_METHOD_RES_ID)

        try {
            textGoogleWallet = textGoogleWallet.replace(
                "[A]", "<font color='"
                        + getColorHexString(this, R.color.grey_900_grey_100)
                        + "'>"
            )
            textGoogleWallet = textGoogleWallet.replace("[/A]", "</font>")
        } catch (e: java.lang.Exception) {
            LogUtil.logError("Exception formatting string", e)
        }

        binding.paymentMethodGoogleWalletText.text =
            HtmlCompat.fromHtml(textGoogleWallet, HtmlCompat.FROM_HTML_MODE_LEGACY)

        binding.paymentMethodGoogleWalletIcon.setImageResource(PAY_METHOD_ICON_RES_ID)
        binding.options.isVisible = false
        binding.layoutButtons.isVisible = false

        binding.cancelButton.setOnClickListener {
            cancelClick()
        }

        binding.continueButton.apply {
            setOnClickListener {
                when (displayedAccountType) {
                    PRO_I -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) SKU_PRO_I_MONTH
                        else SKU_PRO_I_YEAR
                    )
                    PRO_II -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) SKU_PRO_II_MONTH
                        else SKU_PRO_II_YEAR
                    )
                    PRO_III -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) SKU_PRO_III_MONTH
                        else SKU_PRO_III_YEAR
                    )
                    PRO_LITE -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) SKU_PRO_LITE_MONTH
                        else SKU_PRO_LITE_YEAR
                    )
                }
            }

            isEnabled = false

            setTextColor(
                ContextCompat.getColor(
                    this@UpgradeAccountActivity,
                    R.color.grey_700_026_grey_300_026
                )
            )
        }

        showPaymentMethods()
        refreshAccountInfo()

        if (!viewModel.isInventoryFinished()) {
            logDebug("if (!myAccountInfo.isInventoryFinished())")
            binding.paymentMethodGoogleWallet.isVisible = false
        }

        binding.paymentMethodGoogleWalletIcon.isVisible = true
        binding.semitransparentLayer.isVisible = true
        window.statusBarColor = ContextCompat.getColor(this, R.color.grey_020_white_020)
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.white_dark_grey))
        binding.toolbar.elevation = 0F
        displayedAccountType = upgradeType
        showDisplayedAccount()
    }

    private fun showPaymentMethods() {
        viewModel.checkProductAccounts() ?: return
        val paymentBitSet = viewModel.getPaymentBitSet()

        if (paymentBitSet == null) {
            logWarning("Not payment bit set received!!!")
            binding.paymentTextPaymentMethod.text =
                StringResourcesUtils.getString(R.string.no_available_payment_method)
            binding.paymentMethodGoogleWallet.isVisible = false
            return
        }

        if (!viewModel.isInventoryFinished()) {
            logDebug("if (!myAccountInfo.isInventoryFinished())")
            binding.paymentMethodGoogleWallet.isVisible = false
        } else if (isPaymentMethodAvailable(paymentBitSet, PAYMENT_METHOD_GOOGLE_WALLET)) {
            binding.paymentMethodGoogleWallet.isVisible = true
            binding.layoutButtons.isVisible = true
            binding.options.isVisible = true

            binding.continueButton.apply {
                isEnabled = true
                setTextColor(getThemeColor(this@UpgradeAccountActivity, R.attr.colorSecondary))
            }

            binding.billedMonthly.isVisible = true
            binding.billedYearly.isVisible = true
            binding.paymentTextPaymentMethod.text =
                StringResourcesUtils.getString(R.string.payment_method)
        } else {
            binding.paymentMethodGoogleWallet.isVisible = false
            binding.layoutButtons.isVisible = false
            binding.options.isVisible = false
            binding.billedMonthly.isVisible = false
            binding.billedYearly.isVisible = false
            binding.paymentTextPaymentMethod.text =
                StringResourcesUtils.getString(R.string.no_available_payment_method)
        }
    }

    private fun showDisplayedAccount() {
        val accounts = viewModel.checkProductAccounts() ?: return

        for (i in accounts.indices) {
            val account = accounts[i]

            if (account.level == displayedAccountType) {
                val textToShow: Spanned = viewModel.getPriceString(this, account, false)

                if (account.months == 1) {
                    binding.billedMonthly.text = textToShow
                } else if (account.months == 12) {
                    binding.billedYearly.text = textToShow
                }
            }
        }

        when (displayedAccountType) {
            PRO_I -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_I_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_I_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_II -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_II_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_II_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_III -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_III_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_III_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_LITE -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_LITE_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_LITE_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
        }
    }

    /**
     * Method to check if monthly billing period has been selected
     *
     * @return True if monthly billing period has been selected or false otherwise.
     */
    private fun isMonthlyBillingPeriodSelected(): Boolean {
        return binding.billingPeriod.checkedRadioButtonId == R.id.billed_monthly
    }
}