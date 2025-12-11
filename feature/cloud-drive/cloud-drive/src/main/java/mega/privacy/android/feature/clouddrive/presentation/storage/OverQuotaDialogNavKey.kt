package mega.privacy.android.feature.clouddrive.presentation.storage

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogViewM3
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey


fun EntryProviderScope<NavKey>.overQuotaDialog(
    navigateToUpgradeAccount: () -> Unit,
    navigateToCustomizedPlan: (Context, String, AccountType) -> Unit,
    navigateToAchievements: () -> Unit,
    onDismiss: (NavKey) -> Unit,
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
            onClose = { onDismiss(key) },
        )

    }
}