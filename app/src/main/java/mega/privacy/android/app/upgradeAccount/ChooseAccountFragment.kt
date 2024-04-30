package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel.Companion.getProductId
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountView
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import mega.privacy.android.app.upgradeAccount.view.VariantBOnboardingDialogView
import mega.privacy.android.app.utils.billing.PaymentUtils
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantAViewProPlansButtonEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBFreePlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIIIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProIPlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProLitePlanContinueButtonPressedEvent
import mega.privacy.mobile.analytics.event.OnboardingUpsellingDialogVariantBProPlanIIIDisplayedEvent
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChooseAccountFragment : Fragment() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    private val chooseAccountViewModel by activityViewModels<ChooseAccountViewModel>()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    internal lateinit var chooseAccountActivity: ChooseAccountActivity


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
        val mode by getThemeMode()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        MegaAppTheme(isDark = mode.isDarkMode()) {
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
                )
            } else if (uiState.enableVariantBUI) {
                VariantBOnboardingDialogView(
                    state = uiState,
                    onBackPressed = chooseAccountActivity::onFreeClick,
                    onContinueClicked = {
                        callContinueButtonAnalytics(uiState.chosenPlan)
                        val chosenPlan =
                            UpgradeAccountFragment.convertAccountTypeToInt(uiState.chosenPlan)
                        if (uiState.chosenPlan === AccountType.FREE) {
                            chooseAccountActivity.onFreeClick()
                        } else {
                            billingViewModel.startPurchase(
                                chooseAccountActivity,
                                getProductId(uiState.isMonthlySelected, chosenPlan),
                            )
                        }
                    },
                    onChoosingMonthlyYearlyPlan = chooseAccountViewModel::onSelectingMonthlyPlan,
                    onChoosingPlanType = chooseAccountViewModel::onSelectingPlanType,
                    onPlayStoreLinkClicked = this::redirectToPlayStoreSubscription,
                    onProIIIVisible = {
                        Analytics.tracker.trackEvent(
                            OnboardingUpsellingDialogVariantBProPlanIIIDisplayedEvent
                        )
                    }
                )
            } else {
                ChooseAccountView(
                    state = uiState,
                    onBackPressed = chooseAccountActivity::onFreeClick,
                    onPlanClicked = chooseAccountActivity::onPlanClicked
                )
            }
        }
    }

    private fun redirectToPlayStoreSubscription(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        runCatching {
            startActivity(launchBrowser)
        }.onFailure {
            Timber.e("Failed to open play store subscription page with error: ${it.message}")
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

        val intent = Intent(requireContext(), ManagerActivity::class.java)
            .putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
            .putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
            .putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, true)

        startActivity(intent)
        requireActivity().finish()
    }
}