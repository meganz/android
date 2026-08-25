package mega.privacy.android.feature.payment.presentation.upgrade

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.navigation.payment.toSource
import mega.privacy.mobile.analytics.event.AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.AdsUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.BackButtonPressedEvent
import mega.privacy.mobile.analytics.event.GetStartedForFreeUpgradePlanButtonPressedEvent
import mega.privacy.mobile.analytics.event.MaybeLaterUpgradeAccountButtonPressedEvent

@Composable
fun UpgradeAccountRoute(
    isNewCreationAccount: Boolean,
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
    onBack: () -> Unit = {},
    chooseAccountViewModel: UpgradeAccountViewModel = hiltViewModel<UpgradeAccountViewModel, UpgradeAccountViewModel.Factory> { factory ->
        factory.create(isUpgradeAccount = isUpgradeAccount)
    },
    billingViewModel: BillingViewModel = hiltViewModel<BillingViewModel>(),
    accountStorageViewModel: AccountStorageViewModel = hiltViewModel<AccountStorageViewModel>(),
) {
    val uiState by chooseAccountViewModel.state.collectAsStateWithLifecycle()
    val accountStorageUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val activity = LocalActivity.current
    val events = remember(uiState.currentSubscriptionPlan) {
        upgradeAccountEvents(uiState.currentSubscriptionPlan)
    }
    // The current plan is only monitored for the upgrade flow; the onboarding flow always shows a
    // free account, so its screen-view event does not have to wait for one.
    val isAccountResolved = !isUpgradeAccount || uiState.currentSubscriptionPlan != null

    BackHandler(onBack = onBack)

    LaunchedOnceEffect(isAccountResolved) {
        if (isAccountResolved) {
            Analytics.tracker.trackEvent(events.screenView)
        }
    }

    LaunchedEffect(Unit) {
        billingViewModel.billingUpdateEvent.collect {
            if (it is BillingEvent.OnPurchaseUpdate) {
                activity?.let { activity ->
                    onPurchasesUpdated(
                        activity = activity,
                        isUpgradeAccount = isUpgradeAccount,
                        openFromSource = openFromSource,
                    )
                }
                billingViewModel.markHandleBillingEvent()
            }
        }
    }

    val screen: @Composable (Boolean) -> Unit = { isSubscriptionRevampEnabled ->
        UpgradeAccountScreen(
            uiState = uiState,
            accountStorageUiState = accountStorageUiState,
            isNewCreationAccount = isNewCreationAccount,
            isUpgradeAccount = isUpgradeAccount,
            isSubscriptionRevampEnabled = isSubscriptionRevampEnabled,
            onFreePlanClicked = {
                Analytics.tracker.trackEvent(
                    GetStartedForFreeUpgradePlanButtonPressedEvent
                )
                activity?.let {
                    onFreeClick(
                        activity = it,
                        onBack = onBack
                    )
                }
            },
            maybeLaterClicked = {
                Analytics.tracker.trackEvent(
                    MaybeLaterUpgradeAccountButtonPressedEvent
                )
                activity?.let {
                    onFreeClick(
                        activity = it,
                        onBack = onBack
                    )
                }
            },
            onInAppCheckoutClick = { subscription ->
                sendAccountTypeAnalytics(
                    isUpgradeAccount = isUpgradeAccount,
                    openFromSource = openFromSource,
                    planType = subscription.accountType,
                    isUpgradeAccountDueToAds = accountStorageViewModel.isUpgradeAccountDueToAds(),
                    events = events,
                )
                activity?.let {
                    billingViewModel.startPurchase(
                        activity = activity,
                        subscription = subscription,
                        source = openFromSource.toSource()
                    )
                }
            },
            onSubscriptionUnavailableLearnMoreClick = {
                activity?.let {
                    megaNavigator.launchUrl(it, SUBSCRIPTION_UNAVAILABLE_LEARN_MORE_URL)
                }
            },
            onPricingPageClick = {
                activity?.let {
                    megaNavigator.launchUrl(it, PRICING_PAGE_URL)
                }
            },
            onOfferExpired = chooseAccountViewModel::onOfferExpired,
            onBack = {
                Analytics.tracker.trackEvent(BackButtonPressedEvent)
                onBack()
            },
        )
    }

    FeatureFlagGate(
        feature = ApiFeatures.SubscriptionDiscountRevamp,
        disabled = { screen(false) },
        enabled = { screen(true) },
    )
}

private const val SUBSCRIPTION_UNAVAILABLE_LEARN_MORE_URL =
    "https://help.mega.io/plans-storage"

private const val PRICING_PAGE_URL = "https://mega.io/pro"


private fun sendAccountTypeAnalytics(
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
    planType: AccountType,
    isUpgradeAccountDueToAds: Boolean,
    events: UpgradeAccountEvents,
) {
    if (isUpgradeAccount) {
        if (openFromSource == UpgradeAccountSource.ADS_FREE_SCREEN) {
            Analytics.tracker.trackEvent(AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent)
        } else if (isUpgradeAccountDueToAds) {
            Analytics.tracker.trackEvent(AdsUpgradeAccountPlanPageBuyButtonPressedEvent)
        }
    }
    events.buyPlanPressed[planType]?.let { Analytics.tracker.trackEvent(it) }
}

private fun onFreeClick(
    activity: Activity,
    onBack: () -> Unit,
) {
    if (activity is UpgradeAccountActivity) {
        activity.finish()
    } else {
        onBack()
    }
}

private fun onPurchasesUpdated(
    activity: Activity,
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
) {
    if (isUpgradeAccount) {
        if (openFromSource == UpgradeAccountSource.MY_ACCOUNT_SCREEN) {
            activity.finish()
        }
        // other cases stay in the same activity
    } else {
        activity.finish()
    }
}
