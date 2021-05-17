package mega.privacy.android.app.activities.upgradeAccount

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
import mega.privacy.android.app.utils.AlertsAndWarnings.askForCustomizedPlan
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.Util.isDarkMode

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

    private val updatePricingObserver = Observer<Boolean> { update ->
        if (update) {
            setPricingInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUpgradeAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpView()
        setUpObservers()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(updateMyAccountReceiver)
        LiveEventBus.get(EventConstants.EVENT_UPDATE_PRICING, Boolean::class.java)
            .removeObserver(updatePricingObserver)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return super.onOptionsItemSelected(item)
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
                binding.semitransparentLayer.isVisible = false
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
                binding.semitransparentLayer.isVisible = false
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
                binding.semitransparentLayer.isVisible = false
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
                binding.semitransparentLayer.isVisible = false
                onUpgradeClick(PRO_III)
            }
        }

        binding.lblCustomPlan.setOnClickListener {
            askForCustomizedPlan(this, megaApi.myEmail, viewModel.getAccountType())
        }

        checkScroll()
        setAccountDetails()
        refreshAccountInfo()
        setPricingInfo()
        showAvailableAccount()
    }

    private fun setUpObservers() {
        registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
        LiveEventBus.get(EventConstants.EVENT_UPDATE_PRICING, Boolean::class.java)
            .observeForever(updatePricingObserver)
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.appBarLayout.elevation = if (withElevation) elevation else 0f
        changeStatusBarColorForElevation(this, withElevation)

        binding.toolbar.setBackgroundColor(
            if (isDarkMode(this@UpgradeAccountActivity) && withElevation) getColorForElevation(
                this@UpgradeAccountActivity,
                elevation
            ) else android.R.color.transparent
        )
    }

    private fun setAccountDetails() {
        if (viewModel.isGettingInfo()) {
            binding.textOfMyAccount.text = getString(R.string.recovering_info)
            binding.textOfMyAccount.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.grey_054_white_054
                )
            )

            return
        }

        var text: String
        val color: String

        when (viewModel.getAccountType()) {
            FREE -> {
                text = getString(R.string.type_of_my_account, getString(R.string.free_account))
                color = ContextCompat.getColor(this, R.color.green_500_green_400).toString()
            }
            PRO_I -> {
                text = getString(R.string.type_of_my_account, getString(R.string.pro1_account))
                color = ContextCompat.getColor(this, R.color.red_600_red_300).toString()
            }
            PRO_II -> {
                text = getString(R.string.type_of_my_account, getString(R.string.pro2_account))
                color = ContextCompat.getColor(this, R.color.red_600_red_300).toString()
            }
            PRO_III -> {
                text = getString(R.string.type_of_my_account, getString(R.string.pro3_account))
                color = ContextCompat.getColor(this, R.color.red_600_red_300).toString()
            }
            PRO_LITE -> {
                text = getString(R.string.type_of_my_account, getString(R.string.prolite_account))
                color = ContextCompat.getColor(this, R.color.lite_account).toString()
            }
            else -> {
                text = getString(R.string.type_of_my_account, getString(R.string.free_account))
                color = ContextCompat.getColor(this, R.color.green_500_green_400).toString()
            }
        }

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
        refreshAccountInfo()
    }
}