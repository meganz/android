package mega.privacy.android.feature.payment.presentation.upgrade

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.sharedcomponents.container.AppContainerProvider
import mega.privacy.android.core.sharedcomponents.coroutine.collectFlow
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.feature.payment.util.PaymentUtils.getProductId
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.mobile.analytics.event.AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.AdsUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.BuyProIEvent
import mega.privacy.mobile.analytics.event.BuyProIIEvent
import mega.privacy.mobile.analytics.event.BuyProIIIEvent
import mega.privacy.mobile.analytics.event.BuyProLiteEvent
import mega.privacy.mobile.analytics.event.GetStartedForFreeUpgradePlanButtonPressedEvent
import mega.privacy.mobile.analytics.event.MaybeLaterUpgradeAccountButtonPressedEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountPlanScreenEvent
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountFragment : Fragment() {
    @Inject
    lateinit var appContainerProvider: AppContainerProvider

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val chooseAccountViewModel by activityViewModels<ChooseAccountViewModel>()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    private val accountStorageViewModel by activityViewModels<AccountStorageViewModel>()

    private lateinit var chooseAccountActivity: ChooseAccountActivity

    private val openFromSource by lazy {
        arguments?.serializable(ChooseAccountActivity.EXTRA_SOURCE)
            ?: UpgradeAccountSource.UNKNOWN
    }

    private val isUpgradeAccount by lazy {
        arguments?.getBoolean(ChooseAccountViewModel.EXTRA_IS_UPGRADE_ACCOUNT, false) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseAccountActivity = activity as ChooseAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = appContainerProvider.buildSharedAppContainer(
        context = requireContext(),
        useLegacyStatusBarColor = false
    ) {
        ChooseAccountBody()
    }.apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(billingViewModel.billingUpdateEvent) {
            if (it is BillingEvent.OnPurchaseUpdate) {
                onPurchasesUpdated(it.purchases)
                billingViewModel.markHandleBillingEvent()
            }
        }
    }

    @SuppressLint("ProduceStateDoesNotAssignValue")
    @Composable
    fun ChooseAccountBody() {
        val uiState by chooseAccountViewModel.state.collectAsStateWithLifecycle()
        val accountStorageUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()
        val isNewCreationAccount =
            arguments?.getBoolean(ExtraConstant.NEW_CREATION_ACCOUNT, false) ?: false

        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(UpgradeAccountPlanScreenEvent)
        }

        NewChooseAccountScreen(
            uiState = uiState,
            accountStorageUiState = accountStorageUiState,
            isNewCreationAccount = isNewCreationAccount,
            isUpgradeAccount = isUpgradeAccount,
            onFreePlanClicked = {
                Analytics.tracker.trackEvent(
                    GetStartedForFreeUpgradePlanButtonPressedEvent
                )
                chooseAccountActivity.onFreeClick()
            },
            maybeLaterClicked = {
                Analytics.tracker.trackEvent(
                    MaybeLaterUpgradeAccountButtonPressedEvent
                )
                chooseAccountActivity.onFreeClick()
            },
            onBuyPlanClick = { accountType, isMonthly ->
                sendAccountTypeAnalytics(accountType)
                billingViewModel.startPurchase(
                    chooseAccountActivity,
                    getProductId(isMonthly, accountType),
                )
            },
            onBack = {
                requireActivity().finish()
            }
        )
    }

    private fun sendAccountTypeAnalytics(planType: AccountType) {
        if (isUpgradeAccount) {
            if (openFromSource == UpgradeAccountSource.ADS_FREE_SCREEN) {
                Analytics.tracker.trackEvent(AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent)
            } else if (accountStorageViewModel.isUpgradeAccountDueToAds()) {
                Analytics.tracker.trackEvent(AdsUpgradeAccountPlanPageBuyButtonPressedEvent)
            }
        }
        when (planType) {
            AccountType.PRO_I -> Analytics.tracker.trackEvent(BuyProIEvent)

            AccountType.PRO_II -> Analytics.tracker.trackEvent(BuyProIIEvent)

            AccountType.PRO_III -> Analytics.tracker.trackEvent(BuyProIIIEvent)

            AccountType.PRO_LITE -> Analytics.tracker.trackEvent(BuyProLiteEvent)

            else -> Unit
        }
    }

    private fun onPurchasesUpdated(
        purchases: List<MegaPurchase>,
    ) {
        if (purchases.isNotEmpty()) {
            val purchase = purchases.first()
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            if (billingViewModel.isPurchased(purchase)) {
                //payment has been processed
                Timber.d("Purchase $sku successfully")
            } else {
                //payment is being processed or in unknown state
                Timber.d("Purchase %s is being processed or in unknown state.", sku)
            }
        } else {
            //down grade case
            Timber.d("Downgrade, the new subscription takes effect when the old one expires.")
        }

        if (isUpgradeAccount) {
            if (openFromSource == UpgradeAccountSource.MY_ACCOUNT_SCREEN) {
                megaNavigator.openMyAccountActivity(
                    context = chooseAccountActivity,
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }
            // other cases stay in the same activity
        } else {
            val bundle = Bundle().apply {
                chooseAccountActivity.intent.extras?.let { putAll(it) }
                putBoolean(ExtraConstant.EXTRA_FIRST_LOGIN, true)
                if (!containsKey(ExtraConstant.EXTRA_NEW_ACCOUNT)) {
                    putBoolean(ExtraConstant.EXTRA_NEW_ACCOUNT, true)
                }
                if (!containsKey(ExtraConstant.NEW_CREATION_ACCOUNT)) {
                    putBoolean(ExtraConstant.NEW_CREATION_ACCOUNT, true)
                }
            }

            megaNavigator.openManagerActivity(
                context = chooseAccountActivity,
                data = chooseAccountActivity.intent.data,
                action = chooseAccountActivity.intent.action,
                bundle = bundle
            )
        }

        requireActivity().finish()
    }
}