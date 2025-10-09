package mega.privacy.android.feature.payment.presentation.upgrade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.Purchase
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.feature.payment.model.AccountTypeInt
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import mega.privacy.android.feature.payment.util.PaymentUtils
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
                        purchases = it.purchases,
                        isUpgradeAccount = isUpgradeAccount,
                        openFromSource = openFromSource
                    )
                }
                billingViewModel.markHandleBillingEvent()
            }
        }
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
            activity?.let { onFreeClick(it, megaNavigator) }
        },
        maybeLaterClicked = {
            Analytics.tracker.trackEvent(
                MaybeLaterUpgradeAccountButtonPressedEvent
            )
            activity?.let { onFreeClick(it, megaNavigator) }
        },
        onBuyPlanClick = { accountType, isMonthly ->
            sendAccountTypeAnalytics(
                isUpgradeAccount = isUpgradeAccount,
                openFromSource = openFromSource,
                planType = accountType,
                isUpgradeAccountDueToAds = accountStorageViewModel.isUpgradeAccountDueToAds()
            )
            activity?.let {
                billingViewModel.startPurchase(
                    activity,
                    PaymentUtils.getProductId(isMonthly, accountType),
                )
            }
        },
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

private fun onFreeClick(activity: Activity, megaNavigator: MegaNavigator) {
    val bundle = createNavigationBundle(activity, AccountType.FREE)
    navigateToManagerActivity(megaNavigator, activity, bundle)
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
    purchases: List<MegaPurchase>,
    isUpgradeAccount: Boolean,
    openFromSource: UpgradeAccountSource,
) {
    if (purchases.isNotEmpty()) {
        val purchase = purchases.first()
        //payment may take time to process, we will not give privilege until it has been fully processed
        val sku = purchase.sku
        if (purchase.state == Purchase.PurchaseState.PURCHASED) {
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
                context = activity,
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            activity.finish()
        }
        // other cases stay in the same activity
    } else {
        // Reuse the common navigation logic for non-upgrade account cases
        val bundle = createNavigationBundle(activity)
        navigateToManagerActivity(megaNavigator, activity, bundle)
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