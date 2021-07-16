package mega.privacy.android.app.upgradeAccount

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE
import mega.privacy.android.app.constants.BroadcastConstants.INVALID_ACTION
import mega.privacy.android.app.databinding.ActivityChooseUpgradeAccountBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import nz.mega.sdk.MegaApiJava

open class ChooseAccountActivity: PasscodeActivity(), Scrollable {

    protected lateinit var binding: ActivityChooseUpgradeAccountBinding
    protected val viewModel by viewModels<ChooseUpgradeAccountViewModel>()

    private var displayedAccountType = INVALID_VALUE

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            manageUpdateReceiver(intent.getIntExtra(ACTION_TYPE, INVALID_ACTION))
        }
    }

    protected open fun manageUpdateReceiver(action: Int) {
        if (isFinishing) {
            return
        }

        when (action) {
            UPDATE_GET_PRICING -> setPricingInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseUpgradeAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.initPayments(this)
        setupView()
        setupObservers()
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

    private fun setupView() {
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

        binding.chooseAccountFreeLayout.setOnClickListener {  }
        binding.upgradeProliteLayout.setOnClickListener { onUpgradeClick(PRO_LITE) }
        binding.upgradeProILayout.setOnClickListener { onUpgradeClick(PRO_I) }
        binding.upgradeProIiLayout.setOnClickListener { onUpgradeClick(PRO_II) }
        binding.upgradeProIiiLayout.setOnClickListener { onUpgradeClick(PRO_III) }

        binding.lblCustomPlan.apply {
            isVisible = false

            var textToShow = StringResourcesUtils.getString(R.string.label_custom_plan)
            val strColor =
                Util.getHexValue(
                    ColorUtils.getThemeColor(
                        this@ChooseAccountActivity,
                        R.attr.colorSecondary
                    )
                )

            textToShow = textToShow.replace("[A]", "<b><font color='$strColor'>")
                .replace("[/A]", "</font></b>")

            text = textToShow.toSpannedHtmlText()

            setOnClickListener {
                AlertsAndWarnings.askForCustomizedPlan(
                    this@ChooseAccountActivity,
                    megaApi.myEmail,
                    viewModel.getAccountType()
                )
            }
        }

        binding.semitransparentLayer.setOnClickListener { cancelClick() }

        refreshAccountInfo()
        checkScroll()
    }

    /**
     * Hides the current selected payment plan.
     */
    private fun cancelClick() {
        checkScroll()

        if (!Util.isDarkMode(this)) {
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        }

        binding.semitransparentLayer.isVisible = false
        binding.availablePaymentMethods.isVisible = false
    }

    private fun setupObservers() {
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )

        viewModel.getQueryPurchasesMessage().observe(this, ::showQueryPurchasesResult)
        viewModel.onUpdatePricing().observe(this, ::updatePricing)
    }

    /**
     * Shows the result of a purchase as an alert.
     *
     * @param message String to show as message alert.
     */
    private fun showQueryPurchasesResult(message: String?) {
        if (TextUtil.isTextEmpty(message)) {
            return
        }

        Util.showAlert(this, message, null)
        viewModel.resetQueryPurchasesMessage()
    }

    /**
     * Updates the pricing info if needed.
     *
     * @param update True if should update, false otherwise.
     */
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

        ColorUtils.changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.apply {
            setBackgroundColor(
                if (Util.isDarkMode(this@ChooseAccountActivity) && withElevation) ColorUtils.getColorForElevation(
                    this@ChooseAccountActivity,
                    elevation
                )
                else ContextCompat.getColor(this@ChooseAccountActivity, R.color.white_dark_grey)
            )

            setElevation(if (withElevation) elevation else 0f)
        }
    }

    private fun setPricingInfo() {
        val productAccounts = viewModel.getProductAccounts()

        if (productAccounts == null) {
            LogUtil.logDebug("productAccounts == null")
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

        if (displayedAccountType != INVALID_VALUE) {
            onUpgradeClick(displayedAccountType)
        }
    }

    /**
     * Shows the selected payment plan.
     *
     * @param upgradeType Selected payment plan.
     */
    private fun onUpgradeClick(upgradeType: Int) {
        if (viewModel.getPaymentBitSet() == null) {
            LogUtil.logWarning("PaymentBitSet Null")
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
            setTextColor(ContextCompat.getColor(this@ChooseAccountActivity, color))
            text = StringResourcesUtils.getString(title)
        }

        binding.paymentMethodGoogleWalletLayer.isVisible = false

        var textGoogleWallet = StringResourcesUtils.getString(BillingManagerImpl.PAY_METHOD_RES_ID)

        try {
            textGoogleWallet = textGoogleWallet.replace(
                "[A]", "<font color='"
                        + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                        + "'>"
            )
            textGoogleWallet = textGoogleWallet.replace("[/A]", "</font>")
        } catch (e: java.lang.Exception) {
            LogUtil.logError("Exception formatting string", e)
        }

        binding.paymentMethodGoogleWalletText.text =
            HtmlCompat.fromHtml(textGoogleWallet, HtmlCompat.FROM_HTML_MODE_LEGACY)

        binding.paymentMethodGoogleWalletIcon.setImageResource(BillingManagerImpl.PAY_METHOD_ICON_RES_ID)
        binding.options.isVisible = false
        binding.layoutButtons.isVisible = false

        binding.cancelButton.setOnClickListener {
            cancelClick()
        }

        binding.continueButton.apply {
            setOnClickListener {
                when (displayedAccountType) {
                    PRO_I -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) BillingManagerImpl.SKU_PRO_I_MONTH
                        else BillingManagerImpl.SKU_PRO_I_YEAR
                    )
                    PRO_II -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) BillingManagerImpl.SKU_PRO_II_MONTH
                        else BillingManagerImpl.SKU_PRO_II_YEAR
                    )
                    PRO_III -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) BillingManagerImpl.SKU_PRO_III_MONTH
                        else BillingManagerImpl.SKU_PRO_III_YEAR
                    )
                    PRO_LITE -> viewModel.launchPayment(
                        if (isMonthlyBillingPeriodSelected()) BillingManagerImpl.SKU_PRO_LITE_MONTH
                        else BillingManagerImpl.SKU_PRO_LITE_YEAR
                    )
                }
            }

            isEnabled = false

            setTextColor(
                ContextCompat.getColor(
                    this@ChooseAccountActivity,
                    R.color.grey_700_026_grey_300_026
                )
            )
        }

        showPaymentMethods()
        refreshAccountInfo()

        if (!viewModel.isInventoryFinished()) {
            LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())")
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
            LogUtil.logWarning("Not payment bit set received!!!")
            binding.paymentTextPaymentMethod.text =
                StringResourcesUtils.getString(R.string.no_available_payment_method)
            binding.paymentMethodGoogleWallet.isVisible = false
            return
        }

        if (!viewModel.isInventoryFinished()) {
            LogUtil.logDebug("if (!myAccountInfo.isInventoryFinished())")
            binding.paymentMethodGoogleWallet.isVisible = false
        } else if (Util.isPaymentMethodAvailable(
                paymentBitSet,
                MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET
            )
        ) {
            binding.paymentMethodGoogleWallet.isVisible = true
            binding.layoutButtons.isVisible = true
            binding.options.isVisible = true

            binding.continueButton.apply {
                isEnabled = true
                setTextColor(
                    ColorUtils.getThemeColor(
                        this@ChooseAccountActivity,
                        R.attr.colorSecondary
                    )
                )
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
                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_I_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_I_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_II -> {
                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_II_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_II_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_III -> {
                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_III_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_III_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        binding.billedMonthly.isChecked = true
                    }

                    binding.billedYearly.isVisible = false
                }
            }
            PRO_LITE -> {
                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_LITE_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        binding.billedYearly.isChecked = true
                    }

                    binding.billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(BillingManagerImpl.SKU_PRO_LITE_YEAR)) {
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