package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.account.AccountStorageViewModel
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountView
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import mega.privacy.android.app.upgradeAccount.view.VariantBOnboardingDialogView
import mega.privacy.android.app.utils.billing.PaymentUtils
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.AdsUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.BuyProIEvent
import mega.privacy.mobile.analytics.event.BuyProIIEvent
import mega.privacy.mobile.analytics.event.BuyProIIIEvent
import mega.privacy.mobile.analytics.event.BuyProLiteEvent
import mega.privacy.mobile.analytics.event.GetStartedForFreeUpgradePlanButtonPressedEvent
import mega.privacy.mobile.analytics.event.MaybeLaterUpgradeAccountButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantAViewProPlansButtonEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBFreePlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIIIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProLitePlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProPlanIIIDisplayedEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountPlanScreenEvent
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountFragment : Fragment() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    private val chooseAccountViewModel by activityViewModels<ChooseAccountViewModel>()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    private val accountStorageViewModel by activityViewModels<AccountStorageViewModel>()

    private lateinit var chooseAccountActivity: ChooseAccountActivity

    private val openFromSource by lazy {
        arguments?.serializable(ChooseAccountActivity.EXTRA_SOURCE)
            ?: UpgradeAccountSource.UNKNOWN
    }

    private val isUpgradeAccount by lazy {
        arguments?.getBoolean(ChooseAccountActivity.EXTRA_IS_UPGRADE_ACCOUNT, false) ?: false
    }

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseAccountActivity = activity as ChooseAccountActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { ChooseAccountBody() }
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
            arguments?.getBoolean(ManagerActivity.NEW_CREATION_ACCOUNT, false) ?: false

        val mode by monitorThemeModeUseCase()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(UpgradeAccountPlanScreenEvent)
        }

        if (uiState.isProPromoRevampEnabled) {
            SharedAppContainer(
                themeMode = mode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                useLegacyStatusBarColor = false
            ) {
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
        } else {
            MegaAppContainer(
                themeMode = mode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory
            ) {
                val modifier = Modifier
                    .semantics {
                        testTagsAsResourceId = true
                    }
                    .systemBarsPadding()
                if (uiState.enableVariantAUI) {
                    VariantAOnboardingDialogView(
                        state = uiState,
                        onSkipPressed = chooseAccountActivity::onFreeClick,
                        onViewPlansPressed = {
                            Analytics.tracker.trackEvent(
                                OnboardingUpsellingDialogVariantAViewProPlansButtonEvent
                            )
                            chooseAccountActivity.onPlanClicked(AccountType.PRO_I)
                        },
                        modifier = modifier,
                    )
                } else if (uiState.enableVariantBUI) {
                    VariantBOnboardingDialogView(
                        state = uiState,
                        accountUiState = accountStorageUiState,
                        onBackPressed = chooseAccountActivity::onFreeClick,
                        onContinueClicked = {
                            callContinueButtonAnalytics(uiState.chosenPlan)
                            if (uiState.chosenPlan === AccountType.FREE) {
                                chooseAccountActivity.onFreeClick()
                            } else {
                                billingViewModel.startPurchase(
                                    chooseAccountActivity,
                                    getProductId(uiState.isMonthlySelected, uiState.chosenPlan),
                                )
                            }
                        },
                        onChoosingMonthlyYearlyPlan = chooseAccountViewModel::onSelectingMonthlyPlan,
                        onChoosingPlanType = chooseAccountViewModel::onSelectingPlanType,
                        onLinkClicked = context::launchUrl,
                        onProIIIVisible = {
                            Analytics.tracker.trackEvent(
                                OnboardingUpsellingDialogVariantBProPlanIIIDisplayedEvent
                            )
                        },
                        modifier = modifier,
                    )
                } else {
                    ChooseAccountView(
                        state = uiState,
                        accountStorageUIState = accountStorageUiState,
                        onBackPressed = chooseAccountActivity::onFreeClick,
                        onPlanClicked = chooseAccountActivity::onPlanClicked,
                        modifier = modifier,
                    )
                }
            }
        }
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

    private fun callContinueButtonAnalytics(planType: AccountType) {
        when (planType) {
            AccountType.PRO_I -> Analytics.tracker.trackEvent(
                OnboardingUpsellingDialogVariantBProIPlanContinueButtonPressedEvent
            )

            AccountType.PRO_II -> Analytics.tracker.trackEvent(
                OnboardingUpsellingDialogVariantBProIIPlanContinueButtonPressedEvent
            )

            AccountType.PRO_III -> Analytics.tracker.trackEvent(
                OnboardingUpsellingDialogVariantBProIIIPlanContinueButtonPressedEvent
            )

            AccountType.PRO_LITE -> Analytics.tracker.trackEvent(
                OnboardingUpsellingDialogVariantBProLitePlanContinueButtonPressedEvent
            )

            else -> Analytics.tracker.trackEvent(
                OnboardingUpsellingDialogVariantBFreePlanContinueButtonPressedEvent
            )
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
                Timber.d(
                    "Purchase $sku successfully, subscription type is: "
                            + PaymentUtils.getSubscriptionType(
                        sku,
                        chooseAccountActivity
                    ) + ", subscription renewal type is: "
                            + PaymentUtils.getSubscriptionRenewalType(sku, chooseAccountActivity)
                )
                RatingHandlerImpl(chooseAccountActivity).updateTransactionFlag(true)
            } else {
                //payment is being processed or in unknown state
                Timber.d("Purchase %s is being processed or in unknown state.", sku)
            }
        } else {
            //down grade case
            Timber.d("Downgrade, the new subscription takes effect when the old one expires.")
        }

        if (isUpgradeAccount) {
            if (myAccountInfo.isUpgradeFromAccount()) {
                val intent = Intent(chooseAccountActivity, MyAccountActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                chooseAccountActivity.startActivity(intent)
            }
        } else {
            val intent = Intent(requireContext(), ManagerActivity::class.java)
                .putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                .putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
                .putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)

            startActivity(intent)
        }

        requireActivity().finish()
    }
}