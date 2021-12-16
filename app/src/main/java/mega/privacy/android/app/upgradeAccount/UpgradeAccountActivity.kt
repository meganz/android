package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_PURCHASES_UPDATED
import mega.privacy.android.app.upgradeAccount.PaymentActivity.Companion.UPGRADE_TYPE
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import java.util.*

class UpgradeAccountActivity : ChooseAccountActivity() {

    private var upgradeAlert: AlertDialog? = null

    private val purchaseResultObserver =
        Observer<Pair<String, String>> { content -> showQueryPurchasesResult(content) }

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

        upgradeAlert?.dismiss()

        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_PURCHASES_UPDATED)
            .removeObserver(purchaseResultObserver as Observer<Any>)
    }

    override fun onBackPressed() {
        finish()
    }

    private fun setupView() {
        supportActionBar?.title = StringResourcesUtils.getString(R.string.action_upgrade_account)
            .toUpperCase(Locale.getDefault())

        binding.titleMyAccountType.isVisible = true
        binding.chooseAccountFreeLayout.isVisible = false

        setAccountDetails()
        showAvailableAccount()
    }

    private fun setupObservers() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_PURCHASES_UPDATED)
            .observeForever(purchaseResultObserver as Observer<Any>)
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
        return
    }

    /**
     * Shows the result of a purchase as an alert.
     *
     * @param content First String to show as title alert, second String to show as message alert.
     */
    private fun showQueryPurchasesResult(content: Pair<String, String>?) {
        if (content == null) {
            return
        }

        upgradeAlert = MaterialAlertDialogBuilder(this)
            .setTitle(content.first)
            .setMessage(content.second)
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_ok)) { _, _ ->
                finish()
            }
            .show()
    }
}