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
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE
import mega.privacy.android.app.constants.BroadcastConstants.INVALID_ACTION
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.databinding.ActivityChooseUpgradeAccountBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.app.utils.Constants.UPDATE_GET_PRICING
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.util.Locale

open class ChooseAccountActivity : PasscodeActivity(), Scrollable {

    companion object {
        private const val BILLING_WARNING_SHOWN = "BILLING_WARNING_SHOWN"
    }

    protected lateinit var binding: ActivityChooseUpgradeAccountBinding
    protected val viewModel by viewModels<ChooseUpgradeAccountViewModel>()

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

        unregisterReceiver(updateMyAccountReceiver)
        destroyPayments()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onFreeClick()
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
            title = StringResourcesUtils.getString(R.string.choose_account_fragment)
        }

        binding.billingWarningClose.setOnClickListener {
            binding.billingWarningLayout.isVisible = false
            checkScroll()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        binding.chooseAccountFreeLayout.setOnClickListener { onFreeClick() }
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

        refreshAccountInfo()
        checkScroll()
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
            IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.scrollView.canScrollVertically(SCROLLING_UP_DIRECTION)
                || binding.billingWarningLayout.isVisible
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
        var textToShowFreeStorage = "[A] 20 GB+ [/A]" +
                StringResourcesUtils.getString(R.string.label_storage_upgrade_account) + " "

        try {
            textToShowFreeStorage = textToShowFreeStorage.replace(
                "[A]", "<font color='"
                        + getColorHexString(this, R.color.grey_900_grey_100)
                        + "'>"
            ).replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        binding.storageFree.text =
            "$textToShowFreeStorage<sup><small><font color='#ff333a'>1</font></small></sup>"
                .toSpannedHtmlText()

        var textToShowFreeBandwidth = "[A] " +
                StringResourcesUtils.getString(R.string.limited_bandwith) +
                "[/A] " +
                StringResourcesUtils.getString(R.string.label_transfer_quota_upgrade_account)

        try {
            textToShowFreeBandwidth = textToShowFreeBandwidth
                .replace(
                    "[A]", "<font color='"
                            + getColorHexString(this, R.color.grey_900_grey_100)
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

    private fun onFreeClick() {
        onUpgradeClick(FREE)
    }

    /**
     * Select a payment for the new acco
     *
     * @param upgradeType Selected payment plan.
     */
    protected open fun onUpgradeClick(upgradeType: Int) {
        val intent = Intent(this, ManagerActivity::class.java)
            .putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
            .putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
            .putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)
            .putExtra(IntentConstants.EXTRA_UPGRADE_ACCOUNT, upgradeType != FREE)
            .putExtra(IntentConstants.EXTRA_ACCOUNT_TYPE, upgradeType)

        startActivity(intent)
        finish()
    }
}