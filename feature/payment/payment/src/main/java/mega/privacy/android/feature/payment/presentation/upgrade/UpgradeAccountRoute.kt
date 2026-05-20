package mega.privacy.android.feature.payment.presentation.upgrade

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.payment.UpgradeAccountSource
import mega.privacy.android.navigation.payment.toSource
import mega.privacy.mobile.analytics.event.AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.AdsUpgradeAccountPlanPageBuyButtonPressedEvent
import mega.privacy.mobile.analytics.event.BackButtonPressedEvent
import mega.privacy.mobile.analytics.event.BuyProIEvent
import mega.privacy.mobile.analytics.event.BuyProIIEvent
import mega.privacy.mobile.analytics.event.BuyProIIIEvent
import mega.privacy.mobile.analytics.event.BuyProLiteEvent
import mega.privacy.mobile.analytics.event.GetStartedForFreeUpgradePlanButtonPressedEvent
import mega.privacy.mobile.analytics.event.MaybeLaterUpgradeAccountButtonPressedEvent
import mega.privacy.mobile.analytics.event.UpgradeAccountPlanScreenEvent

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

    BackHandler(onBack = onBack)

    LaunchedOnceEffect(Unit) {
        Analytics.tracker.trackEvent(UpgradeAccountPlanScreenEvent)
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

    UpgradeAccountScreen(
        uiState = uiState,
        accountStorageUiState = accountStorageUiState,
        isNewCreationAccount = isNewCreationAccount,
        isUpgradeAccount = isUpgradeAccount,
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
                isUpgradeAccountDueToAds = accountStorageViewModel.isUpgradeAccountDueToAds()
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
        onBack = {
            Analytics.tracker.trackEvent(BackButtonPressedEvent)
            onBack()
        },
    )
}

private const val SUBSCRIPTION_UNAVAILABLE_LEARN_MORE_URL =
    "https://help.mega.io/plans-storage"


private fun sendAccountTypeAnalytics(
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
    planType: AccountType,
    isUpgradeAccountDueToAds: Boolean,
) {
    if (isUpgradeAccount) {
        if (openFromSource == UpgradeAccountSource.ADS_FREE_SCREEN) {
            Analytics.tracker.trackEvent(AdFreeDialogUpgradeAccountPlanPageBuyButtonPressedEvent)
        } else if (isUpgradeAccountDueToAds) {
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
