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
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.upgradeAccount.UpgradeAccountViewModel.Companion.TYPE_STORAGE_LABEL
import mega.privacy.android.app.activities.upgradeAccount.UpgradeAccountViewModel.Companion.TYPE_TRANSFER_LABEL
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.databinding.ActivityUpgradeAccountBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.service.iab.BillingManagerImpl
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
import mega.privacy.android.app.utils.billing.PaymentUtils.*
import nz.mega.sdk.MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET

open class UpgradeAccountActivity : PasscodeActivity(), Scrollable {

    private lateinit var binding: ActivityUpgradeAccountBinding
    private val viewModel by viewModels<UpgradeAccountViewModel>()

    private var displayedAccountType = INVALID_VALUE

    private lateinit var selectPaymentMethod: TextView
    private lateinit var googlePlayLayout: RelativeLayout
    private lateinit var googlePlayLayer: RelativeLayout
    private lateinit var optionsBilling: LinearLayout
    private lateinit var billingPeriod: RadioGroup
    private lateinit var billedMonthly: RadioButton
    private lateinit var billedYearly: RadioButton
    private lateinit var layoutButtons: LinearLayout
    private lateinit var continueButton: TextView

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

    private val updatePricingObserver = Observer<Boolean> { update ->
        if (update) {
            setPricingInfo()
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
        LiveEventBus.get(EventConstants.EVENT_UPDATE_PRICING, Boolean::class.java)
            .removeObserver(updatePricingObserver)

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

        binding.upgradeProliteLayout.setOnClickListener {
            if (binding.prolite.selectPaymentsLayout.isVisible) {
                binding.prolite.selectPaymentsLayout.isVisible = false
                showSemiTransparentLayer(false)
            } else {
                binding.proI.selectPaymentsLayout.isVisible = false
                binding.proIi.selectPaymentsLayout.isVisible = false
                binding.proIii.selectPaymentsLayout.isVisible = false
                onUpgradeClick(PRO_LITE)
            }
        }

        binding.upgradeProILayout.setOnClickListener {
            if (binding.proI.selectPaymentsLayout.isVisible) {
                binding.proI.selectPaymentsLayout.isVisible = false
            } else {
                binding.prolite.selectPaymentsLayout.isVisible = false
                binding.proIi.selectPaymentsLayout.isVisible = false
                binding.proIii.selectPaymentsLayout.isVisible = false
                showSemiTransparentLayer(false)
                onUpgradeClick(PRO_I)
            }
        }

        binding.upgradeProIiLayout.setOnClickListener {
            if (binding.proIi.selectPaymentsLayout.isVisible) {
                binding.proIi.selectPaymentsLayout.isVisible = false
            } else {
                binding.prolite.selectPaymentsLayout.isVisible = false
                binding.proI.selectPaymentsLayout.isVisible = false
                binding.proIii.selectPaymentsLayout.isVisible = false
                showSemiTransparentLayer(false)
                onUpgradeClick(PRO_II)
            }
        }

        binding.upgradeProIiiLayout.setOnClickListener {
            if (binding.proIii.selectPaymentsLayout.isVisible) {
                binding.proIii.selectPaymentsLayout.isVisible = false
            } else {
                binding.prolite.selectPaymentsLayout.isVisible = false
                binding.proI.selectPaymentsLayout.isVisible = false
                binding.proIi.selectPaymentsLayout.isVisible = false
                showSemiTransparentLayer(false)
                onUpgradeClick(PRO_III)
            }
        }

        binding.lblCustomPlan.setOnClickListener {
            askForCustomizedPlan(this, megaApi.myEmail, viewModel.getAccountType())
        }

        binding.semitransparentLayer.setOnClickListener { cancelClick() }

        setAccountDetails()
        refreshAccountInfo()
        setPricingInfo()
        showAvailableAccount()
    }

    private fun showSemiTransparentLayer(show: Boolean) {
        binding.semitransparentLayer.isVisible = show
    }

    private fun cancelClick() {
        showSemiTransparentLayer(false)
        binding.prolite.selectPaymentsLayout.isVisible = false
        binding.proI.selectPaymentsLayout.isVisible = false
        binding.proIi.selectPaymentsLayout.isVisible = false
        binding.proIii.selectPaymentsLayout.isVisible = false
    }

    private fun setUpObservers() {
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
        LiveEventBus.get(EventConstants.EVENT_UPDATE_PRICING, Boolean::class.java)
            .observeForever(updatePricingObserver)

        viewModel.getQueryPurchasesMessage().observe(this, ::showQueryPurchasesResult)
    }

    private fun showQueryPurchasesResult(message: String?) {
        if (isTextEmpty(message)) {
            return
        }

        showAlert(this, message, null)
        viewModel.resetQueryPurchasesMessage()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.appBarLayout.elevation = if (withElevation) elevation else 0f
        changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.setBackgroundColor(
            if (isDarkMode(this) && withElevation) getColorForElevation(this, elevation)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
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
        val selectPaymentMethodClicked: ScrollView

        when (upgradeType) {
            PRO_LITE -> selectPaymentMethodClicked = binding.prolite.selectPaymentsLayout
            PRO_I -> selectPaymentMethodClicked = binding.proI.selectPaymentsLayout
            PRO_II -> selectPaymentMethodClicked = binding.proIi.selectPaymentsLayout
            PRO_III -> selectPaymentMethodClicked = binding.proIii.selectPaymentsLayout
            else -> {
                displayedAccountType = INVALID_VALUE
                return
            }
        }

        if (viewModel.getPaymentBitSet() == null) {
            logWarning("PaymentBitSet Null")
            return
        }

        selectPaymentMethod =
            selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_method)

        val paymentTitle =
            selectPaymentMethodClicked.findViewById<TextView>(R.id.payment_text_payment_title)

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

        paymentTitle.setTextColor(ContextCompat.getColor(this, color))
        paymentTitle.text = StringResourcesUtils.getString(title)

        googlePlayLayout =
            selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet)

        googlePlayLayer =
            selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_layer)

