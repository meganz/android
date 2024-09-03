package mega.privacy.android.app.upgradeAccount

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.account.AccountStorageViewModel
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.view.UpgradeAccountView
import mega.privacy.android.app.utils.billing.PaymentUtils
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PaymentPlatformType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.BuyProIEvent
import mega.privacy.mobile.analytics.event.BuyProIIEvent
import mega.privacy.mobile.analytics.event.BuyProIIIEvent
import mega.privacy.mobile.analytics.event.BuyProLiteEvent
import mega.privacy.mobile.analytics.event.CancelUpgradeMyAccountEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountCancelledEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountPurchaseSucceededEvent
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UpgradeAccountFragment : Fragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val upgradeAccountViewModel by activityViewModels<UpgradeAccountViewModel>()
    private val accountStorageViewModel by activityViewModels<AccountStorageViewModel>()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    internal lateinit var upgradeAccountActivity: UpgradeAccountActivity


    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upgradeAccountActivity = activity as UpgradeAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { UpgradeAccountBody() }
        viewLifecycleOwner.collectFlow(billingViewModel.billingUpdateEvent) {
            if (it is BillingEvent.OnPurchaseUpdate) {
                onPurchasesUpdated(it.purchases)
                billingViewModel.markHandleBillingEvent()
            }
        }
    }


    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun UpgradeAccountBody() {
        val uiState by upgradeAccountViewModel.state.collectAsStateWithLifecycle()
        val accountUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()

        val mode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        BackHandler { trackAndFinish() }
        OriginalTempTheme(isDark = mode.isDarkMode()) {
            UpgradeAccountView(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                },
                state = uiState,
                accountStorageState = accountUiState,
                onBackPressed = {
                    Analytics.tracker.trackEvent(CancelUpgradeMyAccountEvent)
                    trackAndFinish()
                },
                onBuyClicked = {
                    upgradeAccountViewModel.currentPaymentCheck(uiState.chosenPlan)
                    if (uiState.currentPayment == UpgradePayment() ||
                        PaymentPlatformType.SUBSCRIPTION_FROM_GOOGLE_PLATFORM == uiState.currentPayment.currentPayment?.platformType
                    ) {
                        startPurchase(uiState.isMonthlySelected, uiState.chosenPlan)
                    }
                    Analytics.tracker.trackEvent(UpgradeAccountBuyButtonPressedEvent)
                },
                onPlayStoreLinkClicked = this::redirectToPlayStoreSubscription,
                onPricingPageClicked = this::redirectToPricingPage,
                onChoosingMonthlyYearlyPlan = upgradeAccountViewModel::onSelectingMonthlyPlan,
                onChoosingPlanType = {
                    with(upgradeAccountViewModel) {
                        if (isBillingAvailable()) {
                            onSelectingPlanType(it)
                        } else {
                            Timber.w("Billing not available")
                            setBillingWarningVisibility(true)
                        }
                    }
                },
                showBillingWarning = { upgradeAccountViewModel.setBillingWarningVisibility(true) },
                hideBillingWarning = { upgradeAccountViewModel.setBillingWarningVisibility(false) },
                onDialogConfirmButtonClicked = {
                    upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
                        showBuyNewSubscriptionDialog = false
                    )
                    startPurchase(
                        uiState.isMonthlySelected,
                        uiState.chosenPlan
                    )
                },
                onDialogDismissButtonClicked = {
                    upgradeAccountViewModel.setShowBuyNewSubscriptionDialog(
                        showBuyNewSubscriptionDialog = false
                    )
                },
                showUpgradeWarningBanner = uiState.isCrossAccountMatch.not()
            )
        }
    }

    private fun trackAndFinish() {
        Analytics.tracker.trackEvent(UpgradeAccountCancelledEvent)
        upgradeAccountActivity.finish()
    }

    private fun startPurchase(
        isMonthlySelected: Boolean,
        chosenPlan: AccountType,
    ) {
        when (chosenPlan) {
            AccountType.PRO_LITE -> Analytics.tracker.trackEvent(BuyProLiteEvent)
            AccountType.PRO_I -> Analytics.tracker.trackEvent(BuyProIEvent)
            AccountType.PRO_II -> Analytics.tracker.trackEvent(BuyProIIEvent)
            AccountType.PRO_III -> Analytics.tracker.trackEvent(BuyProIIIEvent)
            else -> {}
        }

        billingViewModel.startPurchase(
            upgradeAccountActivity,
            getProductId(
                isMonthlySelected,
                chosenPlan
            )
        )
    }

    private fun redirectToPlayStoreSubscription(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(ACTION_VIEW, uriUrl)
        runCatching {
            startActivity(launchBrowser)
        }.onFailure {
            Timber.e("Failed to open play store subscription page with error: ${it.message}")
        }
    }

    private fun redirectToPricingPage(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(requireContext(), WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    private fun onPurchasesUpdated(
        purchases: List<MegaPurchase>,
    ) {
        if (purchases.isNotEmpty()) {
            val purchase = purchases.first()
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            if (billingViewModel.isPurchased(purchase)) {
                Analytics.tracker.trackEvent(UpgradeAccountPurchaseSucceededEvent)

                //payment has been processed
                Timber.d(
                    "Purchase $sku successfully, subscription type is: "
                            + PaymentUtils.getSubscriptionType(
                        sku,
                        upgradeAccountActivity
                    ) + ", subscription renewal type is: "
                            + PaymentUtils.getSubscriptionRenewalType(sku, upgradeAccountActivity)
                )
                RatingHandlerImpl(upgradeAccountActivity).updateTransactionFlag(true)
            } else {
                //payment is being processed or in unknown state
                Timber.d("Purchase %s is being processed or in unknown state.", sku)
            }
        } else {
            //down grade case
            Timber.d("Downgrade, the new subscription takes effect when the old one expires.")
        }

        if (myAccountInfo.isUpgradeFromAccount()) {
            val intent = Intent(upgradeAccountActivity, MyAccountActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            upgradeAccountActivity.startActivity(intent)
        } else {
            upgradeAccountActivity.onBackPressedDispatcher.onBackPressed()
        }
    }
}