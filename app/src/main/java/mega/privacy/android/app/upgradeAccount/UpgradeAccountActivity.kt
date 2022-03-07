package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.PaymentActivity.Companion.UPGRADE_TYPE
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import java.util.*

class UpgradeAccountActivity : ChooseAccountActivity() {

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
        supportActionBar?.title = StringResourcesUtils.getString(R.string.action_upgrade_account)
            .uppercase(Locale.getDefault())

        binding.titleMyAccountType.isVisible = true
        binding.chooseAccountFreeLayout.isVisible = false

        setAccountDetails()
        showAvailableAccount()
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


        viewModel.refreshAccountInfo()
        startActivity(Intent(this, PaymentActivity::class.java)
            .putExtra(UPGRADE_TYPE, upgradeType))
    }
}