        googlePlayLayer.isVisible = false

        val googleWalletText =
            selectPaymentMethodClicked.findViewById<TextView>(R.id.payment_method_google_wallet_text)

        var textGoogleWallet = StringResourcesUtils.getString(BillingManagerImpl.PAY_METHOD_RES_ID)

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

        googleWalletText.text =
            HtmlCompat.fromHtml(textGoogleWallet, HtmlCompat.FROM_HTML_MODE_LEGACY)

        selectPaymentMethodClicked.findViewById<ImageView>(R.id.payment_method_google_wallet_icon)
            .setImageResource(BillingManagerImpl.PAY_METHOD_ICON_RES_ID)

        optionsBilling = selectPaymentMethodClicked.findViewById(R.id.options)
        optionsBilling.isVisible = false
        billingPeriod = selectPaymentMethodClicked.findViewById(R.id.billing_period)
        billedMonthly = billingPeriod.findViewById(R.id.billed_monthly)
        billedYearly = billingPeriod.findViewById(R.id.billed_yearly)
        layoutButtons = selectPaymentMethodClicked.findViewById(R.id.layout_buttons)
        layoutButtons.isVisible = false

        selectPaymentMethodClicked.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            cancelClick()
        }

        continueButton = selectPaymentMethodClicked.findViewById(R.id.continue_button)
        continueButton.apply {
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
            googlePlayLayout.isVisible = false
        }

        selectPaymentMethodClicked.isVisible = true
        showSemiTransparentLayer(true)

        when (upgradeType) {
            PRO_I -> Handler().post {
                binding.scrollView.smoothScrollTo(0, binding.upgradeProILayout.top)
            }
            PRO_II -> Handler().post {
                binding.scrollView.smoothScrollTo(0, binding.upgradeProIiLayout.top)
            }
            PRO_III -> Handler().post {
                binding.scrollView.smoothScrollTo(0, binding.upgradeProIiiLayout.bottom)
            }
        }

        displayedAccountType = upgradeType
        showmyF()
    }

    private fun showPaymentMethods() {
        viewModel.checkProductAccounts() ?: return
        val paymentBitSet = viewModel.getPaymentBitSet()

        if (paymentBitSet == null) {
            logWarning("Not payment bit set received!!!")
            selectPaymentMethod.text =
                StringResourcesUtils.getString(R.string.no_available_payment_method)
            googlePlayLayout.isVisible = false
            return
        }

        if (!viewModel.isInventoryFinished()) {
            logDebug("if (!myAccountInfo.isInventoryFinished())")
            googlePlayLayout.isVisible = false
        } else if (isPaymentMethodAvailable(paymentBitSet, PAYMENT_METHOD_GOOGLE_WALLET)) {
            googlePlayLayout.isVisible = true
            layoutButtons.isVisible = true
            optionsBilling.isVisible = true
            continueButton.isEnabled = true
            continueButton.setTextColor(getThemeColor(this, R.attr.colorSecondary))
            billedMonthly.isVisible = true
            billedYearly.isVisible = true
            selectPaymentMethod.text = StringResourcesUtils.getString(R.string.payment_method)
        } else {
            googlePlayLayout.isVisible = false
            layoutButtons.isVisible = false
            optionsBilling.isVisible = false
            billedMonthly.isVisible = false
            billedYearly.isVisible = false
            selectPaymentMethod.text =
                StringResourcesUtils.getString(R.string.no_available_payment_method)
        }
    }

    private fun showmyF() {
        val accounts = viewModel.checkProductAccounts() ?: return

        for (i in accounts.indices) {
            val account = accounts[i]

            if (account.level == displayedAccountType) {
                val textToShow: Spanned = viewModel.getPriceString(this, account, false)

                if (account.months == 1) {
                    billedMonthly.text = textToShow
                } else if (account.months == 12) {
                    billedYearly.text = textToShow
                }
            }
        }

        when (displayedAccountType) {
            PRO_I -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_I_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        billedYearly.isChecked = true
                    }

                    billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_I_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        billedMonthly.isChecked = true
                    }

                    billedYearly.isVisible = false
                }
            }
            PRO_II -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_II_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        billedYearly.isChecked = true
                    }

                    billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_II_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        billedMonthly.isChecked = true
                    }

                    billedYearly.isVisible = false
                }
            }
            PRO_III -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_III_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        billedYearly.isChecked = true
                    }

                    billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_III_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        billedMonthly.isChecked = true
                    }

                    billedYearly.isVisible = false
                }
            }
            PRO_LITE -> {
                if (viewModel.isPurchasedAlready(SKU_PRO_LITE_MONTH)) {
                    if (isMonthlyBillingPeriodSelected()) {
                        billedYearly.isChecked = true
                    }

                    billedMonthly.isVisible = false
                }

                if (viewModel.isPurchasedAlready(SKU_PRO_LITE_YEAR)) {
                    if (!isMonthlyBillingPeriodSelected()) {
                        billedMonthly.isChecked = true
                    }

                    billedYearly.isVisible = false
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
        return billingPeriod.checkedRadioButtonId == R.id.billed_monthly
    }
}