package mega.privacy.android.app.presentation.storage

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

data object OverQuotaDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            overQuotaDialogDestination(
                navigateToUpgradeAccount = {
                    navigationHandler.navigate(UpgradeAccountNavKey())
                },
                navigateToCustomizedPlan = { context, email, accountType ->
                    AlertsAndWarnings.askForCustomizedPlan(
                        context = context,
                        myEmail = email,
                        accountType = accountType
                    )
                },
                navigateToAchievements = {
                    navigationHandler.navigate(AchievementNavKey)
                },
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.overQuotaDialogDestination(
    navigateToUpgradeAccount: () -> Unit,
    navigateToCustomizedPlan: (Context, String, AccountType) -> Unit,
    navigateToAchievements: () -> Unit,
    remove: (DialogNavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<OverQuotaDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val context = LocalContext.current
        StorageStatusDialogViewM3(
            storageState = if (key.isOverQuota) StorageState.Red else StorageState.Orange,
            preWarning = !key.isOverQuota,
            overQuotaAlert = key.overQuotaAlert,
            onUpgradeClick = navigateToUpgradeAccount,
            onCustomizedPlanClick = { email, accountType ->
                navigateToCustomizedPlan(context, email, accountType)
            },
            onAchievementsClick = navigateToAchievements,
            onClose = {
                onDialogHandled()
                remove(key)
            },
        )
    }
}

