package mega.privacy.android.feature.payment.presentation.upgrade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.feature.payment.model.AccountTypeInt
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
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

@Composable
fun ChooseAccountRoute(
    isNewCreationAccount: Boolean,
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
    onBack: () -> Unit = {},
    chooseAccountViewModel: ChooseAccountViewModel = hiltViewModel<ChooseAccountViewModel>(),
    billingViewModel: BillingViewModel = hiltViewModel<BillingViewModel>(),
    accountStorageViewModel: AccountStorageViewModel = hiltViewModel<AccountStorageViewModel>(),
) {
    val uiState by chooseAccountViewModel.state.collectAsStateWithLifecycle()
    val accountStorageUiState by accountStorageViewModel.state.collectAsStateWithLifecycle()
    val billingUIState by billingViewModel.uiState.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(UpgradeAccountPlanScreenEvent)
    }

    LaunchedEffect(Unit) {
        billingViewModel.billingUpdateEvent.collect {
            if (it is BillingEvent.OnPurchaseUpdate) {
                activity?.let { activity ->
                    onPurchasesUpdated(
                        megaNavigator = megaNavigator,
                        activity = activity,
                        isUpgradeAccount = isUpgradeAccount,
                        openFromSource = openFromSource,
                        isSingleActivityEnabled = uiState.isSingleActivityEnabled
                    )
                }
                billingViewModel.markHandleBillingEvent()
            }
        }
    }

    EventEffect(
        event = billingUIState.onExternalPurchaseClick,
        onConsumed = billingViewModel::onExternalPurchaseClickEventConsumed
    ) { url ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity?.startActivity(intent)
        }.onFailure { e ->
            Timber.Forest.e(e, "Failed to launch external purchase URL: $url")
            billingViewModel.onGeneralError()
        }
    }

    NewChooseAccountScreen(
        uiState = uiState,
        accountStorageUiState = accountStorageUiState,
        billingUIState = billingUIState,
        isNewCreationAccount = isNewCreationAccount,
        isUpgradeAccount = isUpgradeAccount,
        onFreePlanClicked = {
            Analytics.tracker.trackEvent(
                GetStartedForFreeUpgradePlanButtonPressedEvent
            )
            activity?.let {
                onFreeClick(
                    activity = it,
                    megaNavigator = megaNavigator,
                    isSingleActivityEnabled = uiState.isSingleActivityEnabled
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
                    megaNavigator = megaNavigator,
                    isSingleActivityEnabled = uiState.isSingleActivityEnabled
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
                    subscription = subscription
                )
            }
        },
        onExternalCheckoutClick = { subscription, monthly ->
            sendAccountTypeAnalytics(
                isUpgradeAccount = isUpgradeAccount,
                openFromSource = openFromSource,
                planType = subscription.accountType,
                isUpgradeAccountDueToAds = accountStorageViewModel.isUpgradeAccountDueToAds()
            )
            billingViewModel.onExternalPurchaseClick(subscription, monthly)
        },
        isExternalCheckoutEnabled = uiState.isExternalCheckoutEnabled,
        isExternalCheckoutDefault = uiState.isExternalCheckoutDefault,
        userAgeComplianceStatus = uiState.userAgeComplianceStatus,
        clearExternalPurchaseError = billingViewModel::clearExternalPurchaseError,
        onBack = onBack
    )
}


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
    megaNavigator: MegaNavigator,
    isSingleActivityEnabled: Boolean,
) {
    if (isSingleActivityEnabled) {
        activity.finish()
    } else {
        val bundle = createNavigationBundle(activity, AccountType.FREE)
        navigateToManagerActivity(megaNavigator, activity, bundle)
    }
}

private fun convertAccountTypeToInt(accountType: AccountType): Int {
    return when (accountType) {
        AccountType.PRO_LITE -> AccountTypeInt.PRO_LITE
        AccountType.PRO_I -> AccountTypeInt.PRO_I
        AccountType.PRO_II -> AccountTypeInt.PRO_II
        AccountType.PRO_III -> AccountTypeInt.PRO_III
        else -> AccountTypeInt.FREE
    }
}

/**
 * Creates a bundle for navigation with common extras
 */
private fun createNavigationBundle(activity: Activity, accountType: AccountType? = null): Bundle {
    return Bundle().apply {
        activity.intent.extras?.let { putAll(it) }
        putBoolean(ExtraConstant.EXTRA_FIRST_LOGIN, true)
        if (!containsKey(ExtraConstant.EXTRA_NEW_ACCOUNT)) {
            putBoolean(ExtraConstant.EXTRA_NEW_ACCOUNT, true)
        }
        if (!containsKey(ExtraConstant.NEW_CREATION_ACCOUNT)) {
            putBoolean(ExtraConstant.NEW_CREATION_ACCOUNT, true)
        }
        accountType?.let {
            putBoolean(ExtraConstant.EXTRA_UPGRADE_ACCOUNT, it != AccountType.FREE)
            putInt(ExtraConstant.EXTRA_ACCOUNT_TYPE, convertAccountTypeToInt(it))
        }
    }
}

private fun onPurchasesUpdated(
    megaNavigator: MegaNavigator,
    activity: Activity,
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
    isSingleActivityEnabled: Boolean,
) {
    if (isUpgradeAccount) {
        if (openFromSource == UpgradeAccountSource.MY_ACCOUNT_SCREEN) {
            megaNavigator.openMyAccountActivity(
                context = activity,
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            activity.finish()
        }
        // other cases stay in the same activity
    } else if (!isSingleActivityEnabled) {
        // Reuse the common navigation logic for non-upgrade account cases
        val bundle = createNavigationBundle(activity)
        navigateToManagerActivity(megaNavigator, activity, bundle)
    } else {
        activity.finish()
    }
}

/**
 * Navigates to ManagerActivity with the provided bundle
 */
private fun navigateToManagerActivity(
    megaNavigator: MegaNavigator,
    activity: Activity,
    bundle: Bundle,
) {
    megaNavigator.openManagerActivity(
        context = activity,
        data = activity.intent.data,
        action = activity.intent.action,
        bundle = bundle
    )
    activity.finish()
}
