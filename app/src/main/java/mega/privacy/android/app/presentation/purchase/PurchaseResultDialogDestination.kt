package mega.privacy.android.app.presentation.purchase

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.PurchaseType
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.feature.payment.components.PurchaseSuccessDialog
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.PurchaseResultDialogNavKey
import mega.privacy.android.shared.resources.R as sharedR

data object PurchaseResultDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            purchaseResultDialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.purchaseResultDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<PurchaseResultDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        when (key.purchaseType) {
            PurchaseType.PENDING -> {
                BasicDialog(
                    title = stringResource(id = R.string.title_user_purchased_subscription),
                    description = stringResource(id = R.string.message_user_payment_pending),
                    buttons = persistentListOf(
                        BasicDialogButton(
                            text = stringResource(id = sharedR.string.general_ok),
                            onClick = {
                                onDialogHandled()
                                remove(key)
                            }
                        ),
                    ),
                    onDismissRequest = {
                        onDialogHandled()
                        remove(key)
                    },
                )
            }

            PurchaseType.DOWNGRADE -> {
                BasicDialog(
                    title = stringResource(id = sharedR.string.general_upgrade_button),
                    description = stringResource(id = R.string.message_user_purchased_subscription_down_grade),
                    buttons = persistentListOf(
                        BasicDialogButton(
                            text = stringResource(id = sharedR.string.general_ok),
                            onClick = {
                                onDialogHandled()
                                remove(key)
                            }
                        ),
                    ),
                    onDismissRequest = {
                        onDialogHandled()
                        remove(key)
                    },
                )
            }

            PurchaseType.SUCCESS -> {
                val purchaseInfo = getPurchaseInfo(key.activeSubscriptionSku)
                if (purchaseInfo != null) {
                    PurchaseSuccessDialog(
                        accountName = stringResource(id = purchaseInfo.accountNameRes),
                        accountImage = purchaseInfo.imageRes,
                        message = stringResource(id = purchaseInfo.messageRes),
                        onDismiss = {
                            onDialogHandled()
                            remove(key)
                        }
                    )
                } else {
                    onDialogHandled()
                    remove(key)
                }
            }
        }
    }
}

private data class PurchaseInfo(
    val accountNameRes: Int,
    @DrawableRes val imageRes: Int,
    val messageRes: Int,
)

private fun getPurchaseInfo(sku: String): PurchaseInfo? {
    return when (sku) {
        Skus.SKU_PRO_I_MONTH -> PurchaseInfo(
            accountNameRes = R.string.pro1_account,
            imageRes = R.drawable.ic_pro_i_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_1_monthly
        )

        Skus.SKU_PRO_I_YEAR -> PurchaseInfo(
            accountNameRes = R.string.pro1_account,
            imageRes = R.drawable.ic_pro_i_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_1_yearly
        )

        Skus.SKU_PRO_II_MONTH -> PurchaseInfo(
            accountNameRes = R.string.pro2_account,
            imageRes = R.drawable.ic_pro_ii_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_2_monthly
        )

        Skus.SKU_PRO_II_YEAR -> PurchaseInfo(
            accountNameRes = R.string.pro2_account,
            imageRes = R.drawable.ic_pro_ii_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_2_yearly
        )

        Skus.SKU_PRO_III_MONTH -> PurchaseInfo(
            accountNameRes = R.string.pro3_account,
            imageRes = R.drawable.ic_pro_iii_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_3_monthly
        )

        Skus.SKU_PRO_III_YEAR -> PurchaseInfo(
            accountNameRes = R.string.pro3_account,
            imageRes = R.drawable.ic_pro_iii_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_3_yearly
        )

        Skus.SKU_PRO_LITE_MONTH -> PurchaseInfo(
            accountNameRes = R.string.prolite_account,
            imageRes = R.drawable.ic_lite_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_lite_monthly
        )

        Skus.SKU_PRO_LITE_YEAR -> PurchaseInfo(
            accountNameRes = R.string.prolite_account,
            imageRes = R.drawable.ic_lite_big_crest,
            messageRes = R.string.upgrade_account_successful_pro_lite_yearly
        )

        else -> null
    }
}

