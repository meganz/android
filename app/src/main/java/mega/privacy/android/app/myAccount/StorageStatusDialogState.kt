package mega.privacy.android.app.myAccount

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.StorageState

/**
 * Data class defining the state of [mega.privacy.android.app.myAccount.StorageStatusDialogView]
 *
 * @property storageState
 * @property isAchievementsEnabled
 * @property product
 * @property accountType
 * @property preWarning
 * @property overQuotaAlert
 */
data class StorageStatusDialogState(
    val storageState: StorageState = StorageState.Unknown,
    val isAchievementsEnabled: Boolean = false,
    val product: Product? = null,
    val accountType: AccountType = AccountType.UNKNOWN,
    val preWarning: Boolean = false,
    val overQuotaAlert: Boolean = false,
)