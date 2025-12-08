package mega.privacy.android.feature.clouddrive.presentation.storage

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey


fun EntryProviderScope<NavKey>.overQuotaDialog(
    navigateToUpgradeAccount: () -> Unit,
    navigateToCustomizedPlan: (String, AccountType) -> Unit,
    navigateToAchievements: () -> Unit,
    onDismiss: (NavKey) -> Unit,
) {
    entry<OverQuotaDialogNavKey> { key ->

        StorageStatusDialogViewM3(
            storageState = if (key.isOverQuota) StorageState.Red else StorageState.Orange,
            preWarning = !key.isOverQuota,
            overQuotaAlert = true,
            onUpgradeClick = navigateToUpgradeAccount,
            onCustomizedPlanClick = { email, accountType ->
                navigateToCustomizedPlan(email, accountType)
            },
            onAchievementsClick = navigateToAchievements,
            onClose = { onDismiss(key) },
        )

    }
}