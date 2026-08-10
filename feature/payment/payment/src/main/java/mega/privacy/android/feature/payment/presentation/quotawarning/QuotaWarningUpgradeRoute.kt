package mega.privacy.android.feature.payment.presentation.quotawarning

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType

/**
 * Route for the quota-warning upsell screen. Wires the [QuotaWarningUpgradeViewModel] state, the
 * Google Play purchase flow via [BillingViewModel], and navigation to the full plan list.
 *
 * @param onViewAllPlans navigates to the full upgrade/plan-list screen
 * @param onBack closes the screen
 */
@Composable
fun QuotaWarningUpgradeRoute(
    type: QuotaWarningType,
    trigger: QuotaWarningTrigger,
    onViewAllPlans: () -> Unit,
    onBack: () -> Unit,
    viewModel: QuotaWarningUpgradeViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val megaNavigator = rememberMegaNavigator()

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        billingViewModel.billingUpdateEvent.collect {
            if (it is BillingEvent.OnPurchaseUpdate) {
                billingViewModel.markHandleBillingEvent()
                onBack()
            }
        }
    }

    QuotaWarningUpgradeScreen(
        type = type,
        trigger = trigger,
        uiState = uiState,
        onUpgradeClick = { subscription ->
            activity?.let {
                billingViewModel.startPurchase(
                    activity = it,
                    subscription = subscription,
                    source = UpgradeSource.Main,
                )
            }
        },
        onViewAllPlansClick = onViewAllPlans,
        onLearnMoreClick = {
            activity?.let { megaNavigator.launchUrl(it, TRANSFER_QUOTA_LEARN_MORE_URL) }
        },
        onContactSupportClick = {
            activity?.let {
                megaNavigator.openAskForCustomizedPlan(
                    context = it,
                    email = uiState.email,
                    accountType = uiState.currentPlan ?: AccountType.FREE,
                )
            }
        },
        onManagePlanClick = {
            activity?.let {
                // Keep the pricing page intact — the "no plans" param would suppress the plan list
                megaNavigator.launchUrl(it, MANAGE_PLAN_URL, appendNoPlansParam = false)
            }
        },
        onClose = onBack,
    )
}

private const val TRANSFER_QUOTA_LEARN_MORE_URL =
    "https://help.mega.io/plans-storage/space-storage/transfer-quota"
private const val MANAGE_PLAN_URL = "https://mega.io/pricing#pro-flexi"
