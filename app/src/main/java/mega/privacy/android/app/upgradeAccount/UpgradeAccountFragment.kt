package mega.privacy.android.app.upgradeAccount

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.FragmentUpgradeAccountBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util
import timber.log.Timber

@AndroidEntryPoint
class UpgradeAccountFragment : Fragment(), Scrollable {

    companion object {
        const val SUBSCRIPTION_FROM_ITUNES = 10
        const val SUBSCRIPTION_FROM_ANDROID_PLATFORM = 11
        const val SUBSCRIPTION_FROM_OTHER_PLATFORM = 12
        private const val BILLING_WARNING_SHOWN = "BILLING_WARNING_SHOWN"
    }

    private val viewModel: ChooseUpgradeAccountViewModel by viewModels()
    private lateinit var binding: FragmentUpgradeAccountBinding

    private var subscriptionWarningDialog: VerticalLayoutButtonDialog? = null

    private val updateMyAccountReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            manageUpdateReceiver(intent.getIntExtra(BroadcastConstants.ACTION_TYPE,
                BroadcastConstants.INVALID_ACTION))
        }
    }

    private fun manageUpdateReceiver(action: Int) {
        if (Activity().isFinishing) {
            return
        }

        when (action) {
            Constants.UPDATE_ACCOUNT_DETAILS -> showAvailableAccount()
            Constants.UPDATE_GET_PRICING -> setPricingInfo()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionWarningDialog?.dismiss()
        requireContext().unregisterReceiver(updateMyAccountReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentUpgradeAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BILLING_WARNING_SHOWN, binding.billingWarningLayout.isVisible)
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.refreshAccountInfo()
        setupView()
        setupObservers()

        if (savedInstanceState != null
            && savedInstanceState.getBoolean(BILLING_WARNING_SHOWN, false)
        ) {
            showBillingWarning()
        }
    }

    private fun setupView() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.action_upgrade_account)
        }

        binding.billingWarningClose.setOnClickListener {
            binding.billingWarningLayout.isVisible = false
            checkScroll()
        }

        binding.scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        binding.upgradeProliteLayout.setOnClickListener { onUpgradeClick(Constants.PRO_LITE) }
        binding.upgradeProILayout.setOnClickListener { onUpgradeClick(Constants.PRO_I) }
        binding.upgradeProIiLayout.setOnClickListener { onUpgradeClick(Constants.PRO_II) }
        binding.upgradeProIiiLayout.setOnClickListener { onUpgradeClick(Constants.PRO_III) }

        binding.titleMyAccountType.isVisible = true

        binding.lblCustomPlan.apply {
            isVisible = false

            var textToShow = getString(R.string.label_custom_plan)
            val strColor =
                Util.getHexValue(
                    ColorUtils.getThemeColor(
                        requireActivity(),
                        R.attr.colorSecondary
                    )
                )

            textToShow = textToShow.replace("[A]", "<b><font color='$strColor'>")
                .replace("[/A]", "</font></b>")

            text = textToShow.toSpannedHtmlText()

            setOnClickListener {
                AlertsAndWarnings.askForCustomizedPlan(
                    requireActivity(),
                    BaseActivity().megaApi.myEmail,
                    viewModel.getAccountType()
                )
            }
        }

        checkScroll()
        setAccountDetails()
        showAvailableAccount()
        setPricingInfo()
    }

    /**
     * Shows a warning when the billing is not available.
     */
    fun showBillingWarning() {
        binding.billingWarningLayout.isVisible = true
        checkScroll()
    }

    private fun setupObservers() {
        requireContext().registerReceiver(
            updateMyAccountReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
        )
        viewModel.onUpgradeClick().observe(viewLifecycleOwner) { upgradeType ->
            startActivity(
                Intent(requireActivity(), PaymentActivity::class.java)
                    .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
            )
        }
        viewModel.onUpgradeClickWithSubscription().observe(viewLifecycleOwner) { warning ->
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

        ColorUtils.changeStatusBarColorForElevation(requireActivity(), withElevation)

        binding.toolbar.apply {
            setBackgroundColor(
                if (Util.isDarkMode(context) && withElevation) ColorUtils.getColorForElevation(
                    requireActivity(),
                    elevation
                )
                else ContextCompat.getColor(requireActivity(), R.color.white_dark_grey)
            )

            setElevation(if (withElevation) elevation else 0f)
        }
    }

    private fun showAvailableAccount() {
        when (viewModel.getAccountType()) {
            Constants.PRO_I -> binding.upgradeProILayoutTransparent.isVisible = true
            Constants.PRO_II -> binding.upgradeProIiLayoutTransparent.isVisible = true
            Constants.PRO_III -> {
                binding.lblCustomPlan.isVisible = true
                binding.upgradeProIiiLayoutTransparent.isVisible = true
            }
            Constants.PRO_LITE -> binding.upgradeProliteLayoutTransparent.isVisible = true
        }
    }

    private fun setPricingInfo() {
        val productAccounts = viewModel.getProductAccounts()

        if (productAccounts == null) {
            Timber.d("productAccounts == null")
            viewModel.refreshPricing()
            return
        }

        for (i in productAccounts.indices) {
            val account = productAccounts[i]

            if (account.months == 1) {
                val textToShow: Spanned = viewModel.getPriceString(requireContext(), account, true)
                val textStorage: Spanned = viewModel.generateByteString(
                    requireContext(),
                    account.storage.toLong(),
                    ChooseUpgradeAccountViewModel.TYPE_STORAGE_LABEL
                )

                val textTransfer: Spanned = viewModel.generateByteString(
                    requireContext(),
                    account.transfer.toLong(),
                    ChooseUpgradeAccountViewModel.TYPE_TRANSFER_LABEL
                )

                when (account.level) {
                    Constants.PRO_I -> {
                        binding.monthProI.text = textToShow
                        binding.storageProI.text = textStorage
                        binding.bandwidthProI.text = textTransfer
                    }
                    Constants.PRO_II -> {
                        binding.monthProIi.text = textToShow
                        binding.storageProIi.text = textStorage
                        binding.bandwidthProIi.text = textTransfer
                    }
                    Constants.PRO_III -> {
                        binding.monthProIii.text = textToShow
                        binding.storageProIii.text = textStorage
                        binding.bandwidthProIii.text = textTransfer
                    }
                    Constants.PRO_LITE -> {
                        binding.monthLite.text = textToShow
                        binding.storageLite.text = textStorage
                        binding.bandwidthLite.text = textTransfer
                    }
                }
            }
        }
    }

    private fun setAccountDetails() {
        if (viewModel.isGettingInfo()) {
            binding.textOfMyAccount.text = getString(R.string.recovering_info)
            binding.textOfMyAccount.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.grey_054_white_054
                )
            )

            return
        }

        val textValue: Int
        val colorValue: Int

        when (viewModel.getAccountType()) {
            Constants.PRO_I -> {
                textValue = R.string.pro1_account
                colorValue = R.color.red_600_red_300
            }
            Constants.PRO_II -> {
                textValue = R.string.pro2_account
                colorValue = R.color.red_600_red_300
            }
            Constants.PRO_III -> {
                textValue = R.string.pro3_account
                colorValue = R.color.red_600_red_300
            }
            Constants.PRO_LITE -> {
                textValue = R.string.prolite_account
                colorValue = R.color.lite_account
            }
            else -> {
                textValue = R.string.free_account
                colorValue = R.color.green_500_green_400
            }
        }

        var text = getString(
            R.string.type_of_my_account,
            getString(textValue)
        )

        val color = ContextCompat.getColor(requireContext(), colorValue).toString()


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
                context = requireContext(),
                title = getString(R.string.title_existing_subscription),
                message = when (platformType) {
                    SUBSCRIPTION_FROM_ANDROID_PLATFORM -> {
                        getString(
                            R.string.message_subscription_from_android_platform,
                            methodName
                        )
                    }
                    SUBSCRIPTION_FROM_ITUNES -> {
                        getString(R.string.message_subscription_from_itunes)
                    }
                    else -> {
                        getString(R.string.message_subscription_from_other_platform)
                    }
                },
                positiveButtonTitle = getString(R.string.button_buy_new_subscription),
                onPositiveButtonClicked = {
                    startActivity(
                        Intent(requireActivity(), PaymentActivity::class.java)
                            .putExtra(PaymentActivity.UPGRADE_TYPE, upgradeType)
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