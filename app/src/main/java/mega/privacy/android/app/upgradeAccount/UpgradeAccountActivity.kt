package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.PaymentActivity.Companion.UPGRADE_TYPE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import timber.log.Timber
import java.util.Locale

class UpgradeAccountActivity : ChooseAccountActivity() {

    companion object {
        const val SUBSCRIPTION_FROM_ITUNES = 10
        const val SUBSCRIPTION_FROM_ANDROID_PLATFORM = 11
        const val SUBSCRIPTION_FROM_OTHER_PLATFORM = 12
    }

    private var subscriptionWarningDialog: VerticalLayoutButtonDialog? = null

    override fun manageUpdateReceiver(action: Int) {
        super.manageUpdateReceiver(action)

        when (action) {
            UPDATE_ACCOUNT_DETAILS -> showAvailableAccount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
        setupObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionWarningDialog?.dismiss()
    }

    override fun onBackPressed() {
        finish()
    }

    private fun setupView() {
        supportActionBar?.title = StringResourcesUtils.getString(R.string.action_upgrade_account)

        binding.titleMyAccountType.isVisible = true
        binding.chooseAccountFreeLayout.isVisible = false

        setAccountDetails()
        showAvailableAccount()
    }

    private fun setupObservers() {
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
            Timber.w(e, "Exception formatting string")
        }

        binding.textOfMyAccount.text = text.toSpannedHtmlText()
    }

    /**
     * Shows the selected payment plan.
     *
     * @param upgradeType Selected payment plan.
     */
    override fun onUpgradeClick(upgradeType: Int) {
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