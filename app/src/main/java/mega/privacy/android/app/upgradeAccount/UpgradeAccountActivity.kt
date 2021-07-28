package mega.privacy.android.app.upgradeAccount

import android.os.Bundle
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava

class UpgradeAccountActivity : ChooseAccountActivity() {

    private var displayedAccountType = INVALID_VALUE

    override fun manageUpdateReceiver(action: Int) {
        super.manageUpdateReceiver(action)

        when (action) {
            UPDATE_ACCOUNT_DETAILS -> showAvailableAccount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupView()
    }

    override fun onBackPressed() {
        finish()
    }

    private fun setupView() {
        binding.titleMyAccountType.isVisible = true
        binding.chooseAccountFreeLayout.isVisible = false

        setAccountDetails()
        showAvailableAccount()
    }

    override fun setPricingInfo() {
        super.setPricingInfo()

        if (displayedAccountType != INVALID_VALUE) {
            onUpgradeClick(displayedAccountType)
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
            LogUtil.logWarning("Exception formatting string", e)
        }

        binding.textOfMyAccount.text = text.toSpannedHtmlText()
    }

    /**
     * Shows the selected payment plan.
     *
     * @param upgradeType Selected payment plan.
     */
    override fun onUpgradeClick(upgradeType: Int) {
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
            setTextColor(ContextCompat.getColor(this@UpgradeAccountActivity, color))
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
                    this@UpgradeAccountActivity,
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
                        this@UpgradeAccountActivity,
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